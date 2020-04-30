package processor;

import connection.SocketThread;
import core.DateManager;
import core.ProgramResources;
import helpers.MessageHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.ArrayList;

/**
 * AP processing logic
 * @author Karol Cagáň
 * @version 1.0
 */
public class APProcessor {
  private final ProgramResources programResources;
  private final int maxPower;
  private final int apTransmissionParamId;
  private final int edTransmissionParamId;
  private final int downSFSensitivity;
  private final int downPowerSensitivity;
  private final int maxSpf;
  private final String algorithm;

  /**
   * Constructor
   * @param programResources
   */
  public APProcessor(ProgramResources programResources) {
    this.programResources = programResources;
    this.maxPower = programResources.props.getInt("LoRaSettings.maxPower");
    this.apTransmissionParamId = programResources.props.getInt("LoRaSettings.apTransmissionParamId");
    this.edTransmissionParamId = programResources.props.getInt("LoRaSettings.edTransmissionParamId");
    this.downSFSensitivity = programResources.props.getInt("LoRaSettings.powerDownSpfRssiSensitivityBoundary");
    this.downPowerSensitivity = programResources.props.getInt("LoRaSettings.powerDownPowerRssiSensitivityBoundary");
    this.maxSpf = programResources.props.getInt("LoRaSettings.maxSpf");
    this.algorithm = programResources.props.getStr("ServerSetting.algorithm");
    System.out.println("Access Point Processor created successfully!");
  }

  /**
   * Processes SETR messages
   * @param jsonobject
   * @param st
   */
  public void processSETR(JSONObject jsonobject, SocketThread st) {
    // Version 1.0 only supports static params for each AP configuration, change here
    int transmissionParamId = apTransmissionParamId;
    JSONObject setaMsg = new JSONObject();

    try {
      // Writes existing AP into database
      programResources.dbHandler.writeAp(
        jsonobject.getString("id"),
        "STIOT v 1.01",
        jsonobject.getInt("max_power"),
        jsonobject.getInt("channels"),
        Time.valueOf(DateManager.formatDate("00:mm:ss")),
        jsonobject.getJSONObject("lora_stand").getString("name"),
        jsonobject.getJSONObject("lora_stand").getString("version"),
        transmissionParamId
      );

      // Writes HW ID into software handler thread
      st.sethWIdentifier(jsonobject.getString("id"));

      // Gets transmission param for AP from DB
      JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(transmissionParamId));

      // Builds transmission param array
      JSONObject normalParam = new JSONObject();
      normalParam.put("sf", 0); // Multi-spreading factor AP/s are always set on zero
      normalParam.put("cr", params.get("coderate"));
      normalParam.put("band", params.get("bandwidth"));
      normalParam.put("type", "NORMAL");
      normalParam.put("freqs", params.get("standard_freq"));

      // Params for emergency message
      JSONObject emerParam = new JSONObject(normalParam.toString());
      emerParam.put("type", "EMER");
      emerParam.put("freqs", params.get("emergency_freq"));

      // Params for registration message
      JSONObject regParam = new JSONObject(normalParam.toString());
      regParam.put("type", "REG");
      regParam.put("freqs", params.get("registration_freq"));

      // Creates an array of params as a response
      JSONArray messageBody = new JSONArray();
      messageBody.put(emerParam);
      messageBody.put(normalParam);
      messageBody.put(regParam);

      // Builds the answer message
      setaMsg.put("message_name", "SETA");
      setaMsg.put("message_body", messageBody);

      System.out.println("New SETA msg built for AP: " + setaMsg);
      st.write(setaMsg.toString());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Process key from KEYS
   * @param jsonobject
   */
  public void processKEYS(JSONObject jsonobject) {
    try {
      programResources.dbHandler.writeKey(jsonobject.getString("dev_id"), jsonobject.getInt("seq"), jsonobject.getString("key"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns encryption key
   * @param jsonobject
   * @return JSONObject
   */
  public JSONObject processKEYR(JSONObject jsonobject) {
    JSONObject keyAMsg = new JSONObject();

    try {
      JSONObject params = new JSONObject(programResources.dbHandler.readNode(jsonobject.getString("dev_id")));

      JSONObject messageBody = new JSONObject();
      messageBody.put("dev_id", jsonobject.getString("dev_id"));
      messageBody.put("seq", params.get("last_seq"));
      messageBody.put("key", params.get("dh_key"));

      keyAMsg.put("message_name", "KEYA");
      keyAMsg.put("message_body", messageBody);

      System.out.println("New KEYA msg built for AP " + keyAMsg);

      return keyAMsg;
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return keyAMsg;
  }

  /**
   * Process registration request
   * @param currentGrape
   */
  public void processREGR(ArrayList<JSONObject> currentGrape) {
    String preSharedKey = programResources.props.getStr("APProcessor.preSharedKey");
    // System.out.println("Pre Shared Key: " + preSharedKey);

    try{
      System.out.println("Registering batch " + currentGrape.toString());

      // Determines which message is used as primary
      JSONObject primary = null;

      for (JSONObject jsonObject : currentGrape) {
        if (primary == null) {
          primary = jsonObject;
        } else {
          // Determine the best downlink candidate
          if (programResources.edProcessor.getMetric(primary.getInt("rssi"), primary.getInt("duty_c"))<programResources.edProcessor.getMetric(jsonObject.getInt("rssi"), jsonObject.getInt("duty_c"))) {
            primary = jsonObject;
          }
        }
      }
      int apIdentifier = primary.getInt("apIdentifier");

      // If message is received with exceptional quality decreases up power already, otherwise set power to max
      int upPw = maxPower;
      int downPw = maxPower;
      int spf = maxSpf;

      // Determine transmission power down
      if (primary.getInt("rssi") > this.downPowerSensitivity) {
        if (primary.getInt("rssi") > this.downSFSensitivity) {
          spf--;
        } else {
          upPw--;
          downPw--;
        }
      }

      // Version 1.0 only supports one application, change here
      int appId = 1;

      // Version 1.0 only supports one default transmission param for all ED, place transmission param logic here
      int Transmission_PARAM_ID = edTransmissionParamId;

      // Writes node into DB
      programResources.dbHandler.writeNode(
        primary.getString("dev_id"),
        upPw,
        downPw,
        spf,
        DateManager.formatDate("00:mm:ss"),
        appId,
        Transmission_PARAM_ID
      );

      // Preparing response
      JSONObject REGAmsg = new JSONObject();

      //gets transmission param for AP from DB
      JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(Transmission_PARAM_ID));

      //builds transmission param array
      JSONObject normalParam = new JSONObject();
      normalParam.put("sf", spf);
      normalParam.put("cr", params.get("coderate"));
      normalParam.put("band", params.get("bandwidth"));
      normalParam.put("type", "NORMAL");
      normalParam.put("power", upPw);
      normalParam.put("freqs", params.get("standard_freq"));

      JSONObject emerParam = new JSONObject(normalParam.toString());
      emerParam.put("type", "EMER");
      emerParam.put("sf", maxSpf);
      emerParam.put("power", maxPower); // Fixed max power for emergency Bcast
      emerParam.put("freqs", params.get("emergency_freq"));

      JSONObject regParam = new JSONObject(normalParam.toString());
      regParam.put("type", "REG");
      regParam.put("sf", maxSpf);
      regParam.put("power", maxPower); // Fixed max power for reg Bcast
      regParam.put("freqs", params.get("registration_freq"));

      //creates an array of params as a response
      JSONArray netData = new JSONArray();
      netData.put(emerParam);
      netData.put(normalParam);
      netData.put(regParam);

      //creates response body
      JSONObject messageBody = new JSONObject();
      messageBody.put("dev_id", primary.getString("dev_id"));
      messageBody.put("power", upPw);
      messageBody.put("sh_key", preSharedKey);
      messageBody.put("app_data", ""); // Version 1.0 does not support app data on first downlink message
      messageBody.put("net_data", netData);

      // Builds the reply message
      REGAmsg.put("message_name","REGA");
      REGAmsg.put("message_body", messageBody);

      messageBody.put("time", MessageHelper.getMsgCost(messageBody, primary.getInt("sf"), primary.getInt("band")));

      System.out.println("New REGA msg created for AP: " + REGAmsg);
      // Sends answer do desired AP
      this.programResources.sslConnection.socketThreadArrayList.get(apIdentifier).write(REGAmsg.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
