package traffic;

/**
 * Waits for LoRa messages to synchronize
 * @author Karol Cagáň
 * @version 1.0
 */
public class ConcentratorDeadTimer extends Thread {
  private String key;
  private LoRaConcentrator parent;
  private boolean isRegistration;
  private int sleepTime;

  /**
   * Constructor
   * @param key
   * @param parent
   * @param isRegistration
   */
  public ConcentratorDeadTimer(String key, LoRaConcentrator parent, boolean isRegistration) {
    this.key = key;
    this.parent = parent;
    this.isRegistration = isRegistration;
    this.sleepTime = parent.programResources.props.getInt("ConcentratorDeadTimer.waitTime");
    // this.run();
  }

  public void run() {
    try {
      // Time to wait for all messages to come, e.g. synchronization timer
      sleep(this.sleepTime);

      if (!isRegistration) {
        parent.finalize(key);
      } else {
        parent.register(key);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
