package org.ligerbots.steamworks.subsystems;

import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.networktables.ConnectionInfo;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.networktables.NetworkTablesJNI;
import edu.wpi.first.wpilibj.tables.ITable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import org.ligerbots.steamworks.RobotMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The subsystem that handles communication with the android.
 */
public class Vision extends Subsystem implements SmartDashboardLogger {
  static class VisionData {
    double rvec0;
    double rvec1;
    double rvec2;
    double tvec0;
    double tvec1;
    double tvec2;
  }
  
  private static final int CS_STREAM_PORT = 5810;
  private static final int DATA_PORT = 5808;
  private static final int CS_FEEDBACK_INTERVAL = 1000;
  private static final int CS_MAGIC_NUMBER = 16777216;
  
  Relay ledRing;
  ITable table = null;
  // buffer vision data for multithreaded access
  VisionData[] visionData = { new VisionData(), new VisionData() };
  volatile int currentVisionDataIndex = 0;
  Logger logger = LoggerFactory.getLogger(Vision.class);  
  /**
   * Creates the instance of VisionSubsystem.
   */
  public Vision() {
    logger.trace("Beginning vision");
    ledRing = new Relay(RobotMap.RELAY_LED_RING);

    Thread forwardThread = new Thread(this::packetForwardingThread);
    forwardThread.setDaemon(true);
    forwardThread.setName("Packet Forwarding Thread");
    forwardThread.start();

    Thread dataThread = new Thread(this::dataThread);
    dataThread.setDaemon(true);
    dataThread.setName("Vision Data Thread");
    dataThread.start();
  }

  /**
   * Sends an enable flag to the phone to enable or disable image processing. Helpful for making
   * sure the phone doesn't eat power when it doesn't need to.
   * 
   * @param enabled Whether image processing should be enabled or not
   */
  public void setVisionEnabled(boolean enabled) {
    
    if (table == null) {
      logger.trace("Getting new vision table");
      table = NetworkTable.getTable("Vision");
    }
    logger.trace("Vision enabled: " + enabled);
    table.putBoolean("enabled", enabled);
  }

  /**
   * Turns the LED ring for the retroreflective tape on or off.
   * @param on Whether the LED ring should be on or not.
   */
  public void setLedRingOn(boolean on) {
    logger.trace("Setting LedRingOn to " + on);
    ledRing.set(on ? Relay.Value.kForward : Relay.Value.kReverse);
  }
  
  public VisionData getVisionData() {
    
    return visionData[currentVisionDataIndex];
  }

  public void initDefaultCommand() {}

  /**
   * This method runs in a separate thread and receives data from the phone.
   */
  public void dataThread() {
    DatagramChannel udpChannel = null;
    ByteBuffer dataPacket = ByteBuffer.allocateDirect(Double.SIZE / 8 * 6);

    try {
      udpChannel = DatagramChannel.open();
      udpChannel.socket().setReuseAddress(true);
      udpChannel.socket().bind(new InetSocketAddress(DATA_PORT));
      udpChannel.configureBlocking(true);
      logger.trace("Configuring socket in dataThread");
    } catch (Exception ex) {
      logger.error("Exception during socket configuration in dataThread", ex);
    }

    // the phone sends processing data over UDP faster than NetworkTables
    // 10fps refresh rate, so here we set up a receiver for the data

    while (true) {
      try {
        dataPacket.position(0);
        SocketAddress from = udpChannel.receive(dataPacket);
        if (from == null) {
          continue;
        }

        VisionData notCurrentData = visionData[1 - currentVisionDataIndex];
        notCurrentData.rvec0 = dataPacket.getDouble();
        notCurrentData.rvec1 = dataPacket.getDouble();
        notCurrentData.rvec2 = dataPacket.getDouble();
        notCurrentData.tvec0 = dataPacket.getDouble();
        notCurrentData.tvec1 = dataPacket.getDouble();
        notCurrentData.tvec2 = dataPacket.getDouble();
        currentVisionDataIndex = 1 - currentVisionDataIndex;
        logger.trace("Getting data from socket in dataThread");
        // now do something with the data
      } catch (IOException ex) {
        logger.error("IOException during socket communication in dataThread", ex);
      }
    }
  }

  /**
   * This runs in a separate thread and forwards vision frames from the phone to the DS.
   */
  public void packetForwardingThread() {
    logger.trace("Setting up the packetForwardingThread");
    DatagramChannel udpChannel = null;
    InetSocketAddress sendAddress = null;
    ByteBuffer recvPacket = null;
    byte[] feedbackMessage = "👌".getBytes();
    ByteBuffer feedbackPacket = ByteBuffer.allocateDirect(feedbackMessage.length);
    feedbackPacket.put(feedbackMessage);
    long lastFeedbackTime = System.currentTimeMillis();

    // set up UDP
    try {
      logger.trace("Configuring socket in packetForwardingThread");
      udpChannel = DatagramChannel.open();
      udpChannel.socket().setReuseAddress(true);
      udpChannel.socket().bind(new InetSocketAddress(CS_STREAM_PORT));
      udpChannel.configureBlocking(false);

      recvPacket = ByteBuffer.allocateDirect(udpChannel.socket().getReceiveBufferSize());
    } catch (Exception ex) {
      logger.error("Exception during socket configuration in packetForwardingThread", ex);
    }

    while (true) {
      try {
        logger.trace("Getting driver laptop IP from networktables in packetForwardingThread");
        // steal the driver laptop's IP from networktables
        if (sendAddress == null) {
          ConnectionInfo[] connections = NetworkTablesJNI.getConnections();
          for (ConnectionInfo connInfo : connections) {
            // we want the laptop, not the phone
            if (connInfo.remote_id.equals("Android")) {
              continue;
            }
            sendAddress = new InetSocketAddress(connInfo.remote_ip, CS_STREAM_PORT);
          }
        }
        logger.trace("Getting packet from phone in packetForwardingThread");
        // get a packet from the phone
        SocketAddress from = null;
        recvPacket.limit(recvPacket.capacity());
        recvPacket.position(0);
        from = udpChannel.receive(recvPacket);
        boolean gotPacket = from != null;

        // if we have a packet and it's time to tell the phone we're
        // getting packets then tell the phone we're getting packets
        
        if (from != null && System.currentTimeMillis() - lastFeedbackTime > CS_FEEDBACK_INTERVAL) {
          logger.trace("Dropping all old packets in packetForwardingThread");
          lastFeedbackTime = System.currentTimeMillis();
          feedbackPacket.position(0);
          udpChannel.send(feedbackPacket, from);
        }

        // if sending packets to the driver laptop turns out to be
        // slower than receiving packets from the phone, then drop
        // everything except the latest packet
        while (from != null) {
          // save the length of what we got last time
          recvPacket.limit(recvPacket.capacity());
          recvPacket.position(0);
          from = udpChannel.receive(recvPacket);
        }

        if (sendAddress != null && gotPacket) {
          // make sure to forward a packet of the same length, by
          // setting the limit on the bytebuffer
          recvPacket.position(0);
          int magic = recvPacket.getInt();
          int length = recvPacket.getInt();
          if (magic == CS_MAGIC_NUMBER) {
            recvPacket.limit(length + 8);
            recvPacket.position(0);
            udpChannel.send(recvPacket, sendAddress);
          }
          // otherwise, it's probably a control packet from the
          // dashboard sending the resolution and fps settings - we
          // don't actually care
        }
      } catch (Exception ex) {
        logger.error("Exception forwarding packets during packetForwardingThread");
      }
    }
  }

  @Override
  public void sendDataToSmartDashboard() {
    // TODO Auto-generated method stub
    
  }
}
