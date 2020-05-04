package traffic;

import core.ProgramResources;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Concentrates messages from different concentrators
 * @author Karol Cagáň
 * @version 0.3
 */
public class LoRaConcentrator {
  public ProgramResources programResources;
  private final HashMap<String, ArrayList<JSONObject>> matchingTable;
  private final boolean banditAlgorithm;

  /**
   * Constructor
   * @param programResources instance of base program resources
   */
  public LoRaConcentrator(ProgramResources programResources) {
    this.programResources = programResources;
    String algorithm = programResources.props.getStr("ServerSetting.algorithm");
    this.banditAlgorithm = algorithm.equals("ucb") || algorithm.equals("ts");
    matchingTable = new HashMap<>();
  }

  /**
   * Inserts messages into hashtable for matching communication
   * @param jsonObject received JSON object
   * @param apIdentifier sender local identifier
   * @param hWID sender global identifier
   * @param type message type
   * @throws Exception
   */
  public void catchMsg(JSONObject jsonObject, int apIdentifier, String hWID, String type) throws Exception {
    // Appends the JSON with internal AP identifier for future callback
    jsonObject.put("apIdentifier", apIdentifier);
    jsonObject.put("hWIdentifier", hWID);
    ArrayList<JSONObject> myGrape;

    boolean isRegistration = type.equals("reg");
    // Builds string hash key
    String key;

    if (isRegistration) {
      key = jsonObject.getString("dev_id");
    } else {
      key = jsonObject.getString("data") + jsonObject.getString("dev_id");
    }

    synchronized (matchingTable) {
      myGrape = matchingTable.get(key);
    }

    if (myGrape == null) {
      // Key is not present in table, creates a new entry
      myGrape = new ArrayList<JSONObject>();
      myGrape.add(jsonObject);

      synchronized (matchingTable) {
        matchingTable.put(key, myGrape);
      }
      // Starts synchronization timer
      new ConcentratorDeadTimer(key, this, type).start();
    } else {
      // Message already caught from different AP, appends the message
      myGrape.add(jsonObject);
      synchronized (matchingTable) {
        matchingTable.replace(key, myGrape);
      }
    }
  }

  /**
   * After the messages have been synchronized clears the hashmap and handles messages
   * @param key matching table key
   */
  public void finalize(String key) {
    ArrayList<JSONObject> currentGrape;

    synchronized (matchingTable) {
      currentGrape = matchingTable.remove(key);
    }

    // Calls server logic to process the messages
    synchronized (programResources.edProcessor) {
      if (this.banditAlgorithm) {
        programResources.apProcessor.updateBandits(currentGrape);
      }
      programResources.edProcessor.processRXL(currentGrape);
    }
  }

  /**
   * After the messages have been synchronized clears the hashmap and handles messages
   * @param key
   */
  public void register(String key) {
    ArrayList<JSONObject> currentGrape;

    synchronized (matchingTable) {
      currentGrape = matchingTable.remove(key);
    }

    // Calls server logic to process the messages
    synchronized (programResources.edProcessor) {
      if (this.banditAlgorithm) {
        programResources.apProcessor.updateBandits(currentGrape);
      }
      programResources.apProcessor.processREGR(currentGrape);
    }
  }
}
