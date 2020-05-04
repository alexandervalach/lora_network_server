package core;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date and time helper
 * @author Karol Cagáň
 * @version 0.3
 */
public class DateManager {
  /**
   * Returns date as the posted format
   * @param formatter string datetime format
   * @return String
   */
  public static String formatDate(String formatter) {
    Date date = new Date();
    DateFormat writeFormat = new SimpleDateFormat(formatter);
    return writeFormat.format(date);
  }

  public static Timestamp getTimestamp() {
    Date date = new Date();
    return new Timestamp(date.getTime());
  }
}
