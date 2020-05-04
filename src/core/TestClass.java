package core;

import org.json.JSONObject;

/**
 * Calls predetermined test case scenario
 * @author Karol Cagáň
 * @version 0.3
 */
public class TestClass {
  /**
   * Debug program version
   */
  public static void testFunct(ProgramResources programResources) {
    try {
      String testujSETA ="{ \"message_name\": \"SETR\", \"message_body\": { \"ID\": \"22\", \"ver\": \"STIOT v 1.01\", \"channels\": 15, \"sup_feqs\": [50.1,0.2,51.5],\"sup_sfs\": [\"5/7\",\"6/7\"],\"m_chan\": true,\"sup_crs\": [5,8,7],\"sup_bands\": [124,555],\"lora_stand\": {\"name\": \"LoRa@FIIT\",\"version\": \"0.1a\"},\"max_power\": \"10\"}}";
      String testujKEYS ="{ \"message_name\": \"KEYS\", \"message_body\": { \"dev_id\": 22222bbb, \"seq\": 1,\"key\": 1226343482323433200 } }";
      String testujKEYR ="{ \"message_name\": \"KEYR\", \"message_body\": { \"dev_id\": 22222bbb } }";
      String testujRXL ="{\"time\":1491648871,\"dev_id\":\"QUFB\",\"sf\":7,\"cr\":\"4/5\",\"band\":\"250000\",\"rssi\":\"-98\",\"snr\":\"7.0\",\"data\":\"RklJVEtB\",\"conf_need\":true,\"duty_c\":\"20000\",\"ack\":\"VOLATILE\",\"seq\":22310}";
      String testujRXL2 ="{\"time\":1491648899,\"dev_id\":\"QUFB\",\"sf\":7,\"cr\":\"4/5\",\"band\":\"250000\",\"rssi\":\"-80\",\"snr\":\"4.0\",\"data\":\"RklJVEtB\",\"conf_need\":false,\"duty_c\":\"20000\",\"ack\":\"MANDATORY\",\"seq\":22310}";
      String testujRXL3 ="{\"message_name\":\"RXL\",\"message_body\":{\"time\":1491648899,\"dev_id\":\"QUFB\",\"sf\":7,\"cr\":\"4/5\",\"band\":\"250000\",\"rssi\":\"-120\",\"snr\":\"4.0\",\"data\":\"ine\",\"conf_need\":false,\"duty_c\":\"234\",\"ack\":\"UNSUPPORTED\",\"seq\":123}}";
      //process(null, testujRXL, true, 99999);
      //process(null, testujRXL3, true, 88888); //iny
      //process(null, testujRXL2, true, 88888);
      programResources.loRaConcentrator.catchMsg(new JSONObject(testujRXL), 4,"fiitap2", "normal");
      programResources.loRaConcentrator.catchMsg(new JSONObject(testujRXL2), 4,"fiitap2", "normal");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void test1 (ProgramResources resources) {
    try {
      String regrMessage = "{\"band\":125000,\"cr\":\"4/5\",\"dev_id\":\"11111aaa\",\"duty_c\":14475,\"rssi\":-69.0,\"sf\":9,\"snr\":13.5,\"time\":1575200615604},\"message_name\":\"REGR\"}";
      resources.loRaConcentrator.catchMsg(new JSONObject(regrMessage),  1, "99999ff", "reg");
    } catch (Exception e) {
      System.out.println("TEST FAILED");
      e.printStackTrace();
    }
  }
}
