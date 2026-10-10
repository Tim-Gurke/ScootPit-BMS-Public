package de.wortmonster.jbdtrigger;
public class RideMotionTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        RideMotion m=new RideMotion();
        for(int i=0;i<=44;i++)m.fix(i*1000,0,true);check(!m.stopped(44000,45000));m.fix(45000,0,true);check(m.stopped(45000,45000));
        m.fix(46000,8,true);check(!m.stopped(46000,45000));
        m.reset();m.fix(0,0,true);m.fix(60000,0,true);check(!m.stopped(60000,45000));
        m.reset();for(int i=0;i<=45;i++)m.fix(i*1000,0,true);check(!m.stopped(51000,45000));
        m.fix(51000,0,false);check(!m.stopped(51000,45000));
        m.reset();for(int i=0;i<90;i++)m.fix(i*1000,0,true);check(!m.stopped(89000,90000));m.fix(90000,0,true);check(m.stopped(90000,90000));
        m.reset();for(int i=0;i<=15;i++)m.fix(i*1000,4,true,false);check(m.walking(15000,15000));
        m.reset();for(int i=0;i<=15;i++)m.fix(i*1000,4,true,true);check(!m.walking(15000,15000));
        check(RideMotion.likelyWalking(4,true,false));check(!RideMotion.likelyWalking(8,true,false));check(!RideMotion.likelyWalking(4,true,true));check(!RideMotion.likelyWalking(4,false,false));
        System.out.println("RideMotionTest OK");
    }
}

