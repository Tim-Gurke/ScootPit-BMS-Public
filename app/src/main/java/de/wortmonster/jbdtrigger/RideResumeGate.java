package de.wortmonster.jbdtrigger;

/** A paused ride resumes only after sustained motor load and reliable riding speed return. */
final class RideResumeGate {
    static final double MIN_DISCHARGE_AMPS=1.0;
    static final double MIN_SPEED_KMH=7.0;
    static final long CONFIRM_MS=2500;
    private long candidateSince=-1,lastSample=-1;

    void reset(){candidateSince=lastSample=-1;}

    boolean update(long now,double dischargeAmps,double configuredMinimum,double speedKmh,boolean gpsFresh){
        double currentThreshold=Math.max(MIN_DISCHARGE_AMPS,configuredMinimum);
        if(!gpsFresh||!Double.isFinite(speedKmh)||speedKmh<MIN_SPEED_KMH||!Double.isFinite(dischargeAmps)||dischargeAmps<currentThreshold){reset();return false;}
        if(candidateSince<0||lastSample<0||now-lastSample>2500||now<lastSample)candidateSince=now;
        lastSample=now;
        return now-candidateSince>=CONFIRM_MS;
    }
}
