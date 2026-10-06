package de.wortmonster.jbdtrigger;

/** JBD General Protocol V4, E1. Never infer a charging software lock from FET-off. */
final class JbdControl {
    static byte[] discharge(boolean enabled) {
        int value=enabled?0:2, checksum=-(0xe1+2+value)&65535;
        return new byte[]{(byte)0xdd,0x5a,(byte)0xe1,2,0,(byte)value,
            (byte)(checksum>>8),(byte)checksum,0x77};
    }
    static boolean validReply(byte[] b) {
        if(b==null || b.length<7 || (b[0]&255)!=0xdd || b.length!=(b[3]&255)+7 || b[b.length-1]!=0x77)return false;
        int sum=0;for(int i=2;i<b.length-3;i++)sum+=b[i]&255;
        return ((-sum)&65535)==((b[b.length-3]&255)<<8 | (b[b.length-2]&255));
    }
    static String rejection(boolean fresh, boolean trip, boolean chargeOn, boolean enabled,
                            double current, long quietMs, double speed) {
        if(!fresh)return "Frische BMS-Verbindung erforderlich";
        if(!chargeOn)return "Lade-MOS ist aus: Schalten gesperrt, damit der Ladezustand unverändert bleibt";
        if(trip)return "Zuerst die laufende Fahrt beenden";
        if(speed>=2)return "Nur bei stehendem Roller schalten";
        if(!enabled && (!Double.isFinite(current) || Math.abs(current)>=0.15 || quietMs<5000))return "Roller ausschalten und mindestens 5 Sekunden ohne Last warten";
        return null;
    }
}
