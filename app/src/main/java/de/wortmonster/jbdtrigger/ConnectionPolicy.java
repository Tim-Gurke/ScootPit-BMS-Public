package de.wortmonster.jbdtrigger;
final class ConnectionPolicy {
    /** Require consecutive, fresh strong advertisements before the first connection. */
    static final class InitialSignal {
        private long first = -1, last = -1, counted = -1;
        private int samples;
        void reset() { first = last = counted = -1; samples = 0; }
        boolean accept(long now, int rssi, int threshold, long confirmationMs) {
            if (rssi < threshold) { reset(); return false; }
            if (last < 0 || now < last || now - last > 2500) {
                first = now; counted = -1; samples = 0;
            }
            // Duplicate callbacks in the same instant must not count as stable reception.
            if (counted < 0 || now - counted >= 250) { samples++; counted = now; }
            last = now;
            return samples >= 3 && now - first >= confirmationMs;
        }
    }
    static boolean departed(long now,long missingSince,long grace,int bestRssi,int departureRssi){
        return missingSince>0 && now-missingSince>=grace && bestRssi<departureRssi;
    }
    static boolean recordMovement(boolean active,long now,long packetAt,long idleSince){
        return active && packetAt>0 && now-packetAt<=8000 && (idleSince==0||now-idleSince<3000);
    }
}
