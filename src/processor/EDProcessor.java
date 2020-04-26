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
  private static ProgramResources programResources;
  private int MaxPower;
  private int EDTransmissionParamID;
  private int DownSFSensitivity;
  private int DownPWSensitivity;
  private int UpSFSensitivity;
  private int UpPWSensitivity;
  private int MaxSPF;
  private int SeqTolerance;
  private int maxDutyMillis;
  private int SNRSensitivity;
  private int dutyCycleSensitivity;
  private int dutyCycleRestriction;
  private int sensitivityPoison;
  private int restrictionPoison;

  /**
   * Constructor
   * @param programResources
   */
  public EDProcessor(ProgramResources programResources) {
    this.programResources = programResources;
    this.MaxPower = programResources.props.getInt("LoRaSettings.maxPower");
    this.EDTransmissionParamID = programResources.props.getInt("LoRaSettings.edTransmissionParamId");
    this.DownSFSensitivity = programResources.props.getInt("LoRaSettings.powerDownSpfRssiSensitivityBoundary");
    this.DownPWSensitivity = programResources.props.getInt("LoRaSettings.powerDownPowerRssiSensitivityBoundary");
    this.UpPWSensitivity = programResources.props.getInt("LoRaSettings.powerUpPowerRssiSensitivityBoundary");
    this.UpSFSensitivity = programResources.props.getInt("LoRaSettings.powerUpSpfRssiSensitivityBoundary");
    this.SNRSensitivity = programResources.props.getInt("LoRaSettings.snrSensitivityBoundary");
    this.MaxSPF = programResources.props.getInt("LoRaSettings.maxSpf");
    this.SeqTolerance = programResources.props.getInt("LoRaSettings.seqTolerance");
    this.maxDutyMillis = 3600000 / programResources.props.getInt("LoRaSettings.dutyCyclePercent");
    this.dutyCycleRestriction = programResources.props.getInt("LoRaSettings.dutyCycleRestrictionBoundary");
    this.dutyCycleSensitivity = programResources.props.getInt("LoRaSettings.dutyCycleSensitivityBoundary");
    this.sensitivityPoison = programResources.props.getInt("LoRaSettings.dutyCycleSensitivityPoisonRssiValue");
    this.restrictionPoison = programResources.props.getInt("LoRaSettings.dutyCycleRestrictionPoisonRssiValue");
  }

  /**
   * Process RXL message
   * @param currentGrape
   */
  public void processRXL(ArrayList<JSONObject> currentGrape) {
    try {
      //System.out.println("Processing batch "+currentGrape.toString());
      // Determines which message is used as primary
      JSONObject primary = null;
      for (JSONObject jsonObject : currentGrape) {
        if(primary == null) {
          primary = jsonObject;
        } else {
          // Determine the best downlink candidate
          if (this.getMetric(primary.getInt("rssi"), primary.getInt("duty_c"))<this.getMetric(jsonObject.getInt("rssi"), jsonObject.getInt("duty_c"))) {
            primary = jsonObject;
          }
        }
      }

      // Checks seq number of messages and updates last seq
      String nodeData = this.programResources.dbHandler.readNode(primary.getString("dev_id"));

      if (nodeData == null) {
        System.out.println("Message discarded. No associated node found");
        return;
      }

      JSONObject node = new JSONObject(nodeData);
      if (primary.getInt("seq") > node.getInt("last_seq") + this.SeqTolerance || primary.getInt("seq") <= node.getInt("last_seq")) {
        // Checks for seq overflow
        if (primary.getInt("seq") > this.SeqTolerance) {
          System.out.println("Message discarded because of failed SEQ check");
          return;
        }
      }
      // If check was successful writes new seq into DB
      programResources.dbHandler.writeKey(primary.getString("dev_id"), primary.getInt("seq"), "");

      // Loads previous messages
      int msgGroupId = 0;
      JSONArray prevMsgs;

      try {
        System.out.println("Primary packet: " + primary.toString());
        System.out.println("Node id: " + primary.getString("dev_id"));
        String lastMsg = programResources.dbHandler.readLastNMessages(primary.getString("dev_id"));

        if (lastMsg != null && !lastMsg.equals("[]")) {
          prevMsgs = new JSONArray(lastMsg);
        } else {
          prevMsgs = new JSONArray();
        }
      } catch (Exception e) {
        e.printStackTrace();
        prevMsgs = new JSONArray();
      }

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
            msgGroupId = current.getInt("msg_group_number")+1;
          }
        }
        finalRssi = finalRssi / prevMsgs.length();
        finalSNR = finalSNR / prevMsgs.length();
      }

      int messageTypeId = programResources.dbHandler.readMessageType("normal");

      // Saves all messages into DB
      for (JSONObject jsonObject : currentGrape) {
        boolean primaryMessage = jsonObject.toString().equals(primary.toString());

        programResources.dbHandler.writeUplinkMsg(
            jsonObject.getString("data"),
            Float.parseFloat(jsonObject.getString("snr")),
            Float.parseFloat(jsonObject.getString("rssi")),
            jsonObject.getInt("duty_c"),
            primaryMessage,
            DateManager.getTimestamp(),
            msgGroupId,
            jsonObject.getInt("seq"),
            Float.parseFloat(jsonObject.getString("freq")),
            jsonObject.getInt("sf"),
            jsonObject.getInt("power"),
            jsonObject.getInt("time"),
            jsonObject.getString("cr"),
            jsonObject.getInt("band"),
            messageTypeId,
            jsonObject.getString("hWIdentifier"),
            jsonObject.getString("dev_id")
        );
      }

      // Quits if response unavailable
      if (primary.get("ack").equals("UNSUPPORTED")) {
        return;
      }

      // Determines transmission power down
      boolean powerChanged = false;
      if (finalRssi > this.DownPWSensitivity && !primary.getBoolean("conf_need") && finalSNR > this.SNRSensitivity ) {
        powerChanged = finalRssi > this.DownSFSensitivity ? this.programResources.dbHandler.updatePower(primary.getString("dev_id"), 0, 0, 1) : this.programResources.dbHandler.updatePower(primary.getString("dev_id"), 1, 0, 0);

        if (!powerChanged) {
          powerChanged = this.programResources.dbHandler.updatePower(primary.getString("dev_id"), 1, 0, 0);
        }
      }

      // Determines transmission power up
      if (finalRssi < this.UpPWSensitivity && !primary.getBoolean("conf_need") || finalSNR < this.SNRSensitivity && !primary.getBoolean("conf_need")) {
        powerChanged = finalRssi < this.UpSFSensitivity ? this.programResources.dbHandler.increasePower(primary.getString("dev_id"), 0, 1) : this.programResources.dbHandler.increasePower(primary.getString("dev_id"), 1, 0);

        if (!powerChanged) {
          powerChanged = this.programResources.dbHandler.increasePower(primary.getString("dev_id"), 1, 0);
        }
      }

      // Searches buffer for awaiting response
      JSONObject rawResponse = new JSONObject(programResources.dbHandler.readDownlinkMsg(primary.getString("dev_id")));

      // Checks if there is a pending reply, or reconfiguration needed or both
      if ((primary.get("ack").equals("VOLATILE") || primary.get("ack").equals("MANDATORY"))&& (!rawResponse.toString().equals("{}") || primary.getBoolean("conf_need") || primary.get("ack").equals("MANDATORY") || powerChanged)) {
        // Reads power settings
        int downPw = 0;
        int upPw = 0;
        int spf = 0;

        if (!primary.getBoolean("conf_need")) {
          JSONObject readPower = new JSONObject(programResources.dbHandler.readNode(primary.getString("dev_id")));
          downPw = readPower.getInt("downstream_power");
          upPw = readPower.getInt("upstream_power");
          spf = readPower.getInt("spf");
        } else {
          // If device requests reconfiguration, power is set to full
          downPw = MaxPower;
          upPw = MaxPower;
          spf = MaxSPF;
          this.programResources.dbHandler.updatePower(primary.getString("dev_id"), 0, 0, 0); //set full power for device in DB as well
        }

        // Builds skeleton for a response
        JSONObject txlMsg = new JSONObject();
        JSONArray netData = new JSONArray();
        txlMsg.put("message_name", "TXL");
        JSONObject messageBody = new JSONObject();
        messageBody.put("dev_id", primary.getString("dev_id"));
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
        if (primary.getBoolean("conf_need")) {
          int Transmission_PARAM_ID = EDTransmissionParamID;
          JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(Transmission_PARAM_ID));

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
          emerParam.put("sf", MaxSPF);
          emerParam.put("power", MaxPower); // Fixed max power for emergency Bcast
          emerParam.put("freqs", params.get("emergency_freq_arr"));

          JSONObject regParam = new JSONObject(normalParam.toString());
          regParam.put("type", "REG");
          regParam.put("sf", MaxSPF);
          regParam.put("power", MaxPower); // Fixed max power for reg Bcast
          regParam.put("freqs", params.get("registration_freq_arr"));

          // Creates an array of params as a response
          netData.put(emerParam);
          netData.put(normalParam);
          netData.put(regParam);
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

        System.out.println("New TXL replay for AP");
        System.out.println(txlMsg);

        // Checks for duty Cycle duration of a message
        int remainingDutyC = getRemainingDutyCycle(messageBody, primary.getInt("sf"), primary.getInt("duty_c"));

        // Sends message via desired AP
        int apIdentifier = primary.getInt("apIdentifier");

        // TODO: Uprav ked downlink nepojde kvoli duty cyclu
        if (remainingDutyC > 0) {
          this.programResources.sslConnection.socketThreadArrayList.get(apIdentifier).write(txlMsg.toString());
          System.out.println("********** Raw response " + rawResponse.toString());
          if (rawResponse.toString().equals("{}")) {
            // Writes new wanna-be-sent message
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
            this.programResources.dbHandler.markDownlinkAsSent(rawResponse.getInt("id"), remainingDutyC); // Marks messages as sent in DB
          }
        } else {
          // Version 1.0 does not support network data buffering
          // this.programResources.dbHandler.WriteUnsentMSG_D(message_body.getString("app_data"), primary.getString("hWIdentifier"), message_body.getString("dev_id"));
          System.out.println("Unable to deliver message due to insufficient duty cycle. Oversize of: " + remainingDutyC * (-1));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();;
    }
  }

  /**
   * Calculates metric for selected downlink messages
   * @param rssi
   * @param dutyCycleRemaining
   * @return float
   */
  public float getMetric(int rssi, int dutyCycleRemaining) {
    float dutyCyclePercent = ((dutyCycleRemaining * 100) / maxDutyMillis);

    // If more dutyC remaining than sensitivity boundary does not poison RSSI
    if (dutyCyclePercent > this.dutyCycleSensitivity) {
      return rssi;
    }

    // If less than critical dutyC remaining on a gateway poisons the RSSI by Greater value
    if (dutyCyclePercent < this.dutyCycleRestriction) {
      // If really low duty cycle severely poisons the route
      if (dutyCyclePercent < 5) {
        // System.out.println("*********TotalPoison");
        return rssi - 1000;
      }
      // Else greater poison
      // System.out.println("*********HighPoison");
      return rssi - this.restrictionPoison;
    }
    // If dutyC is between sensitivity and critical boundry poisons the RSSI by medium value
    // System.out.println("*********MediumPoison");
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
        //System.out.println("******Pocitam s KRATKIMI network datami");
      } else if (netData.length() > 1) {
        netDataBytes = 11 + 5;
        //System.out.println("******Pocitam s DLHIMY network datami");
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
    int blockSizeOverhead = Integer.valueOf(appDataBytes + netDataBytes + payloadOverhead) % Integer.valueOf(4);
    System.out.println("Block size overhead is: " + blockSizeOverhead);
    //counts the formula, when all data are present
    float symbolTime = (float) Math.pow(2, spf) / ((float) bandwidth / 1000);
    //System.out.println("****************************Symboltime je: "+Symboltime);
    int msgSymbols  = (int) 8 + ((8 * (netDataBytes + appDataBytes + loraFiitOverheadBytes + payloadOverhead + blockSizeOverhead) - 4 * spf + 28 + 16 ) / (4 * (spf-2))) * (cr + 4);
    //System.out.println("****************************Msg symbolcount je :"+MsgSymbols);

    float msgCost = msgSymbols * symbolTime;
    System.out.println("On-Air time for packet calculated is " + msgCost + " milis");

    return Math.round(msgCost);
  }

  /**
   * Returns remaining duty cycle after a msg is sent
   * @param msgBody
   * @param sf
   * @param dutyCycleBeforeSent
   * @return int
   * @throws Exception
   */
  public int getRemainingDutyCycle(JSONObject msgBody, int sf, int dutyCycleBeforeSent) throws Exception {
    return dutyCycleBeforeSent - getMsgCost(msgBody, sf);
  }

}
