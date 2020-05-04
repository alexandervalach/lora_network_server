package core;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads from and writes values to configuration file
 * @author Karol Cagáň
 * @version 0.3
 */
public class Props {
  private ProgramResources programResources;
  public Properties prop;

  /**
   * Constructor, loads properties
   * @param programResources instance of program resources
   */
  public Props(ProgramResources programResources) {
    try {
      this.programResources = programResources;
      prop = new Properties();
      // Change source properties file here
      InputStream is = new FileInputStream("resources/configuration.config");
      prop.load(is);
      System.out.println(prop.getProperty("Props.loadQuote"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get property as String
   * @param propertyName property name from config file
   * @return String
   */
  public String getStr(String propertyName) {
    return this.prop.getProperty(propertyName);
  }

  /**
   * Get property as int
   * @param propertyName propery name from config file
   * @return String
   */
  public int getInt(String propertyName) {
    return Integer.parseInt(this.prop.getProperty(propertyName));
  }
}
