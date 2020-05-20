package connection;

import core.ProgramResources;
import traffic.MessageController;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Manages secure connection with AP
 * @author Karol Cagáň
 * @version 0.3
 */
public class SSLConnection extends Thread {
  private final ServerSocket ss;
  private final SocketListener messageController;
  public ArrayList<SocketThread> socketThreadArrayList;
  private ProgramResources programResources;
  private int apIterator;

  /***
   * Constructor
   * @param programResources instance of program resources
   */
  public SSLConnection(ProgramResources programResources) throws Exception {
    this.programResources = programResources;
    System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "changeit");

    // Implements security certificate
    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    ss = ssf.createServerSocket(this.programResources.props.getInt("SSLConnection.portNumber"));
    messageController = new MessageController(programResources);
    socketThreadArrayList = new ArrayList<>();
    apIterator = 0;
    System.out.println("Listener for connecting AP-s created successfully!" );
  }

  /**
   * Execution thread - infinite loop for catching new AP-s
   */
  public void run() {
    while (true) {
      Socket s;
      System.out.println("Listener thread started!");
      try {
        // New AP detected
        s = ss.accept();
        System.out.println("New AP detected on link!");
        SocketThread st = new SocketThread(s, messageController, apIterator);

        // New AP added into list of AP-s
        socketThreadArrayList.add(apIterator, st);
        apIterator++;
        st.start();
      } catch (IOException e) {
        try {
          this.shutdown();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
        e.printStackTrace();
      }
    }
  }

  /**
   * Shutdown function
   */
  public void shutdown() throws Exception {
    for (SocketThread socketThread : socketThreadArrayList) {
      socketThread.running = false;
    }
    ss.close();
  }
}
