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
 * @version 0.3
 */
public class APProcessor extends NodeProcessor {
  private final int maxPower;
  private final int apTransmissionParamId;
  private final int edTransmissionParamId;
  private final int downSFSensitivity;
  private final int downPowerSensitivity;
  private final int maxSpf;

  /**
   * Constructor
   * @param programResources instance of basic program resources
   */
  public APProcessor(ProgramResources programResources) {
    super(programResources);
    this.maxPower = programResources.props.getInt("LoRaSettings.maxPower");
    this.apTransmissionParamId = programResources.props.getInt("LoRaSettings.apTransmissionParamId");
    this.edTransmissionParamId = programResources.props.getInt("LoRaSettings.edTransmissionParamId");
    this.downSFSensitivity = programResources.props.getInt("LoRaSettings.powerDownSpfRssiSensitivityBoundary");
    this.downPowerSensitivity = programResources.props.getInt("LoRaSettings.powerDownPowerRssiSensitivityBoundary");
    this.maxSpf = programResources.props.getInt("LoRaSettings.maxSpf");
    System.out.println("Access Point Processor created successfully!");
  }

  /**
   * Processes SETR messages
   * @param message json STIoT message
   * @param st instance of thread
   */
  public void processSETR(JSONObject message, SocketThread st) {
    // Version 1.0 only supports static params for each AP configuration, change here
    int transmissionParamsId = apTransmissionParamId;
    JSONObject setaMsg = new JSONObject();

    try {
      // Writes existing AP into database
      programResources.dbHandler.writeAp(
        message.getString("id"),
        "STIOT v 1.01",
        message.getInt("max_power"),
        message.getInt("channels"),
        Time.valueOf(DateManager.formatDate("00:mm:ss")),
        message.getJSONObject("lora_stand").getString("name"),
        message.getJSONObject("lora_stand").getString("version"),
        transmissionParamsId
      );

      // Writes HW ID into software handler thread
      st.setHwIdentifier(message.getString("id"));
      // Gets transmission param for AP from DB
      JSONObject params = new JSONObject(programResources.dbHandler.readTransmissionParams(transmissionParamsId));

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
   * @param message json STIoT message
   */
  public void processKEYS(JSONObject message) {
    try {
      programResources.dbHandler.writeKey(
        message.getString("dev_id"),
        message.getInt("seq"),
        message.getString("key")
      );
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns encryption key
   * @param message json message
   * @return JSONObject
   */
  public JSONObject processKEYR(JSONObject message) {
    JSONObject keyAMsg = new JSONObject();
    try {
      String devId = message.getString("dev_id");
      JSONObject params = new JSONObject(programResources.dbHandler.readNode(devId));
      JSONObject messageBody = new JSONObject();
      messageBody.put("dev_id", devId);
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
   * @param currentGrape array list of message replicas
   */
  public void processREGR(ArrayList<JSONObject> currentGrape) {
    String preSharedKey = programResources.props.getStr("APProcessor.preSharedKey");

    try {
      System.out.println("Registering batch " + currentGrape.toString());
      JSONObject primary = this.getPrimaryMessage(currentGrape);
      int apIdentifier = primary.getInt("apIdentifier");

      // If message is received with exceptional quality decreases up power already, otherwise set power to max
      int upPw = maxPower;
      int downPw = maxPower;
      int spf = maxSpf;

      // Determine transmission power down
      int rssi = primary.getInt("rssi");
      if (rssi > this.downPowerSensitivity) {
        if (rssi > this.downSFSensitivity) {
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
      JSONObject messageBody = this.getRegaBody(primary, preSharedKey, spf, upPw, Transmission_PARAM_ID);

      if (messageBody == null) {
        return;
      }

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

  public JSONObject getRegaBody (JSONObject primary, String psk, int spf, int upPw, int transmissionParamsId) {
    try {
      // Creates response body
      JSONObject messageBody = new JSONObject();
      JSONArray netData;
      String devId = primary.getString("dev_id");

      if (this.isBanditAlgorithm) {
        netData = this.getStatModel(primary.getString("hWIdentifier"));
        this.programResources.dbHandler.writeStatModel(devId, netData);
      } else {
        netData = this.getNetData(spf, upPw, transmissionParamsId);
      }

      messageBody.put("dev_id", devId);
      messageBody.put("power", upPw);
      messageBody.put("sh_key", psk);
      messageBody.put("app_data", ""); // Version 1.0 does not support app data on first downlink message
      messageBody.put("net_data", netData);
      return messageBody;
    } catch (JSONException e) {
      return null;
    }
  }

  public JSONArray getStatModel(String apId) {
    try {
      return new JSONArray(programResources.dbHandler.readApStatModel(apId));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void updateBandits(ArrayList<JSONObject> currentGrape) {
    /*
    try {
      JSONObject primary = this.getPrimaryMessage(currentGrape);
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
      JSONObject messageBody;

      if (this.isBanditAlgorithm) {
        messageBody = this.apBanditConfiguration(apIdentifier);
      } else {
        messageBody = this.adrConfiguration(primary, preSharedKey, spf, upPw, Transmission_PARAM_ID);
      }

      if (messageBody == null) {
        return;
      }

      // Builds the reply message
      REGAmsg.put("message_name", "REGA");
      REGAmsg.put("message_body", messageBody);
      messageBody.put("time", MessageHelper.getMsgCost(messageBody, primary.getInt("sf"), primary.getInt("band")));

      System.out.println("New REGA msg created for AP: " + REGAmsg);
      // Sends answer do desired AP
      this.programResources.sslConnection.socketThreadArrayList.get(apIdentifier).write(REGAmsg.toString());
    } catch (JSONException e) {

    }
    */
  }
}
