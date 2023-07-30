package connection;

import core.ProgramResources;
import traffic.MessageController;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;

/**
 * Manages secure connection with AP
 * @author Karol Cagáň
 * @version 0.3.2
 */
public class SSLConnection extends Thread {
  private final String keyStore;
  private final String keyStorePassword;
  private final SSLServerSocket serverSocket;
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
    this.keyStore = System.getenv("LONES_KEYSTORE");
    this.keyStorePassword = System.getenv("LONES_KEYSTORE_PASSWORD");

    System.setProperty("keyStorePassword", this.keyStorePassword);
    System.setProperty("keyStore", this.keyStore);

    // Implements security certificate
    /*
    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    serverSocket = (SSLServerSocket) ssf.createServerSocket(this.programResources.props.getInt("SSLConnection.portNumber"));
    serverSocket.setEnabledProtocols(new String[] {"TLSv1.2"});
     */

    char[] password = this.keyStorePassword.toCharArray();
    KeyStore ks = KeyStore.getInstance("JKS");
    InputStream ksIs = new FileInputStream(this.keyStore);
    try {
      ks.load(ksIs, password);
    } finally {
      ksIs.close();
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password);

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
    SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

    /*
     * DEBUG SECTION
     */
    // Map<String, String> envVars = System.getenv();
    // envVars.forEach((k, v) -> System.out.println((k + "=" + v)));
    /*
      END OF DEBUG SECTION
    */

    System.out.println("LONES_PORT: " + System.getenv("LONES_PORT"));
    serverSocket = (SSLServerSocket) socketFactory.createServerSocket(Integer.parseInt(System.getenv("LONES_PORT")));

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
        s = serverSocket.accept();
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
    serverSocket.close();
  }
}
