package de.wortmonster.jbdtrigger;
import java.util.*;
public class BmsExtrasTest {
    public static void main(String[] args){
        byte[] basic=new byte[27];put(basic,0,5055);put(basic,2,65536-120);put(basic,4,1910);put(basic,6,2600);put(basic,8,1);put(basic,10,((2026-2000)<<9)|(1<<5)|26);put(basic,12,0x1001);put(basic,16,1<<6);basic[18]=(byte)0xa3;basic[19]=73;basic[20]=3;basic[21]=13;basic[22]=2;put(basic,23,3013);put(basic,25,2961);
        BmsExtras e=new BmsExtras();if(!e.accept(frame(3,basic),100))throw new AssertionError("Basic acceptance");
        eq(e,"bms_capacity_ah","26,00 Ah");eq(e,"bms_remaining_ah","19,10 Ah");eq(e,"bms_firmware","a.3");eq(e,"bms_date","2026-01-26");eq(e,"bms_cycles","1");eq(e,"bms_balancing_cells","1, 13");eq(e,"bms_protection","Entladen: zu warm");
        byte[] cells=new byte[26];Arrays.fill(cells,(byte)0);for(int i=0;i<13;i++)put(cells,i*2,3887);put(cells,2,3886);put(cells,22,3891);
        if(!e.accept(frame(4,cells),200))throw new AssertionError("Cells");eq(e,"bms_cell_delta","5 mV");eq(e,"bms_cell_min_number","2");eq(e,"bms_cell_max_number","12");eq(e,"bms_cell_13","3,887 V");
        if(e.accept(frame(4,new byte[24]),201)||e.accept(frame(4,new byte[3]),202))throw new AssertionError("Invalid cell count/length");
        String prior=e.values.toString();byte[] bad=frame(4,cells);bad[5]++;if(e.accept(bad,300)||!prior.equals(e.values.toString()))throw new AssertionError("Bad checksum mutates readings");
        byte[] events=new byte[24];put(events,0,2);if(!e.accept(frame(0xaa,events),400))throw new AssertionError("Events");eq(e,"bms_event_0","2");eq(e,"bms_event_10","0");if(e.accept(frame(0xaa,new byte[22]),500))throw new AssertionError("Malformed events");
        if(!e.accept(frame(5,"SP14S004P13S30A".getBytes(java.nio.charset.StandardCharsets.US_ASCII)),600))throw new AssertionError("Identity");eq(e,"bms_model","SP14S004P13S30A");
        byte[] many=Arrays.copyOf(basic,23+33*2);many[22]=33;for(int i=0;i<33;i++)put(many,23+i*2,2961);if(!e.accept(frame(3,many),650)||e.values.containsKey("bms_temp_33"))throw new AssertionError("Sensor catalogue bounds");
        for(String key:e.values.keySet())if(!BmsExtras.known(key)||e.times.get(key)==null)throw new AssertionError("Capability catalogue: "+key);
        if(BmsExtras.known("bms_cell_0")||BmsExtras.known("bms_cell_33")||BmsExtras.known("bms_event_11"))throw new AssertionError("Unknown keys");
        if(!Arrays.equals(BmsExtras.readCommand(3),new byte[]{(byte)0xdd,(byte)0xa5,3,0,(byte)0xff,(byte)0xfd,0x77}))throw new AssertionError("Basic-info read command");
        byte[] command=BmsExtras.readCommand(0xaa);if((command[4]&255)!=0xff||(command[5]&255)!=0x56)throw new AssertionError("Read checksum");
        System.out.println("BMS extras: capacities, firmware, cells, balancing, alarms, counters and malformed frames: OK");
    }
    private static void eq(BmsExtras e,String key,String expected){if(!expected.equals(e.values.get(key)))throw new AssertionError(key+": "+e.values.get(key));}
    private static void put(byte[] b,int i,int n){b[i]=(byte)(n>>8);b[i+1]=(byte)n;}
    private static byte[] frame(int cmd,byte[] data){byte[] b=new byte[data.length+7];b[0]=(byte)0xdd;b[1]=(byte)cmd;b[3]=(byte)data.length;System.arraycopy(data,0,b,4,data.length);int sum=data.length;for(byte v:data)sum+=v&255;put(b,b.length-3,(-sum)&65535);b[b.length-1]=0x77;return b;}
}
