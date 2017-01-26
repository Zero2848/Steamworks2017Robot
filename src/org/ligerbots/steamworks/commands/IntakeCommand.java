package org.ligerbots.steamworks.commands;

import edu.wpi.first.wpilibj.command.Command;
import org.ligerbots.steamworks.Robot;

/**
 *
 */
public class IntakeCommand extends Command {

  boolean isOn;

  public IntakeCommand(boolean isOn) {
    requires(Robot.intake);
    this.isOn = isOn;
  }

  // Called just before this Command runs the first time
  protected void initialize() {}

  // Called repeatedly when this Command is scheduled to run
  protected void execute() {
    if (isOn) {
      Robot.intake.intakeOn();
    } else {
      Robot.intake.intakeOff();
    }
  }

  // Make this return true when this Command no longer needs to run execute()
  protected boolean isFinished() {
    return true;
  }

  // Called once after isFinished returns true
  protected void end() {}

  // Called when another command which requires one or more of the same
  // subsystems is scheduled to run
  protected void interrupted() {}
}