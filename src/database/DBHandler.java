package database;

import core.DateManager;
import core.ProgramResources;
import org.json.JSONObject;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Handles database connection, read and write operations
 * @author Karol Cagáň
 * @author Alexander Valach
 * @version 1.0
 */
public class DBHandler {
  private String JDBC_DRIVER;
  private String DB_URL;

  // DB credentials
  private String USER;
  private String PASS;

  private Connection conn = null;
  private PreparedStatement preparedStmt = null;
  private ResultSet rs = null;
  private int maxPower;
  private int maxSPF;
  private int rssiHarmonizationLimit;
  private Statement stmt;
  private DateFormat dateFormat;

  /**
   * Class initialization
   * @param programResources
   */
  public DBHandler(ProgramResources programResources) {
    this.JDBC_DRIVER = programResources.props.getStr("DBHandler.JDBC_DRIVER");
    this.DB_URL = programResources.props.getStr("DBHandler.DB_URL");
    this.USER = programResources.props.getStr("DBHandler.USER");
    this.PASS = programResources.props.getStr("DBHandler.PASS");
    this.maxSPF = programResources.props.getInt("LoRaSettings.maxSpf");
    this.maxPower = programResources.props.getInt("LoRaSettings.maxPower");
    this.rssiHarmonizationLimit = programResources.props.getInt("LoRaSettings.rssiHarmonizingMsgCount");
    this.dateFormat = new SimpleDateFormat("hh:mm:ss");
    this.connect();
    System.out.println("Database Handler created successfully!");
  }

  /**
   * Connection establishment and environment setup
   */
  public void connect() {
    // Driver registration
    try {
      Class.forName(this.JDBC_DRIVER);
      System.out.println("Connecting to database...");
      this.conn = DriverManager.getConnection(DB_URL, USER, PASS);
      this.stmt = conn.createStatement();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Connected!");
  }

  /**
   * Writes a new AP into DB
   * @param id
   * @param protocolVersion
   * @param maxPower
   * @param channelsNum
   * @param dutyCycleRefresh
   * @param loraProtocol
   * @param loraProtocolVer
   * @param transmissionParamId
   */
  public void writeAp(String id, String protocolVersion, int maxPower, int channelsNum, String dutyCycleRefresh, String loraProtocol, String loraProtocolVer, int transmissionParamId) {
    try {
      preparedStmt = conn.prepareStatement("INSERT INTO aps (id, protocol_ver, max_power, channels_num, duty_cycle_refresh, lora_protocol, lora_protocol_ver, transmission_param_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      preparedStmt.setString(1, id);
      preparedStmt.setString(2, protocolVersion);
      preparedStmt.setInt(3, maxPower);
      preparedStmt.setInt(4, channelsNum);
      preparedStmt.setString(5, dutyCycleRefresh);
      preparedStmt.setString(6, loraProtocol);
      preparedStmt.setString(7, loraProtocolVer);
      preparedStmt.setInt(8, transmissionParamId);
      preparedStmt.executeUpdate();
      System.out.println("New AP written into database with ID: " + id);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes new KEY and SEQ for selected node
   * @param id
   * @param lastSeq
   * @param dhKey
   */
  public void writeKey(String id, int lastSeq, String dhKey) {
    try {
      if (!dhKey.equals("")) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET dh_key = ?, last_seq = ? WHERE id = ?");
        preparedStmt.setString(1, dhKey);
        preparedStmt.setInt(2, lastSeq);
        preparedStmt.setString(3, id);
        preparedStmt.executeUpdate();
        System.out.println("New KEY written into database for node ID: " + id);
      } else {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET last_seq = ? WHERE id = ?");
        preparedStmt.execute();
        System.out.println("Seq updated for node ID: " + id);
      }
      preparedStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes new uplink message into DB
   * @param appData
   * @param snr
   * @param rssi
   * @param dutyCRemaining
   * @param isPrimary
   * @param receivedTime
   * @param msgGroupNumber
   * @param apId
   * @param nodeId
   */
  public void writeUplinkMsg(String appData, float snr, float rssi, int dutyCRemaining, boolean isPrimary, Timestamp receivedTime, int msgGroupNumber, String apId, String nodeId) {
    try {
      preparedStmt = conn.prepareStatement("INSERT INTO uplink_messages (app_data, snr, rssi, duty_cycle_remaining, is_primary, receive_time, msg_group_number, ap_id, node_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
      preparedStmt.setString(1, appData);
      preparedStmt.setFloat(2, snr);
      preparedStmt.setFloat(3, rssi);
      preparedStmt.setInt(4, dutyCRemaining);
      preparedStmt.setBoolean(5, isPrimary);
      preparedStmt.setTimestamp(6, receivedTime);
      preparedStmt.setInt(7, msgGroupNumber);
      preparedStmt.setString(8, apId);
      preparedStmt.setString(9, nodeId);
      preparedStmt.executeUpdate();
      System.out.println("New UPLINK MSG written into database for AP " + apId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Writes new node into DB
  public void WriteNode(String id, int upPower, int downPower, int spf, String formattedDate, int appId, int transmissionParam) {
    try {
      preparedStmt = conn.prepareStatement("INSERT INTO nodes (id, upstream_power, downstream_power, spf, duty_cycle_refresh, application_id, transmission_param_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
      preparedStmt.setString(1, id);
      preparedStmt.setInt(2, upPower);
      preparedStmt.setInt(3, downPower);
      preparedStmt.setInt(4, spf);
      preparedStmt.setTime(5, new Time(dateFormat.parse(formattedDate).getTime()));
      preparedStmt.setInt(6, appId);
      preparedStmt.setInt(7, transmissionParam);
      preparedStmt.executeUpdate();
      System.out.println("New Node written into database: " + id);
    } catch (SQLException | ParseException e) {
      e.printStackTrace();
      try {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET dh_key = '' WHERE ID = ?");
        preparedStmt.setString(1, id);
        preparedStmt.executeUpdate();
        System.out.println("DH key deleted for node: " + id);
      } catch (SQLException e1) {
        e1.printStackTrace();
        this.increasePower(id, 0, 0, 0);
      }
    }

  }

  /**
   * Schedule new downlink messages
   * @param appData
   * @param apId
   * @param nodeId
   */
  public void writeUnsentDownlinkMsg(String appData, String apId, String nodeId) {
    try {
      // Version 1.0 does not support downstream ACK edit here
      preparedStmt = conn.prepareStatement("INSERT INTO downlink_messages (app_data, sent, delivered, ap_id, node_id) VALUES (?, false, false, ?, ?)");
      preparedStmt.setString(1, appData);
      preparedStmt.setString(2, apId);
      preparedStmt.setString(3, nodeId);
      preparedStmt.executeUpdate();
      System.out.println("New Unsent MSG_D written into database for node " + nodeId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Update entry about sent downlink message
   * @param appData
   * @param apId
   * @param nodeId
   */
  public void writeSentDownlinkMsg(String appData, String netData, int dutyCRemaining, String apId, String nodeId) {
    try {
      // Version 1.0 does not support downstream ACK edit here
      preparedStmt = conn.prepareStatement("INSERT INTO downlink_messages (app_data, duty_cycle_remaining, sent, ack_required, delivered, send_time, ap_id, node_id, net_data) VALUES (?, ?, true, false, true, ? ,? ,? ,?)");
      preparedStmt.setString(1, appData);
      preparedStmt.setInt(2, dutyCRemaining);
      preparedStmt.setTimestamp(3, DateManager.getTimestamp());
      preparedStmt.setString(4, apId);
      preparedStmt.setString(5, nodeId);
      preparedStmt.setString(6, netData);
      preparedStmt.executeUpdate();
      System.out.println("New Sent MSG_D written into database for Node: " + nodeId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Updates node power settings in DB
   * @param nodeId
   * @param upPowerDecrement
   * @param downPowerDecrement
   * @param spfDecrement
   * @return boolean
   */
  public boolean updatePower(String nodeId, int upPowerDecrement, int downPowerDecrement, int spfDecrement) {
    try {
      // Special usage: sets device to full power
      if (upPowerDecrement == 0 && downPowerDecrement == 0 && spfDecrement == 0) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET downstream_power = ? upstream_power = ?, spf = ? WHERE id = ?");
        preparedStmt.setInt(1, maxPower);
        preparedStmt.setInt(2, maxPower);
        preparedStmt.setInt(3, maxSPF);
        preparedStmt.setString(4, nodeId);
        preparedStmt.executeUpdate();
        System.out.println("Node power and SPF reset");
        return true;
      }

      JSONObject originalValue = new JSONObject(this.readNode(nodeId));
      int newPower = originalValue.getInt("upstream_power") - upPowerDecrement;
      int newSpf = originalValue.getInt("spf") - spfDecrement;

      if (newPower > 4 && newSpf > 6) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET upstream_power = ? spf = ? WHERE id = ?");
        preparedStmt.setInt(1, newPower);
        preparedStmt.setInt(2, newSpf);
        preparedStmt.setString(3, nodeId);
        preparedStmt.executeUpdate();
        System.out.println("Node power updated to " + newPower + " and SPF to " + newSpf);
        return true;
      }
      System.out.println("No need to update power and SPF");
      return false;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * UPDATE: Updates node power settings in DB
   * @param nodeId
   * @param upPowerIncrement
   * @param downPowerIncrement
   * @param spfIncrement
   * @return boolean
   */
  public boolean increasePower(String nodeId, int upPowerIncrement, int downPowerIncrement, int spfIncrement) {
    try {
      JSONObject originalvalue = new JSONObject(this.readNode(nodeId));
      int newPower = originalvalue.getInt("upstream_power") + upPowerIncrement;
      int newSpf = originalvalue.getInt("spf") + spfIncrement;

      if (newPower <= maxPower && newSpf <= maxSPF) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET upstream_power = ?, spf = ? WHERE id = ?");
        preparedStmt.setInt(1, newPower);
        preparedStmt.setInt(2, newSpf);
        preparedStmt.setString(3, nodeId);
        preparedStmt.executeUpdate();
        System.out.println("Node power updated to " + newPower + " and SPF to " + newSpf);
        return true;
      }

      return false;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Updates downlink buffer message, marked as send
   * @param msgId
   * @param dutyCRemaining
   */
  public void markDownlinkAsSent(int msgId, int dutyCRemaining) {
    try {
      // Version 1.0 does not support downstream ACK edit here
      preparedStmt = conn.prepareStatement("UPDATE downlink_messages SET sent = TRUE, duty_cycle_remaining = ?, delivered = TRUE, send_time = ? WHERE id = ?");
      preparedStmt.setInt(1, dutyCRemaining);
      preparedStmt.setTimestamp(2, DateManager.getTimestamp());
      preparedStmt.setInt(3, msgId);
      preparedStmt.executeUpdate();
      System.out.println("MSG " + msgId + " marked as send");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * QUERY: Returns transmission parameters for AP of selected ID
   * @param transmissionParamId
   * @return String
   */
  public String readTransmissionParams(int transmissionParamId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT row_to_json(t) FROM (select * from transmission_params tpars WHERE tpars.id = ?) t");
      preparedStmt.setInt(1, transmissionParamId);
      rs = preparedStmt.executeQuery();
      rs.next();
      return rs.getString("row_to_json");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * QUERY: Returns node of selected ID
   * @param id
   * @return String
   */
  public String readNode(String id) {
    try {
      preparedStmt = conn.prepareStatement("SELECT row_to_json(t) FROM (select * from nodes WHERE id = ?) t");
      preparedStmt.setString(1, id);
      rs = preparedStmt.executeQuery();
      return !rs.next() ? null : rs.getString("row_to_json");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * QUERY: Finds if DB has any awaiting downlink messages
   * @param nodeId
   * @return String
   */
  public String readDownlinkMsg(String nodeId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT row_to_json(t) FROM (select * from downlink_messages ms WHERE ms.node_id = ? AND ms.sent = FALSE) t");
      preparedStmt.setString(1, nodeId);
      rs = preparedStmt.executeQuery();
      return !rs.next() ? "{}" : rs.getString("row_to_json");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "{}";
  }

  /**
   * QUERY: Finds if DB has any avaiting downlink messages
   * @param nodeId
   * @return String
   */
  public String readUplinkMsg(String nodeId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT row_to_json(t) FROM (select * from uplink_messages ms WHERE ms.node_id = ? AND ms.is_primary = TRUE) t");
      preparedStmt.setString(1, nodeId);
      rs = preparedStmt.executeQuery();
      return !rs.next() ? "{}" : rs.getString("row_to_json");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "{}";
  }

  /**
   * QUERY: Gets last N msgs for prosessing, N defined in config
   * @param nodeId
   * @return String
   */
  public String readLastNMessages(String nodeId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT array_to_json(array_agg(t)) FROM (SELECT * FROM uplink_messages ms WHERE ms.node_id = ? AND ms.is_primary = TRUE ORDER BY ms.id DESC LIMIT ?) t");
      preparedStmt.setString(1, nodeId);
      preparedStmt.setInt(2, rssiHarmonizationLimit);
      rs = preparedStmt.executeQuery();
      return !rs.next() ? "[]" : rs.getString("array_to_json");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "[]";
  }
}
