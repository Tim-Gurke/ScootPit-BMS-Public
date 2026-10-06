package de.wortmonster.jbdtrigger;
import java.util.Arrays;
public class JbdControlTest {
    public static void main(String[] args){
        byte[] expected={(byte)0xdd,0x5a,(byte)0xe1,2,0,2,(byte)0xff,0x1b,0x77};
        if(!Arrays.equals(expected,JbdControl.discharge(false)))throw new AssertionError("Manufacturer discharge-off example");
        byte[] on=JbdControl.discharge(true);
        if(on[5]!=0 || (on[7]&255)!=0x1d)throw new AssertionError("Release discharge software lock");
        byte[] ack={(byte)0xdd,(byte)0xe1,0,0,0,0,0x77};
        if(!JbdControl.validReply(ack))throw new AssertionError("ACK");
        ack[2]=(byte)0x80;ack[4]=(byte)0xff;ack[5]=(byte)0x80;
        if(!JbdControl.validReply(ack))throw new AssertionError("Error reply");
        ack[4]=0;if(JbdControl.validReply(ack))throw new AssertionError("Corrupt ACK");
        reject(false,false,true,false,0,6000,0);
        reject(true,true,true,false,0,6000,0);
        reject(true,false,false,true,0,6000,0);
        reject(true,false,true,false,-0.2,6000,0);
        reject(true,false,true,false,0,4999,0);
        reject(true,false,true,true,0,6000,4);
        reject(true,false,true,false,Double.NaN,6000,0);
        if(JbdControl.rejection(true,false,true,false,0,5000,0)!=null)throw new AssertionError("Quiet stationary release");
        System.out.println("JBD command, ACK checksum, stale/ride/current/charging/movement safeguards: OK");
    }
    private static void reject(boolean fresh,boolean trip,boolean charge,boolean enabled,double current,long quiet,double speed){
        if(JbdControl.rejection(fresh,trip,charge,enabled,current,quiet,speed)==null)throw new AssertionError("Unsafe command accepted");
    }
}
