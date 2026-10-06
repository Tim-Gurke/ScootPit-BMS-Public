package de.wortmonster.jbdtrigger;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Read-only standard JBD 03/04/05/AA responses, independent of Android. */
final class BmsExtras {
    static final String[] ERROR_NAMES={"Zellüberspannung","Zellunterspannung","Akkuüberspannung","Akkuunterspannung","Laden: zu warm","Laden: zu kalt","Entladen: zu warm","Entladen: zu kalt","Ladeüberstrom","Entladeüberstrom","Kurzschluss","Messchipfehler","MOS-Softwaresperre"};
    static final String[] COUNT_NAMES={"Kurzschlüsse","Ladeüberstrom","Entladeüberstrom","Zellüberspannung","Zellunterspannung","Laden: Übertemperatur","Laden: Untertemperatur","Entladen: Übertemperatur","Entladen: Untertemperatur","Akkuüberspannung","Akkuunterspannung"};
    final Map<String,String> values=new LinkedHashMap<>();
    final Map<String,Long> times=new HashMap<>();
    int cells;
    private void put(String key,String value,long at){values.put(key,value);times.put(key,at);}
    boolean accept(byte[] frame,long at){
        if(!JbdControl.validReply(frame)||frame[2]!=0)return false;int cmd=frame[1]&255,n=frame[3]&255;
        if(cmd==3){BmsPacket p=BmsPacket.decode(frame);if(p==null)return false;
            put("bms_remaining_ah",fmt(p.remainingAh,"%.2f Ah"),at);if(p.fullAh>0)put("bms_capacity_ah",fmt(p.fullAh,"%.2f Ah"),at);
            put("bms_cycles",Integer.toString(u16(frame,12)),at);
            int date=u16(frame,14);try{put("bms_date",LocalDate.of(2000+(date>>9),(date>>5)&15,date&31).toString(),at);}catch(Exception ignored){}
            int fw=frame[22]&255;put("bms_firmware",Integer.toHexString(fw>>4)+"."+Integer.toHexString(fw&15),at);
            cells=frame[25]&255;if(cells>0&&cells<=32)put("bms_cells",Integer.toString(cells),at);
            long balance=u16(frame,16)|((long)u16(frame,18)<<16);StringJoiner balanced=new StringJoiner(", ");for(int i=0;i<Math.min(cells,32);i++)if((balance&(1L<<i))!=0)balanced.add(Integer.toString(i+1));
            put("bms_balancing",balance==0?"Inaktiv":"Aktiv",at);put("bms_balancing_cells",balanced.length()==0?"Keine":balanced.toString(),at);
            int errors=u16(frame,20);StringJoiner alarms=new StringJoiner(" · ");for(int i=0;i<ERROR_NAMES.length;i++)if((errors&(1<<i))!=0)alarms.add(ERROR_NAMES[i]);if((errors&0xe000)!=0)alarms.add(String.format(Locale.US,"Weitere: 0x%04X",errors&0xe000));
            put("bms_protection",alarms.length()==0?"Keine":alarms.toString(),at);
            put("bms_charge_fet",p.chargeEnabled?"Freigegeben":"Gesperrt",at);put("bms_discharge_fet",p.dischargeEnabled?"Freigegeben":"Gesperrt",at);
            for(int i=2;i<Math.min(p.temperatures.length,32);i++)put("bms_temp_"+(i+1),fmt(p.temperatures[i],"%.1f °C"),at);
            return true;
        }
        if(cmd==4){if(n<2||n>64||n%2!=0||(cells>0&&n/2!=cells))return false;
            double min=10,max=-1,sum=0;int imin=0,imax=0;
            for(int i=0;i<n/2;i++){double v=u16(frame,4+i*2)/1000.0;if(v<=0||v>5.5)return false;}
            for(int i=0;i<n/2;i++){double v=u16(frame,4+i*2)/1000.0;put("bms_cell_"+(i+1),fmt(v,"%.3f V"),at);sum+=v;if(v<min){min=v;imin=i+1;}if(v>max){max=v;imax=i+1;}}
            put("bms_cell_min",fmt(min,"%.3f V"),at);put("bms_cell_max",fmt(max,"%.3f V"),at);put("bms_cell_delta",fmt((max-min)*1000,"%.0f mV"),at);put("bms_cell_average",fmt(sum/(n/2),"%.3f V"),at);
            put("bms_cell_min_number",Integer.toString(imin),at);put("bms_cell_max_number",Integer.toString(imax),at);return true;
        }
        if(cmd==5){if(n<1||n>64)return false;String name=new String(frame,4,n,StandardCharsets.US_ASCII).replace("\u0000","").trim();if(name.isEmpty()||!name.matches("[\\x20-\\x7e]+"))return false;put("bms_model",name,at);return true;}
        if(cmd==0xaa){if(n!=24)return false;for(int i=0;i<COUNT_NAMES.length;i++)put("bms_event_"+i,Integer.toString(u16(frame,4+2*i)),at);return true;}
        return false;
    }
    static boolean known(String key){return title(key)!=null;}
    static String title(String key){
        switch(key){case "bms_remaining_ah":return "Restkapazität";case "bms_capacity_ah":return "BMS-Kapazität";case "bms_cycles":return "Batteriezyklen";case "bms_date":return "Herstellungsdatum";case "bms_firmware":return "BMS-Firmware";case "bms_model":return "BMS-Kennung";case "bms_cells":return "Zellgruppen";case "bms_balancing":return "Balancierung";case "bms_balancing_cells":return "Balancierte Zellgruppen";case "bms_protection":return "Schutzmeldungen";case "bms_charge_fet":return "Ladefreigabe";case "bms_discharge_fet":return "Entladefreigabe";case "bms_cell_min":return "Zellspannung Minimum";case "bms_cell_max":return "Zellspannung Maximum";case "bms_cell_delta":return "Zelldifferenz";case "bms_cell_average":return "Zellspannung Mittelwert";case "bms_cell_min_number":return "Niedrigste Zellgruppe";case "bms_cell_max_number":return "Höchste Zellgruppe";}
        try{if(key.startsWith("bms_cell_")){int i=Integer.parseInt(key.substring(9));if(i>=1&&i<=32)return "Zellgruppe "+i;}
            if(key.startsWith("bms_temp_")){int i=Integer.parseInt(key.substring(9));if(i>=3&&i<=32)return "Temp"+i;}
            if(key.startsWith("bms_event_")){int i=Integer.parseInt(key.substring(10));if(i>=0&&i<COUNT_NAMES.length)return "Ereignisse: "+COUNT_NAMES[i];}
        }catch(Exception ignored){}return null;
    }
    static byte[] readCommand(int command){int checksum=(-command)&65535;return new byte[]{(byte)0xdd,(byte)0xa5,(byte)command,0,(byte)(checksum>>8),(byte)checksum,0x77};}
    static long expiry(String key){return key.equals("bms_model")||key.equals("bms_firmware")||key.equals("bms_date")?3600000:key.startsWith("bms_event_")?120000:15000;}
    private static String fmt(double v,String format){return String.format(Locale.GERMANY,format,v);}
    private static int u16(byte[] b,int i){return ((b[i]&255)<<8)|(b[i+1]&255);}
}
