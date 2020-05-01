package traffic;

import connection.SocketListener;
import connection.SocketThread;
import core.DateManager;
import core.ProgramResources;
import org.json.JSONObject;

/**
 * Handles all incoming messages
 * @author Karol Cagáň
 * @version 1.0
 */
public class MessageController implements SocketListener {
  private ProgramResources programResources;

  /**
   * Constructor
   * @param programResources
   */
  public MessageController(ProgramResources programResources) {
    System.out.println("Message Controller created successfully!");
    this.programResources = programResources;
  }

  /**
   * Processes a received message
   * @param st
   * @param message
   * @param online
   * @param apIdentifier
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
          type = messageBody.getString("type");
          programResources.loRaConcentrator.catchMsg(messageBody, apIdentifier, st.gethWIdentifier(), type);
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
   * @param apIdentifier
   */
  public void socketDown(int apIdentifier) {
    System.out.println("Connection with " + apIdentifier + " has been lost!");
  }

}
