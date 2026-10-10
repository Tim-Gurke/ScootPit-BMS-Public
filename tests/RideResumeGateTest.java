package de.wortmonster.jbdtrigger;
public class RideResumeGateTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        RideResumeGate gate=new RideResumeGate();
        check(!gate.update(0,0,0.3,5,true));
        check(!gate.update(1000,2,0.3,6,true));
        check(!gate.update(2000,0.8,0.3,12,true));
        check(!gate.update(3000,1.2,0.3,12,true));
        check(!gate.update(4000,1.2,0.3,12,true));
        check(gate.update(5500,1.2,0.3,12,true));
        check(!gate.update(6000,2,0.3,12,false));
        System.out.println("RideResumeGateTest OK");
    }
}
