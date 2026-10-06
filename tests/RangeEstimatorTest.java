package de.wortmonster.jbdtrigger;
public class RangeEstimatorTest {
    public static void main(String[] args) {
        eq(RangeEstimator.range(50,6.25,12.5,99,48,10,15),16);
        eq(RangeEstimator.range(50,0,0,12.5,48,10,15),16);
        eq(RangeEstimator.range(5,0.625,12.5,12.5,48,10,15),0);
        eq(RangeEstimator.consumption(15,100,1),15);
        eq(RangeEstimator.consumption(15,1500,30),17.5);
        eq(RangeEstimator.consumption(15,3000,60),20);
        if(!Double.isNaN(RangeEstimator.range(-1,0,0,12.5,48,10,15)))throw new AssertionError("unknown SOC");
        System.out.println("Range reserve, BMS/fallback capacities and smoothing: OK");
    }
    private static void eq(double actual,double expected){if(Math.abs(actual-expected)>.00001)throw new AssertionError(actual+" != "+expected);}
}
