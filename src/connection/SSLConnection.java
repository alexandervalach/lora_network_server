package connection;

import core.ProgramResources;
import traffic.MessageController;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Manages secure connection with AP
 * @author Karol Cagáň
 * @version 0.3.2
 */
public class SSLConnection extends Thread {
  static final String keyStore = "Javax.net.ssl.keyStore";
  static final String keyStorePassword = "Javax.net.ssl.keyStorePassword";
  private final SSLServerSocket serverSocket;
  private final SocketListener messageController;
  public ArrayList<SocketThread> socketThreadArrayList;
  private int apIterator;

  /***
   * Constructor
   * @param programResources instance of program resources
   */
  public SSLConnection(ProgramResources programResources) throws Exception {
    System.setProperty(keyStore, programResources.props.getStr(keyStore));
    System.setProperty(keyStorePassword, programResources.props.getStr(keyStorePassword));

    char[] password = programResources.props.getStr(keyStorePassword).toCharArray();
    KeyStore ks = KeyStore.getInstance("JKS");
    try (InputStream ksIs = new FileInputStream(programResources.props.getStr(keyStore))) {
      ks.load(ksIs, password);
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password);

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
    SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
    serverSocket = (SSLServerSocket) socketFactory.createServerSocket(programResources.props.getInt("SSLConnection.portNumber"));

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
