package org.ligerbots.steamworks.subsystems;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Pneumatics extends Subsystem {
 
  public enum CompressorState {
    ON, OFF, TOGGLE
  }

  Compressor compressor;
  Logger logger = LoggerFactory.getLogger(Pneumatics.class); 
  public Pneumatics() {
    compressor = new Compressor();
  }

  public void initDefaultCommand() {
    // Set the default command for a subsystem here.
    // setDefaultCommand(new MySpecialCommand());
  }

  public void setCompressor(CompressorState state) {
    if (state == CompressorState.TOGGLE) {
      compressor.setClosedLoopControl(!compressor.getClosedLoopControl());
    } else if (state == CompressorState.ON) {
      compressor.setClosedLoopControl(true);
    } else if (state == CompressorState.OFF) {
      compressor.setClosedLoopControl(false);
    }
    logger.trace("Current compressor state: " + state);
  }

  public boolean isOn() {
    return compressor.getClosedLoopControl();
  }
}
