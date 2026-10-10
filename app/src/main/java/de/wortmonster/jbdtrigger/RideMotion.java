package de.wortmonster.jbdtrigger;

/** Detects reliable stops and slow, unpowered movement that is likely to be walking. */
final class RideMotion {
    static final double WALKING_MAX_KMH = 7.0;
    private long stillSince=-1,lastFix=-1,walkingSince=-1;

    void reset(){stillSince=lastFix=walkingSince=-1;}

    void fix(long now,double kmh,boolean reliable){fix(now,kmh,reliable,false);}

    void fix(long now,double kmh,boolean reliable,boolean motorPowered){
        if(!reliable||!Double.isFinite(kmh)||kmh<0){stillSince=walkingSince=-1;return;}
        if(lastFix<0||now-lastFix>4000||now<lastFix){stillSince=walkingSince=-1;}
        lastFix=now;
        if(kmh>=1.5)stillSince=-1;
        else if(stillSince<0)stillSince=now;
        if(!motorPowered&&kmh>=1.5&&kmh<=WALKING_MAX_KMH){
            if(walkingSince<0)walkingSince=now;
        }else walkingSince=-1;
    }

    boolean stopped(long now,long duration){return stillSince>=0&&lastFix>=0&&now-lastFix<=4000&&now-stillSince>=duration;}
    boolean walking(long now,long duration){return walkingSince>=0&&lastFix>=0&&now-lastFix<=4000&&now-walkingSince>=duration;}
    static boolean likelyWalking(double kmh,boolean reliable,boolean motorPowered){
        return reliable&&Double.isFinite(kmh)&&kmh>=1.5&&kmh<=WALKING_MAX_KMH&&!motorPowered;
    }
}
