package de.wortmonster.jbdtrigger;
final class ConnectionPolicy {
    static boolean departed(long now,long missingSince,long grace,int bestRssi,int departureRssi){
        return missingSince>0 && now-missingSince>=grace && bestRssi<departureRssi;
    }
    static boolean recordMovement(boolean active,long now,long packetAt,long idleSince){
        return active && packetAt>0 && now-packetAt<=8000 && (idleSince==0||now-idleSince<3000);
    }
}
