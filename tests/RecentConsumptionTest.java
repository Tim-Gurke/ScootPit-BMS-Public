package de.wortmonster.jbdtrigger;
public class RecentConsumptionTest {
    static void close(double actual,double expected) { if(Math.abs(actual-expected)>.01)throw new AssertionError(actual+" != "+expected); }
    public static void main(String[] args) {
        RecentConsumption c=new RecentConsumption();c.reset();c.add(100,2);close(c.estimate(15),15);
        c.add(300,6);close(c.estimate(15),20);
        for(int m=400;m<=2000;m+=100)c.add(m,m*.02);
        close(c.estimate(15),20);
        for(int m=2100;m<=3000;m+=100)c.add(m,40+(m-2000)*.04);
        if(c.estimate(15)<35)throw new AssertionError("recent uphill consumption did not take priority");
        double before=c.estimate(15);c.add(1000,1);close(c.estimate(15),before);
        c.reset();close(c.estimate(17),17);
        c.add(300,0);close(c.estimate(17),17);
        c.add(300,3);close(c.estimate(17),10);
        c.reset();for(int m=100;m<=600;m+=100)c.add(m,m*.02);close(c.whPerKm(500),20);
        c.reset();for(int m=100;m<=400;m+=100)c.add(m,m*.02);if(!Double.isNaN(c.whPerKm(500)))throw new AssertionError("500 m gauge must wait for enough distance");
        c.reset();for(int m=100;m<=1600;m+=100)c.add(m,m*.02);close(c.whPerKm(1000),20);
        for(int m=1700;m<=3000;m+=100)c.add(m,m*.02);close(c.whPerKm(1000),20);
        c.resetAt(1000,20);c.add(1100,22);close(c.estimate(17),17);c.add(1300,26);close(c.estimate(17),20);
        System.out.println("Recent consumption: OK");
    }
}

