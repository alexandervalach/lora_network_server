package helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/***
 * Message helper useful functions
 */
public class MessageHelper {
  public static int getMsgCost(JSONObject msgBody, int spf, int bandwidth) {
    // Values initialization
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
        // System.out.println("**** Short network data");
      } else if (netData.length() > 1) {
        netDataBytes = 11 + 5;
        // System.out.println("**** Long network data");
      }
    } catch (JSONException e) {
      // System.out.println("Network data are not present");
    }

    try {
      appData = msgBody.getString("app_data");
    } catch (JSONException e) {
      // System.out.println("App data are not present");
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

    // Calculates padding size
    int blockSizeOverhead = (appDataBytes + netDataBytes + payloadOverhead) % 4;
    // System.out.println("Block size overhead is: " + blockSizeOverhead);
    //counts the formula, when all data are present
    float symbolTime = (float) Math.pow(2, spf) / ((float) bandwidth / 1000);
    // System.out.println("***** Symbol time is: " + symbolTime);
    int msgSymbols  = (int) 8 + ((8 * (netDataBytes + appDataBytes + loraFiitOverheadBytes + payloadOverhead + blockSizeOverhead) - 4 * spf + 28 + 16 ) / (4 * (spf-2))) * (cr + 4);
    // System.out.println("***** Msg symbol count is :" + msgSymbols);
    float msgCost = msgSymbols * symbolTime;
    // System.out.println("On-Air time for packet calculated is " + msgCost + " milis");

    return Math.round(msgCost);
  }

  /***
   * Updates bandit arms rewards and returns json array
   * @param bandits JSON array with bandit arms
   * @param sf spreading factor of arm
   * @param power power of amr
   * @param reward reward increment/decrement value for selected arm
   * @return new JSON bandits
   * @throws JSONException
   */
  public static void updateStatModel (JSONArray bandits, int sf, int power, int reward) throws JSONException {
    for (int i = 0; i < bandits.length() ; i++) {
      JSONObject bandit = bandits.getJSONObject(i);
      if (bandit.getInt("sf") == sf && bandit.getInt("pw") == power) {
        int updatedReward = bandit.getInt("rw") + reward;
        bandit.put("rw", updatedReward);
        System.out.println("Updated bandit arm SF=" + sf + " PWR=" + power + " to RW=" + reward);
      }
    }
  }

}
