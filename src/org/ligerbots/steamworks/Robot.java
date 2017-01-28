
package org.ligerbots.steamworks;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.ligerbots.steamworks.commands.DriveJoystickCommand;
import org.ligerbots.steamworks.subsystems.DriveTrain;
import org.ligerbots.steamworks.subsystems.Feeder;
import org.ligerbots.steamworks.subsystems.GearManipulator;
import org.ligerbots.steamworks.subsystems.Shooter;
import org.ligerbots.steamworks.subsystems.Vision;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the IterativeRobot documentation. If you change the name of this class
 * or the package after creating this project, you must also update the manifest file in the
 * resource directory.
 */
public class Robot extends IterativeRobot {

  public static final DriveTrain driveTrain = null;//new DriveTrain();
  public static final Vision vision = null;//new Vision();
  public static Shooter shooter = null;
  public static final Feeder feeder = null;//new Feeder();
  public static final GearManipulator gearManipulator = null;//new GearManipulator();

//  public static final DriveJoystickCommand driveJoystickCommand = new DriveJoystickCommand();
  public static OperatorInterface operatorInterface;
  Command autonomousCommand;
  SendableChooser<Command> chooser = new SendableChooser<>();
  
  XboxController controller;

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    shooter = new Shooter();
    operatorInterface = new OperatorInterface();
    // chooser.addDefault("Default Auto", new ExampleCommand());
    // chooser.addObject("My Auto", new MyAutoCommand());
    SmartDashboard.putData("Auto mode", chooser);
    
    SmartDashboard.putNumber("rpm", 4000);
    
    controller = new XboxController(0);
  }

  public void commonPeriodic() {
    Scheduler.getInstance().run();
  }

  /**
   * This function is called once each time the robot enters Disabled mode. You can use it to reset
   * any subsystem information you want to clear when the robot is disabled.
   */
  @Override
  public void disabledInit() {

  }

  @Override
  public void disabledPeriodic() {
    commonPeriodic();
    
    System.out.println(SmartDashboard.getNumber("rpm", 0));
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select between different
   * autonomous modes using the dashboard. The sendable chooser code works with the Java
   * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
   * uncomment the getString code to get the auto name from the text box below the Gyro You can add
   * additional auto modes by adding additional commands to the chooser code above (like the
   * commented example) or additional comparisons to the switch structure below with additional
   * strings & commands.
   */
  @Override
  public void autonomousInit() {
    autonomousCommand = chooser.getSelected();

    /*
     * String autoSelected = SmartDashboard.getString("Auto Selector", "Default");
     * switch(autoSelected) { case "My Auto": autonomousCommand = new MyAutoCommand(); break; case
     * "Default Auto": default: autonomousCommand = new ExampleCommand(); break; }
     */

    // schedule the autonomous command (example)
    if (autonomousCommand != null) {
      autonomousCommand.start();
    }
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    commonPeriodic();
  }

  @Override
  public void teleopInit() {
    // This makes sure that the autonomous stops running when
    // teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove
    // this line or comment it out.
    if (autonomousCommand != null) {
      autonomousCommand.cancel();
    }

//    driveJoystickCommand.start();
    
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {
    commonPeriodic();
    
    if(controller.getXButton()) {
      shooter.setShooterRpm(-SmartDashboard.getNumber("rpm", 0));
    } else if(controller.getStickButton(Hand.kLeft)) {
      shooter.setShooterVoltage(12 * controller.getY(Hand.kLeft));
    } else {
      shooter.setShooterRpm(0);
    }
    shooter.fixHacks();
    
    shooter.reportSmartDashboard();
  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
    LiveWindow.run();
    
    shooter.reportSmartDashboard();
    
    shooter.fixHacks();
    
    if(controller.getXButton()) {
      shooter.setShooterRpm(-2000);
    } else {
      shooter.setShooterRpm(0);
    }
    System.out.println(shooter.getShooterRpm());
  }
}
