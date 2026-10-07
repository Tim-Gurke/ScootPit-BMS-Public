package de.wortmonster.jbdtrigger;

/** Only a sequence of reliable speed fixes can confirm a stop. GPS loss is not stillness. */
final class RideMotion {
    private long stillSince=-1,lastFix=-1;
    void reset(){stillSince=lastFix=-1;}
    void fix(long now,double kmh,boolean reliable){
        if(!reliable||!Double.isFinite(kmh)||kmh<0){stillSince=-1;return;}
        if(lastFix<0||now-lastFix>4000||now<lastFix)stillSince=-1;
        lastFix=now;
        if(kmh>=1.5)stillSince=-1;
        else if(stillSince<0)stillSince=now;
    }
    boolean stopped(long now,long duration){return stillSince>=0&&lastFix>=0&&now-lastFix<=4000&&now-stillSince>=duration;}
}
