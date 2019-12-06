package traffic;

import core.ProgramResources;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Concentrates messages from different concentrators
 * @author Karol Cagáň
 * @version 1.0
 */
public class LoRaConcentrator {
  public ProgramResources programResources;
  private HashMap<String, ArrayList<JSONObject>> matchingTable;

  /**
   * Constructor
   * @param programResources
   */
  public LoRaConcentrator(ProgramResources programResources) {
    this.programResources = programResources;
    matchingTable = new HashMap<String, ArrayList<JSONObject>>();
  }

  /**
   * Inserts messages into hashtable for matching communication
   * @param jsonObject
   * @param apIdentifier
   * @param hWID
   * @param isRegistration
   * @throws Exception
   */
  public void catchMsg(JSONObject jsonObject, int apIdentifier, String hWID, boolean isRegistration) throws Exception {
    // Appends the JSON with internal AP identifier for future callback
    jsonObject.put("apIdentifier", apIdentifier);
    jsonObject.put("hWIdentifier", hWID);
    ArrayList<JSONObject> myGrape;
    String key;

    // Builds string hashkey
    key = isRegistration ? jsonObject.getString("dev_id") : jsonObject.getString("data") + jsonObject.getString("dev_id");

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
      new ConcentratorDeadTimer(key, this, isRegistration).start();
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
   * @param key
   */
  public void finalize(String key) {
    ArrayList<JSONObject> currentGrape;

    synchronized (matchingTable) {
      currentGrape = matchingTable.remove(key);
    }

    // Calls server logic to process the messages
    synchronized (programResources.edProcessor) {
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
      programResources.apProcessor.processREGR(currentGrape);
    }
  }
}
