package processor;

import core.ProgramResources;
import core.Props;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Node processing logic
 * @author Alexander Valach
 * @version 0.3
 */
public abstract class NodeProcessor {
  protected ProgramResources programResources;
  protected int maxDutyMillis;
  protected int snrSensitivity;
  protected int dutyCycleSensitivity;
  protected int dutyCycleRestriction;
  protected int sensitivityPoison;
  protected int restrictionPoison;
  protected int maxPower;
  protected int edTransmissionParamId;
  protected int downSfSensitivity;
  protected int downPwSensitivity;
  protected int upSfSensitivity;
  protected int upPwSensitivity;
  protected int maxSpf;
  protected int seqTolerance;
  protected boolean isBanditAlgorithm;

  /**
   * Constructor
   * @param programResources collects all required resources
   */
  public NodeProcessor(ProgramResources programResources) {
    this.programResources = programResources;
    Props props = programResources.props;
    this.maxPower = props.getInt("LoRaSettings.maxPower");
    this.edTransmissionParamId = props.getInt("LoRaSettings.edTransmissionParamId");
    this.downSfSensitivity = props.getInt("LoRaSettings.powerDownSpfRssiSensitivityBoundary");
    this.downPwSensitivity = props.getInt("LoRaSettings.powerDownPowerRssiSensitivityBoundary");
    this.upPwSensitivity = props.getInt("LoRaSettings.powerUpPowerRssiSensitivityBoundary");
    this.upSfSensitivity = props.getInt("LoRaSettings.powerUpSpfRssiSensitivityBoundary");
    this.snrSensitivity = props.getInt("LoRaSettings.snrSensitivityBoundary");
    this.maxSpf = props.getInt("LoRaSettings.maxSpf");
    this.seqTolerance = props.getInt("LoRaSettings.seqTolerance");
    this.maxDutyMillis = 3600000 / props.getInt("LoRaSettings.dutyCyclePercent");
    this.dutyCycleRestriction = props.getInt("LoRaSettings.dutyCycleRestrictionBoundary");
    this.dutyCycleSensitivity = props.getInt("LoRaSettings.dutyCycleSensitivityBoundary");
    this.sensitivityPoison = props.getInt("LoRaSettings.dutyCycleSensitivityPoisonRssiValue");
    this.restrictionPoison = props.getInt("LoRaSettings.dutyCycleRestrictionPoisonRssiValue");
    String algorithm = props.getStr("ServerSetting.algorithm");
    this.isBanditAlgorithm = algorithm.equals("ucb") || algorithm.equals("ts");
  }


  /**
   * Calculates metric for selected downlink messages
   * @param rssi value of rssi
   * @param dutyCycleRemaining remaining duty cycle value
   * @return float
   */
  protected float getMetric(int rssi, int dutyCycleRemaining) {
    float dutyCyclePercent = Math.round((dutyCycleRemaining * 100.0) / maxDutyMillis);

    // If more dutyC remaining than sensitivity boundary does not poison RSSI
    if (dutyCyclePercent > dutyCycleSensitivity) {
      return rssi;
    }

    // If less than critical dutyC remaining on a gateway poisons the RSSI by greater value
    if (dutyCyclePercent < dutyCycleRestriction) {
      // If really low duty cycle severely poisons the route
      if (dutyCyclePercent < 5) {
        System.out.println("**** Total poison");
        return rssi - 1000;
      }
      // Else greater poison
      System.out.println("**** High poison");
      return rssi - restrictionPoison;
    }

    // If dutyC is between sensitivity and critical boundary poisons the RSSI by medium value
    System.out.println("**** Medium poison");
    return rssi - sensitivityPoison;
  }

  /***
   * Determines primary message from list of messages and returns it
   * @param currentGrape list containing copies of one messages from different APs
   * @return JSONObject primary message
   * @throws JSONException
   */
  protected JSONObject getPrimaryMessage (ArrayList<JSONObject> currentGrape) throws JSONException {
    JSONObject primary = null;

    for (JSONObject message : currentGrape) {
      if (primary == null) {
        primary = message;
      } else {
        // Determine the best downlink candidate
        if (getMetric(primary.getInt("rssi"), primary.getInt("duty_c")) < getMetric(message.getInt("rssi"), message.getInt("duty_c"))) {
          primary = message;
        }
      }
    }
    return primary;
  }

  protected JSONObject apBanditConfiguration (int apId) {
    return new JSONObject();
  }

  protected JSONObject enBanditConfiguration (String devId) {
    return new JSONObject();
  }

}
