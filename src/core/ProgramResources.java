package core;

import connection.SSLConnection;
import database.DBHandler;
import processor.APProcessor;
import processor.EDProcessor;
import traffic.LoRaConcentrator;

import java.io.File;
import java.io.PrintStream;

/**
 * Loads all other resources
 * @author Karol Cagáň
 * @author Alexander Valach
 * @version 0.3
 */
public class ProgramResources {
  // Program resources definition
  public DBHandler dbHandler;
  public SSLConnection sslConnection;
  public APProcessor apProcessor;
  public LoRaConcentrator loRaConcentrator;
  public EDProcessor edProcessor;
  public Props props;

  /**
   * Program resources initialization
   * @param test decides whether test or other environment
   */
  public ProgramResources(boolean test) {
    try {
      if (test) {
        this.props = new Props(this);
        System.out.println(DateManager.formatDate("dd.MM.yyyy HH:mm:ss") + " Logging started");
        this.loRaConcentrator = new LoRaConcentrator(this);
        this.apProcessor = new APProcessor(this);
        this.dbHandler = new DBHandler(this);
        this.edProcessor = new EDProcessor(this);

        // Testing the fundamentals functions
        TestClass.testFunct(this);
        TestClass.test1(this);

      } else {
        this.props = new Props(this);
        try (PrintStream o = new PrintStream("logs/" + props.getStr("ServerSetting.logFile"))) {
          // Sets logging output to log file
          System.setOut(o);
        }
        System.out.println(DateManager.formatDate("dd.MM.yyyy HH:mm:ss") + " Logging started");
        this.loRaConcentrator = new LoRaConcentrator(this);
        this.sslConnection = new SSLConnection(this);
        this.apProcessor = new APProcessor(this);
        this.dbHandler = new DBHandler(this);
        this.edProcessor = new EDProcessor(this);
        sslConnection.start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
