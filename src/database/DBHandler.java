package database;

import core.DateManager;
import core.ProgramResources;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Handles database connection, read and write operations
 * @author Karol Cagáň
 * @author Alexander Valach
 * @version 0.3
 */
public class DBHandler {
  private final String JDBC_DRIVER;
  private final String DB_URL;

  // DB credentials
  private final String USER;
  private final String PASS;

  private Connection conn = null;
  private PreparedStatement preparedStmt = null;
  private ResultSet rs = null;
  private final int maxPower;
  private final int maxSPF;
  private final int rssiHarmonizationLimit;
  private final DateFormat dateFormat;

  /**
   * Class initialization
   * @param programResources instance of program resources
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
      Statement stmt = conn.createStatement();
    } catch (PSQLException e) {
      System.out.println("Connection refused by database server! Is it functional and running?");
      System.out.println("LoRa Network Server will now exit");
      System.exit(1);
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
  public void writeAp(String id, String protocolVersion, int maxPower, int channelsNum, Time dutyCycleRefresh, String loraProtocol, String loraProtocolVer, int transmissionParamId) {
    if (this.accessPointExists(id)) {
      System.out.println("AP with ID=" + id + " already exists");
      return;
    }

    try {
      preparedStmt = conn.prepareStatement("INSERT INTO aps (id, protocol_ver, max_power, channels_num, duty_cycle_refresh, lora_protocol, lora_protocol_ver, transmission_param_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      preparedStmt.setString(1, id);
      preparedStmt.setString(2, protocolVersion);
      preparedStmt.setInt(3, maxPower);
      preparedStmt.setInt(4, channelsNum);
      preparedStmt.setTime(5, dutyCycleRefresh);
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
   * @param id node id
   * @param lastSeq last sequence number
   * @param dhKey diffie-hellman key
   */
  public void writeKey(String id, int lastSeq, String dhKey) {
    try {
      if (!dhKey.equals("")) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET dh_key = ?, last_seq = ? WHERE id = ?");
        preparedStmt.setString(1, dhKey);
        preparedStmt.setInt(2, lastSeq);
        preparedStmt.setString(3, id);
        preparedStmt.executeUpdate();
        System.out.println("DH Key: " + dhKey);
        System.out.println("New KEY written into database for node ID: " + id);
      } else {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET last_seq = ? WHERE id = ?");
        preparedStmt.setInt(1, lastSeq);
        preparedStmt.setString(2, id);
        preparedStmt.executeUpdate();
        System.out.println("Seq updated to " + lastSeq + " for node " + id);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /***
   * Updates sequence number of single node
   * @param id end node id
   * @param lastSeq new sequence number value
   */
  public void updateSequence(String id, int lastSeq) {
    try {
      preparedStmt = conn.prepareStatement("UPDATE nodes SET last_seq = ? WHERE id = ?");
      preparedStmt.setInt(1, lastSeq);
      preparedStmt.setString(2, id);
      preparedStmt.executeUpdate();
      System.out.println("SEQ updated to " + lastSeq + " for node " + id);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /***
   * Writes new uplink message into DB
   * @param appData
   * @param snr
   * @param rssi
   * @param dutyCRemaining
   * @param isPrimary
   * @param receiveTime
   * @param msgGroupNumber
   * @param seqNum
   * @param frequency
   * @param spf
   * @param power
   * @param airtime
   * @param coderate
   * @param bandwidth
   * @param messageTypeId
   * @param apId
   * @param nodeId
   */
  public void writeUplinkMsg(String appData, float snr, float rssi, int dutyCRemaining, boolean isPrimary,
                             Timestamp receiveTime, int msgGroupNumber, int seqNum, float frequency, int spf,
                             int power, int airtime, String coderate, int bandwidth, int messageTypeId,
                             String apId, String nodeId) {
    try {
      preparedStmt = conn.prepareStatement("INSERT INTO uplink_messages " +
              "(app_data, snr, rssi, duty_cycle_remaining, is_primary, receive_time, " +
              "msg_group_number, seq, frequency, spf, power, airtime, coderate, bandwidth, " +
              "message_type_id, ap_id, node_id) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      preparedStmt.setString(1, appData);
      preparedStmt.setFloat(2, snr);
      preparedStmt.setFloat(3, rssi);
      preparedStmt.setInt(4, dutyCRemaining);
      preparedStmt.setBoolean(5, isPrimary);
      preparedStmt.setTimestamp(6, receiveTime);
      preparedStmt.setInt(7, msgGroupNumber);
      preparedStmt.setInt(8, seqNum);
      preparedStmt.setFloat(9, frequency);
      preparedStmt.setFloat(10, spf);
      preparedStmt.setInt(11, power);
      preparedStmt.setInt(12, airtime);
      preparedStmt.setString(13, coderate);
      preparedStmt.setInt(14, bandwidth);
      preparedStmt.setInt(15, messageTypeId);
      preparedStmt.setString(16, apId);
      preparedStmt.setString(17, nodeId);
      preparedStmt.executeUpdate();
      System.out.println("New UPLINK MSG from " + nodeId + " written into database for AP " + apId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /***
   * Saves all messages into DB
   * @param currentGrape batch with all message replicas
   * @param primary single message marked as primary
   * @param msgGroupId message group id
   * @param msgTypeId message type id
   * @throws JSONException
   */
  public void bulkInsertUplinkMessages (ArrayList<JSONObject> currentGrape, JSONObject primary, int msgGroupId, int msgTypeId) throws JSONException {
    // TODO: Make bulk insert as a transcation
    for (JSONObject message : currentGrape) {
      this.writeUplinkMsg(
        message.getString("data"),
        Float.parseFloat(message.getString("snr")),
        Float.parseFloat(message.getString("rssi")),
        message.getInt("duty_c"),
        message.toString().equals(primary.toString()),
        DateManager.getTimestamp(),
        msgGroupId,
        message.getInt("seq"),
        Float.parseFloat(message.getString("freq")),
        message.getInt("sf"),
        message.getInt("power"),
        message.getInt("time"),
        message.getString("cr"),
        message.getInt("band"),
        msgTypeId,
        message.getString("hWIdentifier"),
        message.getString("dev_id")
      );
    }
  }

  /**
   * Writes new node into DB
   * @param id hardware identifier
   * @param upPower uplink power
   * @param downPower downlink power
   * @param spf spreading factor value
   * @param formattedDate receive time
   * @param appId application id
   * @param transmissionParam transmission params id
   */
  public void writeNode(String id, int upPower, int downPower, int spf, String formattedDate,
                        int appId, int transmissionParam) {
    if (this.endNodeExists(id)) {
      System.out.println("Node with ID " + id + " already exists");
      return;
    }

    try {
      preparedStmt = conn.prepareStatement("INSERT INTO nodes " +
              "(id, upstream_power, downstream_power, spf, duty_cycle_refresh, application_id, transmission_param_id) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?)");
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
        this.increasePower(id, 0, 0);
      }
    }

  }

  /**
   * Schedule new downlink messages
   * @param appData base64 encoded data
   * @param apId access point hardware identifier
   * @param nodeId end node identifier
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
   * @param appData base64 encoded data
   * @param apId access point hardware identifier
   * @param nodeId end node identifier
   */
  public void writeSentDownlinkMsg(String appData, String netData, int dutyCRemaining, float frequency,
                                   int spf, int power, int airtime, String coderate, int bandwidth,
                                   String apId, String nodeId) {
    try {
      // Version 1.0 does not support downstream ACK edit here
      preparedStmt = conn.prepareStatement("INSERT INTO downlink_messages " +
              "(app_data, duty_cycle_remaining, sent, ack_required, delivered, send_time, " +
              "frequency, spf, power, airtime, coderate, bandwidth, ap_id, node_id, net_data) " +
              "VALUES (?, ?, true, false, true, ? , ?, ?, ?, ?, ?, ?, ? ,? ,?::json)");

      preparedStmt.setString(1, appData);
      preparedStmt.setInt(2, dutyCRemaining);
      preparedStmt.setTimestamp(3, DateManager.getTimestamp());
      preparedStmt.setFloat(4, frequency);
      preparedStmt.setInt(5, spf);
      preparedStmt.setInt(6, power);
      preparedStmt.setInt(7, airtime);
      preparedStmt.setString(8, coderate);
      preparedStmt.setInt(9, bandwidth);
      preparedStmt.setString(10, apId);
      preparedStmt.setString(11, nodeId);
      preparedStmt.setString(12, netData);
      preparedStmt.executeUpdate();
      System.out.println("New Sent MSG_D written into database for Node: " + nodeId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Updates node power settings in DB
   * @param nodeId hardware identifier
   * @param upPowerDecrement uplink power increment
   * @param downPowerDecrement downlink power increment
   * @param spfDecrement sf decrement value
   * @return boolean
   */
  public boolean updatePower(String nodeId, int upPowerDecrement, int downPowerDecrement, int spfDecrement) {
    try {
      // Special usage: sets device to full power
      if (upPowerDecrement == 0 && downPowerDecrement == 0 && spfDecrement == 0) {
        preparedStmt = conn.prepareStatement("UPDATE nodes SET downstream_power = ?, upstream_power = ?, spf = ? WHERE id = ?");
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
        preparedStmt = conn.prepareStatement("UPDATE nodes SET upstream_power = ?, spf = ? WHERE id = ?");
        preparedStmt.setInt(1, newPower);
        preparedStmt.setInt(2, newSpf);
        preparedStmt.setString(3, nodeId);
        preparedStmt.executeUpdate();
        System.out.println("Node power updated to " + newPower + " and SF to " + newSpf);
        return true;
      }
      return false;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * UPDATE: Updates node power settings in DB
   * @param nodeId end node indentifier
   * @param upPowerIncrement  uplink power value increment
   * @param spfIncrement sf value increment
   * @return boolean
   */
  public boolean increasePower(String nodeId, int upPowerIncrement, int spfIncrement) {
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
   * @param msgId downlink_message id from database
   * @param dutyCRemaining remaining duty cycle value
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
   * @param transmissionParamId transmission param id
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
   * @param id hardware identifier of node
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
   * @param nodeId hardware identifier of node
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
   * QUERY: Finds if DB has any awaiting downlink messages
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
   * QUERY: Gets last N messages for processing, N defined in config
   * @param nodeId end node identifier
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
      return "[]";
    }
  }

  /**
   * Checks whether access point exists
   * @param id access point hardware identifier
   * @return Boolean
   */
  public Boolean accessPointExists (String id) {
    try {
      preparedStmt = conn.prepareStatement("SELECT id FROM aps WHERE id = ?");
      preparedStmt.setString(1, id);
      ResultSet result = preparedStmt.executeQuery();
      return result.next();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Checks whether node exists
   * @param id end node hardware identifier
   * @return Boolean
   */
  public Boolean endNodeExists (String id) {
    try {
      preparedStmt = conn.prepareStatement("SELECT id FROM nodes WHERE id = ?");
      preparedStmt.setString(1, id);
      ResultSet result = preparedStmt.executeQuery();
      return result.next();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Checks whether node exists
   * @param message_name message type name
   * @return int
   */
  public int readMessageType (String message_name) {
    try {
      preparedStmt = conn.prepareStatement("SELECT id FROM message_types WHERE name = ?");
      preparedStmt.setString(1, message_name);
      ResultSet result = preparedStmt.executeQuery();
      return !result.next() ? 1 : result.getInt("id");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return 1;
  }

  /**
   * Read statistical modal from DB
   * @param devId hardware node identifier
   * @return json array
   */
  public String readEnStatModel (String devId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT stat_model FROM nodes WHERE id = ?");
      preparedStmt.setString(1, devId);
      ResultSet result = preparedStmt.executeQuery();
      return !result.next() ? "[]" : result.getString("stat_model");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "[]";
  }

  /**
   * Read statistical modal from DB
   * @param apId access point identifier
   * @return json array
   */
  public String readApStatModel (String apId) {
    try {
      preparedStmt = conn.prepareStatement("SELECT stat_model FROM aps WHERE id = ?");
      preparedStmt.setString(1, apId);
      ResultSet result = preparedStmt.executeQuery();
      return !result.next() ?  "[]" : result.getString("stat_model");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "[]";
  }

  public void updateEnStatModel(String devId, String statModel) {
    try {
      preparedStmt = conn.prepareStatement("UPDATE nodes SET stat_model = ?::json WHERE id = ?");
      preparedStmt.setString(1, statModel);
      preparedStmt.setString(2, devId);
      preparedStmt.executeUpdate();
      System.out.println("Updated statistical model for node " + devId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void updateApStatModel(String hwId, String statModel) {
    try {
      preparedStmt = conn.prepareStatement("UPDATE aps SET stat_model = ?::json WHERE id = ?");
      preparedStmt.setString(1, statModel);
      preparedStmt.setString(2, hwId);
      preparedStmt.executeUpdate();
      System.out.println("Updated statistical model for ap " + hwId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
