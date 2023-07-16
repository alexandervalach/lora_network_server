package processor;

import core.ProgramResources;
import helpers.MessageHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * EN processing logic
 * @author Karol Cagáň
 * @author Alexander Valach
 * @version 0.3
 */
public class EDProcessor extends NodeProcessor {
  private final ProgramResources programResources;
  private final int maxPower;
  private final int edTransmissionParamId;
  private final int downSfSensitivity;
  private final int downPwSensitivity;
  private final int upSfSensitivity;
  private final int upPwSensitivity;
  private final int maxSpf;
  private final int seqTolerance;
  private final int snrSensitivity;

  /**
   * Constructor
   * @param programResources collects all required resources
   */
  public EDProcessor(ProgramResources programResources) {
    super(programResources);
    this.programResources = programResources;
    this.maxPower = programResources.props.getInt("LoRaSettings.maxPower");
    this.edTransmissionParamId = programResources.props.getInt("LoRaSettings.edTransmissionParamId");
    this.downSfSensitivity = programResources.props.getInt("LoRaSettings.powerDownSpfRssiSensitivityBoundary");
    this.downPwSensitivity = programResources.props.getInt("LoRaSettings.powerDownPowerRssiSensitivityBoundary");
    this.upPwSensitivity = programResources.props.getInt("LoRaSettings.powerUpPowerRssiSensitivityBoundary");
    this.upSfSensitivity = programResources.props.getInt("LoRaSettings.powerUpSpfRssiSensitivityBoundary");
    this.snrSensitivity = programResources.props.getInt("LoRaSettings.snrSensitivityBoundary");
    this.maxSpf = programResources.props.getInt("LoRaSettings.maxSpf");
    this.seqTolerance = programResources.props.getInt("LoRaSettings.seqTolerance");
    this.maxDutyMillis = 3600000 / programResources.props.getInt("LoRaSettings.dutyCyclePercent");
    this.dutyCycleRestriction = programResources.props.getInt("LoRaSettings.dutyCycleRestrictionBoundary");
    this.dutyCycleSensitivity = programResources.props.getInt("LoRaSettings.dutyCycleSensitivityBoundary");
    this.sensitivityPoison = programResources.props.getInt("LoRaSettings.dutyCycleSensitivityPoisonRssiValue");
    this.restrictionPoison = programResources.props.getInt("LoRaSettings.dutyCycleRestrictionPoisonRssiValue");
  }

  /**
   * Process RXL message
   * @param currentGrape list of current messages in json
   */
  public void processRXL(ArrayList<JSONObject> currentGrape) {
    try {
      System.out.println("Processing batch " + currentGrape.toString());
      // Determines which message is used as primary
      JSONObject primary = this.getPrimaryMessage(currentGrape);

      assert primary != null;
      String devId = primary.getString("dev_id");
      String nodeData = this.programResources.dbHandler.readNode(devId);

      if (nodeData == null) {
        System.out.println("Message from unknown node discarded.");
        return;
      }

      JSONObject node = new JSONObject(nodeData);

      // Checks seq number of messages and updates last seq
      if (!this.checkSequenceNumber(primary, node)) {
        return;
      }

      // If check was successful writes new seq into DB
      // jonsnow: Original line by Karol Cagáň
      // programResources.dbHandler.writeKey(primary.getString("dev_id"), primary.getInt("seq"), "");

      // Update sequence number
      programResources.dbHandler.updateSequence(devId, primary.getInt("seq"));

      // Get message type id from DB
      int msgTypeId = programResources.dbHandler.readMessageType(primary.getString("type"));

      int msgGroupId = 0;
      JSONArray prevMsgs = this.loadPreviousMessages(primary);

      // Loads last N messages from DB
      int finalRssi = 0;
      int finalSnr = 0;

      if (prevMsgs.length() < 1) {
        finalRssi = primary.getInt("rssi");
        finalSnr = primary.getInt("snr");
      } else {
        for (int i = 0; i < prevMsgs.length(); i++) {
          JSONObject current = prevMsgs.getJSONObject(i);
          finalRssi += current.getInt("rssi");
          finalSnr += current.getInt("snr");

          if (i == 0) {
            msgGroupId = current.getInt("msg_group_number") + 1;
          }
        }
        finalRssi = finalRssi / prevMsgs.length();
        finalSnr = finalSnr / prevMsgs.length();
      }

      // Bulk insert of uplink messages
      programResources.dbHandler.bulkInsertUplinkMessages(currentGrape, primary, msgGroupId, msgTypeId);

      //---COMMUNICATION PARAMS ALGORITHM SELECTION
      JSONObject messageBody;

      if (this.isBanditAlgorithm) {
        messageBody = this.mabAlgorithm(primary, finalRssi, finalSnr);
      } else {
        messageBody = this.adrAlgorithm(primary, finalRssi, finalSnr);
      }

      if (messageBody == null) {
        return;
      }

      JSONObject txlMsg = new JSONObject();
      txlMsg.put("message_name", "TXL");
      txlMsg.put("message_body", messageBody);

      System.out.println("New TXL reply for AP");
      System.out.println(txlMsg);

      // Checks for duty Cycle duration of a message
      int remainingDutyC = 0;

      try {
        remainingDutyC = this.getRemainingDutyCycle(messageBody, primary.getInt("sf"), primary.getInt("band"), primary.getInt("duty_c"));
      } catch (Exception e) {
        System.out.println("There was a problem during duty cycle recalculation");
      }

      // Sends message to desired AP
      int apIdentifier = primary.getInt("apIdentifier");

      if (remainingDutyC > 0) {
        JSONObject rawResponse = new JSONObject(programResources.dbHandler.readDownlinkMsg(devId));
        this.programResources.sslConnection.socketThreadArrayList.get(apIdentifier).write(txlMsg.toString());
        System.out.println("****** Raw response " + rawResponse.toString());

        if (rawResponse.toString().equals("{}")) {
          // Write new message and send it to AP
          this.programResources.dbHandler.writeSentDownlinkMsg(
                  messageBody.getString("app_data"),
                  messageBody.getString("net_data"),
                  remainingDutyC,
                  Float.parseFloat(primary.getString("freq")),
                  primary.getInt("sf"),
                  messageBody.getInt("power"),
                  primary.getInt("time"),
                  primary.getString("cr"),
                  primary.getInt("band"),
                  primary.getString("hWIdentifier"),
                  messageBody.getString("dev_id")
          );
        } else {
          // Marks messages as sent in DB
          this.programResources.dbHandler.markDownlinkAsSent(rawResponse.getInt("id"), remainingDutyC);
        }
      } else {
        // Version 1.0 does not support network data buffering
        System.out.println("Unable to deliver message due to insufficient duty cycle. Oversize of: " + remainingDutyC * (-1));
      }
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns remaining duty cycle after a msg is sent
   * @param msgBody json body of message
   * @param sf value of spreading factor
   * @param dutyCycleBeforeSent value of remaining duty cycle
   * @return int
   * @throws Exception
   */
  public int getRemainingDutyCycle(JSONObject msgBody, int sf, int band, int dutyCycleBeforeSent) throws Exception {
    return dutyCycleBeforeSent - MessageHelper.getMsgCost(msgBody, sf, band);
  }

  /***
   * Check sequence number of an incoming message
   * @param primary primary message content
   * @param node end node data from database
   * @return whether appropriate sequence number
   * @throws JSONException
   */
  private Boolean checkSequenceNumber (JSONObject primary, JSONObject node) throws JSONException {
    int seq = primary.getInt("seq");
    int lastSeq = node.getInt("last_seq");

    if (seq > lastSeq + this.seqTolerance || seq <= lastSeq) {
      // Checks for seq overflow
      if (seq > this.seqTolerance) {
        System.out.println("Message discarded because of failed SEQ check");
        return false;
      }
    }
    return true;
  }

  /**
   * Load previous messages
   * @param primary received primary message
   * @return JSON array of last n messages
   * @throws JSONException
   */
  private JSONArray loadPreviousMessages (JSONObject primary) throws JSONException {
    System.out.println("Primary packet: " + primary.toString());
    System.out.println("Node id: " + primary.getString("dev_id"));
    String lastMsg = programResources.dbHandler.readLastNMessages(primary.getString("dev_id"));

    if (lastMsg != null && !lastMsg.equals("[]")) {
      return new JSONArray(lastMsg);
    }
    return new JSONArray();
  }

  /***
   * Updates power and spreading factor based on recent node's communication
   * @param devId end node identifier
   * @param finalRssi average RSSI value
   * @param finalSnr average SNR value
   * @param confNeed specified if access point requires to reconfigure end node
   * @return whether power has been changed
   */
  private boolean updatePower (String devId, int finalRssi, int finalSnr, boolean confNeed) {
    boolean powerChanged = false;

    if (finalRssi > this.downPwSensitivity && !confNeed && finalSnr > this.snrSensitivity) {
      if (finalRssi > this.downSfSensitivity) {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 0, 0, 1);
      } else {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 1, 0, 0);
      }

      if (!powerChanged) {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 1, 0, 0);
      }
    } else if (finalRssi < this.upPwSensitivity && !confNeed || finalSnr < this.snrSensitivity && !confNeed) {
      if (finalRssi < this.upSfSensitivity) {
        powerChanged = this.programResources.dbHandler.increasePower(devId, 0, 1);
      } else {
        powerChanged = this.programResources.dbHandler.increasePower(devId, 1, 0);
      }

      if (!powerChanged) {
        powerChanged = this.programResources.dbHandler.increasePower(devId, 1, 0);
      }
    }
    return powerChanged;
  }

  /***
   * Statistical model update using Multi-Armed Bandit Approach
   * @param primary primary message
   * @param finalRssi average RSSI value
   * @param finalSnr average SNR value
   * @return messageBody
   * @throws JSONException
   */
  private JSONObject mabAlgorithm (JSONObject primary, int finalRssi, int finalSnr) throws JSONException {
    String ackType = primary.getString("ack");

    if (ackType.equals("UNSUPPORTED")) {
      return null;
    }

    int sf = primary.getInt("sf");
    int power = primary.getInt("power");
    String devId = primary.getString("dev_id");
    boolean confNeed = primary.getBoolean("conf_need");

    JSONObject message = new JSONObject();
    JSONObject messageBody = new JSONObject();
    JSONArray arms = this.getEnStatModel(devId);

    if (arms == null) {
      return null;
    }

    // Updated combination is returned
    JSONObject banditArm = this.statModelChange(devId, finalRssi, finalSnr, sf, power, confNeed);
    JSONArray netData = new JSONArray();

    if (banditArm != null) {
      System.out.println("Bandit arm " + banditArm.toString());
      MessageHelper.updateStatModel(arms, banditArm.getInt("sf"), banditArm.getInt("pw"), 1);
      System.out.println(primary.getString("dev_id") + ": Statistical model updated");

      // Only sent net data when ack is not required
      // Nodes are able to update rewards for mandatory messages themselves
      // if (ackType.equals("VOLATILE")) {
      System.out.println(primary.getString("dev_id") + ": Network data update scheduled");
      netData.put(banditArm);
      // }
    }

    if (netData.length() == 0) {
      System.out.println(primary.getString("dev_id") + ": Bandit arm not updated");
      return null;
    }

    message.put("message_name", "TXL");
    messageBody.put("dev_id", devId);
    messageBody.put("app_data", "");
    messageBody.put("net_data", netData);
    messageBody.put("power", power);
    message.put("message_body", messageBody);

    try {
      messageBody.put("time", MessageHelper.getMsgCost(messageBody, sf, primary.getInt("band")));
    } catch (Exception e) {
      System.out.println("Unable to calculate airtime for downlink message");
      e.printStackTrace();
      return null;
    }

    return messageBody;
  }

  /***
   * Handling ADR messages
   * @param primary primary message
   * @param finalRssi average RSSI value
   * @param finalSnr average SNR value
   * @return messageBody
   * @throws JSONException
   */
  private JSONObject adrAlgorithm (JSONObject primary, int finalRssi, int finalSnr) throws JSONException {
    String devId = primary.getString("dev_id");
    String ackType = primary.getString("ack");

    // Quits if response unavailable
    if (ackType.equals("UNSUPPORTED")) {
      return null;
    }

    // Determines transmission power down
    boolean confNeed = primary.getBoolean("conf_need");
    boolean powerChanged = this.updatePower(devId, finalRssi, finalSnr, confNeed);

    // Searches buffer for unsent response
    JSONObject rawResponse = new JSONObject(programResources.dbHandler.readDownlinkMsg(devId));

    // Checks if there is a pending reply, or reconfiguration needed or both
    if ((ackType.equals("VOLATILE") || ackType.equals("MANDATORY")) && (!rawResponse.toString().equals("{}") || confNeed || ackType.equals("MANDATORY") || powerChanged)) {
      // Reads power settings
      int downPw = 0;
      int upPw = 0;
      int spf = 0;

      if (!confNeed) {
        JSONObject readPower = new JSONObject(programResources.dbHandler.readNode(devId));
        downPw = readPower.getInt("downstream_power");
        upPw = readPower.getInt("upstream_power");
        spf = readPower.getInt("spf");
      } else {
        // If device requests reconfiguration, power is set to full
        downPw = maxPower;
        upPw = maxPower;
        spf = maxSpf;
        // Set full power for device in DB as well
        this.programResources.dbHandler.updatePower(devId, 0, 0, 0);
      }

      // Builds skeleton for a response
      JSONArray netData = new JSONArray();
      JSONObject messageBody = new JSONObject();
      messageBody.put("dev_id", devId);
      messageBody.put("power", downPw);

      // Packs app data if available
      if (!rawResponse.toString().equals("{}")) {
        messageBody.put("app_data", rawResponse.get("app_data"));
      } else {
        messageBody.put("app_data", "");
      }

      // Packs network data if required
      if (confNeed) {
        netData = this.getNetData(spf, upPw, edTransmissionParamId);
      }

      // If nodes transmitting power needs to be decreased, packs the config
      if (powerChanged) {
        netData.put(this.getNormalParams(spf, upPw));
      }

      // Packs net data
      messageBody.put("net_data", netData);

      // Calculate airtime for downlink messages
      try {
        messageBody.put("time", MessageHelper.getMsgCost(messageBody, spf, primary.getInt("band")));
      } catch (Exception e) {
        System.out.println("Unable to calculate airtime for downlink message");
        e.printStackTrace();
        return null;
      }

      return messageBody;
    }
    return null;
  }
}
