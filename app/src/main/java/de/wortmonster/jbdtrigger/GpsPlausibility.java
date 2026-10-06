package de.wortmonster.jbdtrigger;

/** Reject GPS spikes before they reach display, counters or recording. */
final class GpsPlausibility {
    static boolean accept(double speed,boolean hasSpeed,double distance,double seconds,
            double accuracy,double previousAccuracy,double previousSpeed,double maximum){
        if(!Double.isFinite(speed)||speed<0||speed>maximum||!Double.isFinite(distance)||distance<0)return false;
        if(seconds<=0)return distance==0;
        double uncertainty=Math.min(35,accuracy)+Math.min(35,previousAccuracy);
        if(distance>maximum/3.6*seconds+uncertainty)return false;
        // With poor fixes don't trust a sudden reported jump; retain the last accepted reading.
        if(hasSpeed && seconds<=5 && speed-previousSpeed>Math.max(12,seconds*14.4))return false;
        return hasSpeed || distance/seconds*3.6<=maximum;
    }
}
