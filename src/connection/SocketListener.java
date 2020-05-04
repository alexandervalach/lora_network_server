package connection;

/**
 * Creates socket listener interface
 * @author Karol Cagáň
 * @version 0.3
 */
public interface SocketListener {
  void process(SocketThread st, String message, boolean online, int apIdentifier);
  void socketDown(int apIdentifier);
}
