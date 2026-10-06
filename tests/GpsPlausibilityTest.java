package de.wortmonster.jbdtrigger;
public class GpsPlausibilityTest {
    public static void main(String[] args){
        if(GpsPlausibility.accept(92,true,5,1,4,4,20,45))throw new AssertionError("92 km/h spike accepted");
        if(GpsPlausibility.accept(30,true,4,1,20,20,0,45))throw new AssertionError("Sudden jump accepted");
        if(GpsPlausibility.accept(20,true,100,1,3,3,20,45))throw new AssertionError("Position jump accepted");
        if(!GpsPlausibility.accept(20,true,5.6,1,3,3,19,45))throw new AssertionError("Normal ride rejected");
        if(!GpsPlausibility.accept(0,true,0,1,3,3,20,45))throw new AssertionError("Stop rejected");
        if(GpsPlausibility.accept(0,false,20,1,3,3,0,45))throw new AssertionError("Derived spike accepted");
        if(GpsPlausibility.accept(Double.NaN,true,0,0,3,3,0,45))throw new AssertionError("NaN accepted");
        System.out.println("GPS spikes, jumps, stop and normal ride: OK");
    }
}
