package de.wortmonster.jbdtrigger;
public class ConnectionPolicyTest {
 public static void main(String[] args){
  if(ConnectionPolicy.departed(29000,1000,30000,-127,-95))throw new AssertionError("Early departure");
  if(ConnectionPolicy.departed(40000,1000,30000,-90,-95))throw new AssertionError("Nearby BMS ended trip");
  if(!ConnectionPolicy.departed(40000,1000,30000,-127,-95))throw new AssertionError("Absent BMS not ended");
  if(ConnectionPolicy.recordMovement(false,20000,20000,0))throw new AssertionError("Paused walking recorded");
  if(ConnectionPolicy.recordMovement(true,20000,10000,0))throw new AssertionError("Disconnected walking recorded");
  if(ConnectionPolicy.recordMovement(true,20000,20000,16000))throw new AssertionError("Idle walking recorded");
  if(!ConnectionPolicy.recordMovement(true,20000,20000,0))throw new AssertionError("Resumed riding blocked");
  System.out.println("Pause, walking, fresh telemetry, reconnect grace and departure: OK");
 }
}
