package traffic;

import connection.SocketListener;
import connection.SocketThread;
import core.DateManager;
import core.ProgramResources;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles all incoming messages
 * @author Karol Cagáň
 * @version 0.3
 */
public class MessageController implements SocketListener {
  private final ProgramResources programResources;

  /**
   * Constructor
   * @param programResources instance of possible program resources
   */
  public MessageController(ProgramResources programResources) {
    System.out.println("Message Controller created successfully!");
    this.programResources = programResources;
  }

  /**
   * Processes a received message
   * @param st local socket thread
   * @param message received message
   * @param online is ap online
   * @param apIdentifier local ap identifier
   */
  public void process(SocketThread st, String message, boolean online, int apIdentifier) {
    System.out.println(DateManager.formatDate("dd.MM.yyyy>HH:mm:ss") + ": Received a new message from " + apIdentifier);
    System.out.println("Message content: " + message);

    try {
      JSONObject jsonMessage = new JSONObject(message);
      // Calls service program according to message type
      JSONObject messageBody = jsonMessage.getJSONObject("message_body");
      String type;

      switch (jsonMessage.getString("message_name")) {
        case "RXL":
        case "REGR":
          try {
            type = messageBody.getString("type");
          } catch (JSONException e) {
            System.out.println("Message type not found, setting message type to: NORMAL");
            type = "normal";
          }
          programResources.loRaConcentrator.catchMsg(messageBody, apIdentifier, st.getHwIdentifier(), type);
          break;
        case "KEYR":
          JSONObject responseKEYR = programResources.apProcessor.processKEYR(messageBody);
          st.write(responseKEYR.toString());
          break;
        case "KEYS":
          programResources.apProcessor.processKEYS(messageBody);
          break;
        case "SETR":
          programResources.apProcessor.processSETR(messageBody, st);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Closes connection with AP
   * @param apIdentifier local access point identifier
   */
  public void socketDown(int apIdentifier) {
    System.out.println("Connection with " + apIdentifier + " has been lost!");
  }

}
