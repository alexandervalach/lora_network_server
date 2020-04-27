package processor;

import core.DateManager;
import core.ProgramResources;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * EN processing logic
 * @author Karol Cagáň
 * @version 1.0
 */
public class EDProcessor {
  private final ProgramResources programResources;
  private final int maxPower;
  private final int edTransmissionParamId;
  private final int downSfSensitivity;
  private final int downPwSensitivity;
  private final int upSfSensitivity;
  private final int upPwSensitivity;
  private final int maxSpf;
  private final int seqTolerance;
  private final int maxDutyMillis;
  private final int snrSensitivity;
  private final int dutyCycleSensitivity;
  private final int dutyCycleRestriction;
  private final int sensitivityPoison;
  private final int restrictionPoison;
  private final String algorithm;

  /**
   * Constructor
   * @param programResources collects all required resources
   */
  public EDProcessor(ProgramResources programResources) {
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
    this.algorithm = programResources.props.getStr("ServerSetting.algorithm");
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

      // Loads previous messages
      int msgGroupId = 0;
      JSONArray prevMsgs = this.loadPreviousMessages(primary);

      // Loads last N messages from DB
      int finalRssi = 0;
      int finalSNR = 0;

      if (prevMsgs.length() < 1) {
        finalRssi = primary.getInt("rssi");
        finalSNR = primary.getInt("snr");
      } else {
        for (int i = 0; i < prevMsgs.length(); i++) {
          JSONObject current = prevMsgs.getJSONObject(i);
          finalRssi += current.getInt("rssi");
          finalSNR += current.getInt("snr");

          if (i == 0) {
            msgGroupId = current.getInt("msg_group_number") + 1;
          }
        }
        finalRssi = finalRssi / prevMsgs.length();
        finalSNR = finalSNR / prevMsgs.length();
      }

      // Get message type id from DB
      int msgTypeId = programResources.dbHandler.readMessageType(primary.getString("type"));

      // Bulk insert of uplink messages
      programResources.dbHandler.bulkInsertUplinkMessages(currentGrape, primary, msgGroupId, msgTypeId);

      // Quits if response unavailable
      String ackType = primary.getString("ack");

      if (ackType.equals("UNSUPPORTED")) {
        return;
      }

      // Determines transmission power down
      boolean confNeed = primary.getBoolean("conf_need");
      boolean powerChanged = this.updatePower(devId, finalRssi, finalSNR, confNeed);

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
          //set full power for device in DB as well
          this.programResources.dbHandler.updatePower(devId, 0, 0, 0);
        }

        // Builds skeleton for a response
        JSONObject txlMsg = new JSONObject();
        JSONArray netData = new JSONArray();
        txlMsg.put("message_name", "TXL");
        JSONObject messageBody = new JSONObject();
        messageBody.put("dev_id", devId);
        messageBody.put("power", downPw);

        // Calculate airtime for downlink messages
        messageBody.put("time", getMsgCost(primary, spf));

        // Packs app data if available
        if (!rawResponse.toString().equals("{}")) {
          messageBody.put("app_data", rawResponse.get("app_data"));
        } else {
          messageBody.put("app_data", "");
        }

        // Packs network data if needed
        if (confNeed) {
          if (this.algorithm.equals("adr")) {
            System.out.println("Using ADR algorithm");
            JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(edTransmissionParamId));

            // Builds transmission param array
            JSONObject normalParam = new JSONObject();
            normalParam.put("sf", spf);
            normalParam.put("cr", params.get("coderate"));
            normalParam.put("band", params.get("bandwidth"));
            normalParam.put("type", "NORMAL");
            normalParam.put("power", upPw);
            normalParam.put("freqs", params.get("standard_freq_arr"));

            JSONObject emerParam = new JSONObject(normalParam.toString());
            emerParam.put("type", "EMER");
            emerParam.put("sf", maxSpf);
            emerParam.put("power", maxPower); // Fixed max power for emergency Bcast
            emerParam.put("freqs", params.get("emergency_freq_arr"));

            JSONObject regParam = new JSONObject(normalParam.toString());
            regParam.put("type", "REG");
            regParam.put("sf", maxSpf);
            regParam.put("power", maxPower); // Fixed max power for reg Bcast
            regParam.put("freqs", params.get("registration_freq_arr"));

            // Creates an array of params as a response
            netData.put(emerParam);
            netData.put(normalParam);
            netData.put(regParam);
          } else if (this.algorithm.equals("ts")) {
            System.out.println("Using Thompson Sampling algorithm");
            JSONObject stat_model = node.getJSONObject("stat_model");
            System.out.println(stat_model);
          } else if (this.algorithm.equals("ucb")) {
            System.out.println("Using Upper Confidence Bound algorithm");
            node.getJSONObject("stat_model");
            JSONObject stat_model = node.getJSONObject("stat_model");
            System.out.println(stat_model);
          }
        }

        // If nodes transmitting power needs to be decreased, packs the config
        if (powerChanged) {
          JSONObject normalParam = new JSONObject();
          normalParam.put("type","NORMAL");
          normalParam.put("power", upPw);
          normalParam.put("sf", spf);
          netData.put(normalParam);
        }

        // Packs net data
        messageBody.put("net_data", netData);

        // Builds message
        txlMsg.put("message_body", messageBody);

        System.out.println("New TXL reply for AP");
        System.out.println(txlMsg);

        // Checks for duty Cycle duration of a message
        int remainingDutyC = this.getRemainingDutyCycle(messageBody, primary.getInt("sf"), primary.getInt("duty_c"));

        // Sends message to desired AP
        int apIdentifier = primary.getInt("apIdentifier");

        // TODO: Uprav ked downlink nepojde kvoli duty cyclu
        if (remainingDutyC > 0) {
          this.programResources.sslConnection.socketThreadArrayList.get(apIdentifier).write(txlMsg.toString());
          System.out.println("********** Raw response " + rawResponse.toString());

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
          // this.programResources.dbHandler.WriteUnsentMSG_D(message_body.getString("app_data"), primary.getString("hWIdentifier"), message_body.getString("dev_id"));
          System.out.println("Unable to deliver message due to insufficient duty cycle. Oversize of: " + remainingDutyC * (-1));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Calculates metric for selected downlink messages
   * @param rssi value of rssi
   * @param dutyCycleRemaining remaining duty cycle value
   * @return float
   */
  public float getMetric(int rssi, int dutyCycleRemaining) {
    float dutyCyclePercent = Math.round((dutyCycleRemaining * 100.0) / maxDutyMillis);

    // If more dutyC remaining than sensitivity boundary does not poison RSSI
    if (dutyCyclePercent > this.dutyCycleSensitivity) {
      return rssi;
    }

    // If less than critical dutyC remaining on a gateway poisons the RSSI by greater value
    if (dutyCyclePercent < this.dutyCycleRestriction) {
      // If really low duty cycle severely poisons the route
      if (dutyCyclePercent < 5) {
        System.out.println("***** Total poison");
        return rssi - 1000;
      }
      // Else greater poison
      System.out.println("**** High poison");
      return rssi - this.restrictionPoison;
    }
    // If dutyC is between sensitivity and critical boundary poisons the RSSI by medium value
    System.out.println("**** Medium poison");
    return rssi - this.sensitivityPoison;

  }

  /**
   * Calculates duty cycle consumed by selected message
   * @param msgBody
   * @param spf
   * @return int
   * @throws Exception
   */
  public int getMsgCost(JSONObject msgBody, int spf) throws Exception {
    // Values initialization
    JSONObject node = new JSONObject(programResources.dbHandler.readNode(msgBody.getString("dev_id")));
    JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(2));
    int cr = 1; // According to code rate 4/5
    int loraFiitOverheadBytes = 12; // 4B LoRa@FIIT data and 8B Lora preamble
    int netDataBytes = 0;
    int appDataBytes = 0;
    JSONArray netData;
    String appData = "";

    try {
      netData = msgBody.getJSONArray("net_data");

      // Length for sending limited config
      if (netData.length() == 1) {
        netDataBytes = 3;
        System.out.println("**** Short network data");
      } else if (netData.length() > 1) {
        netDataBytes = 11 + 5;
        System.out.println("**** Long network data");
      }
    } catch (JSONException e) {
      System.out.println("Network data are not present");
    }

    try {
      appData = msgBody.getString("app_data");
    } catch (JSONException e) {
      System.out.println("App data are not present");
    }

    if (!appData.equals("")) {
      appDataBytes = (appData.length() * 3 / 4) + 1;
      //System.out.println("************Nastavil som dlzku APP data na: "+AppDataBytes);
    }

    int payloadOverhead = 0;
    // Checks if payload is present
    if (appDataBytes + netDataBytes > 0) {
      payloadOverhead = 6;
      // System.out.println("*******************Pridal som payload ovh");
    }

    int bandwidth = params.getInt("bandwidth");

    // Calculates padding size
    int blockSizeOverhead = (appDataBytes + netDataBytes + payloadOverhead) % 4;
    System.out.println("Block size overhead is: " + blockSizeOverhead);
    //counts the formula, when all data are present
    float symbolTime = (float) Math.pow(2, spf) / ((float) bandwidth / 1000);
    System.out.println("***** Symbol time is: " + symbolTime);
    int msgSymbols  = (int) 8 + ((8 * (netDataBytes + appDataBytes + loraFiitOverheadBytes + payloadOverhead + blockSizeOverhead) - 4 * spf + 28 + 16 ) / (4 * (spf-2))) * (cr + 4);
    System.out.println("***** Msg symbol count is :" + msgSymbols);

    float msgCost = msgSymbols * symbolTime;
    System.out.println("On-Air time for packet calculated is " + msgCost + " milis");

    return Math.round(msgCost);
  }

  /**
   * Returns remaining duty cycle after a msg is sent
   * @param msgBody json body of message
   * @param sf value of spreading factor
   * @param dutyCycleBeforeSent value of remaining duty cycle
   * @return int
   * @throws Exception
   */
  public int getRemainingDutyCycle(JSONObject msgBody, int sf, int dutyCycleBeforeSent) throws Exception {
    return dutyCycleBeforeSent - getMsgCost(msgBody, sf);
  }

  /***
   * Determines primary message from list of messages and returns it
   * @param messages list containing copies of one messages from different APs
   * @return JSONObject primary message
   * @throws JSONException
   */
  private JSONObject getPrimaryMessage (ArrayList<JSONObject> messages) throws JSONException {
    JSONObject primary = null;

    for (JSONObject message : messages) {
      if (primary == null) {
        primary = message;
      } else {
        // Determine the best downlink candidate
        if (getMetric(primary.getInt("rssi"), primary.getInt("duty_c")) < getMetric(message.getInt("rssi"), message.getInt("duty_c"))) {
          primary = message;
        }
      }
    }
    return primary;
  }

  /***
   * Check sequence number of an incoming message
   * @param primary
   * @param node
   * @return
   * @throws JSONException
   */
  private Boolean checkSequenceNumber (JSONObject primary, JSONObject node) throws JSONException {
    if (primary.getInt("seq") > node.getInt("last_seq") + this.seqTolerance || primary.getInt("seq") <= node.getInt("last_seq")) {
      // Checks for seq overflow
      if (primary.getInt("seq") > this.seqTolerance) {
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

  private boolean updatePower (String devId, int finalRssi, int finalSNR, boolean confNeed) {
    boolean powerChanged = false;

    if (finalRssi > this.downPwSensitivity && !confNeed && finalSNR > this.snrSensitivity) {
      if (finalRssi > this.downSfSensitivity) {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 0, 0, 1);
      } else {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 1, 0, 0);
      }

      if (!powerChanged) {
        powerChanged = this.programResources.dbHandler.updatePower(devId, 1, 0, 0);
      }
    } else if (finalRssi < this.upPwSensitivity && !confNeed || finalSNR < this.snrSensitivity && !confNeed) {
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
}
