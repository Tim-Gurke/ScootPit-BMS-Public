package de.wortmonster.jbdtrigger;
public class ConnectionPolicyTest {
 public static void main(String[] args){
  ConnectionPolicy.InitialSignal signal=new ConnectionPolicy.InitialSignal();
  if(signal.accept(1000,-80,-70,3000))throw new AssertionError("Distant scooter connected");
  if(signal.accept(2000,-55,-70,3000)||signal.accept(3000,-60,-70,3000))throw new AssertionError("Single strong hit connected");
  if(signal.accept(4000,-75,-70,3000))throw new AssertionError("Weak hit did not reset confirmation");
  if(signal.accept(5000,-60,-70,3000)||signal.accept(6000,-60,-70,3000)||signal.accept(7000,-60,-70,3000))throw new AssertionError("Confirmation ended too early");
  if(!signal.accept(8000,-60,-70,3000))throw new AssertionError("Stable nearby scooter blocked");
  signal.reset();signal.accept(0,-60,-70,1000);signal.accept(0,-60,-70,1000);
  if(signal.accept(1000,-60,-70,1000))throw new AssertionError("Duplicate callbacks counted as stable reception");
  signal.reset();signal.accept(0,-60,-70,3000);if(signal.accept(10000,-60,-70,3000))throw new AssertionError("Stale samples accepted");
  signal.reset();boolean confirmed=false;for(int time=0;time<=3000;time+=100)confirmed=signal.accept(time,-60,-70,3000);
  if(!confirmed)throw new AssertionError("Frequent advertisements prevented confirmation");
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
