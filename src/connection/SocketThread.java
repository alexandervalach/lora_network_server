package connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Creates socket thread for listening
 * @author Karol Cagáň
 * @version 0.3
 */
public class SocketThread extends Thread
{
  public volatile boolean running = true;
  private final Socket socket;
  private OutputStream outStream = null;
  private InputStream inStream = null;
  private final ProcessThread processThread;
  private String hWIdentifier;
  private final int internalIdentifier;

  /**
   * Constructor for new instance
   * @param socket instance of socket
   * @param listener instance of socket listener
   * @param id internal ap identifier
   */
  public SocketThread(Socket socket, SocketListener listener, int id) {
    this.socket = socket;
    this.processThread = new ProcessThread(listener, this);
    this.internalIdentifier = id;
    System.out.println("New AP listener created for AP of internal ID: " + this.internalIdentifier);
  }

  @Override
  public void run() {
    super.run();

    // Starting message processing thread
    new Thread(processThread).start();

    // While running listens for incoming messages
    while (running && socket.isConnected()) {
      try {
        String inData = this.read();
        System.out.println(inData);
        processThread.putToQueue(inData);
      } catch (IOException | InterruptedException e) {
        break;
      }
    }

    // Turn off function
    try {
      processThread.running = false;
      processThread.putToQueue("ENDING");

      if (outStream != null) {
        outStream.close();
      }

      if (inStream != null) {
        inStream.close();
      }

      if (socket != null) {
        socket.close();
      }

      if (this.processThread.listener != null) {
        this.processThread.listener.socketDown(internalIdentifier);
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Sends downlink message to AP
   * @param jsonText json message as a string
   */
  public void write(String jsonText) throws IOException {
    outStream = socket.getOutputStream();
    outStream.write(jsonText.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * hWIdentifier serves for downlink AP identification
   * @return String
   */
  public String getHwIdentifier() {
    return hWIdentifier;
  }

  /**
   * Setter for ap identifier
   * @param hWIdentifier access point id
   */
  public void setHwIdentifier(String hWIdentifier) {
    this.hWIdentifier = hWIdentifier;
  }

  /**
   * Reads data from buffer, returns JSON string
   */
  private String read() throws IOException {
    inStream = socket.getInputStream();
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;

    do {
      length = inStream.read(buffer);

      if (length <= 0) {
        throw new IOException("Connection closed");
      }

      result.write(buffer, 0, length);

    } while (inStream.available() != 0);

    return result.toString(StandardCharsets.UTF_8);
  }

  /**
   * Unprocessed messages wain in a quee until server is ready to accept them
   */
  private class ProcessThread implements Runnable {
    private volatile boolean running = true;
    private final LinkedBlockingQueue<String> jobQueue;
    private final SocketListener listener;
    private final SocketThread parent;

    public ProcessThread(SocketListener listener, SocketThread parent) {
      this.listener = listener;
      this.parent = parent;
      this.jobQueue = new LinkedBlockingQueue<>();
    }

    public void putToQueue(String insert) throws InterruptedException {
      this.jobQueue.put(insert);
    }

    @Override
    public void run() {
      System.out.println("Starting process thread");
      String inData = null;

      while (running) {
        try {
          inData = jobQueue.take();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        // NETWORK DOWN
        if (!running){
          break; // lasts messages + ENDING message
        }
        this.listener.process(parent, inData, running, internalIdentifier);
      }

      // Powerdown function
      if (!running) {
        int leftover = jobQueue.size() - 1; // REMOVE ENDING message
        for (int i =0; i<leftover; i++) {
          inData = jobQueue.poll();
          this.listener.process(parent, inData, false, internalIdentifier);
        }
      }
    }
  }
}
