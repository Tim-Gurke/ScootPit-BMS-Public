package de.wortmonster.jbdtrigger;

import java.util.ArrayDeque;

/** Cumulative GPS distance and integrated battery energy, rather than W / instantaneous speed. */
final class RecentConsumption {
    private final ArrayDeque<double[]> samples = new ArrayDeque<>();
    private double distance, energy, baseDistance, baseEnergy;
    void reset() { resetAt(0,0); }
    void resetAt(double meters,double wh){samples.clear();distance=energy=0;baseDistance=meters;baseEnergy=wh;samples.add(new double[]{0,0});}
    void add(double meters, double wh) {
        meters-=baseDistance;wh-=baseEnergy;
        if (!Double.isFinite(meters) || !Double.isFinite(wh) || meters<distance || wh<energy || meters<0 || wh<0) return;
        distance=meters; energy=wh;
        if (samples.isEmpty()) samples.add(new double[]{0,0});
        // Keep stationary energy in the next travelled segment, but keep memory bounded.
        if (meters==samples.peekLast()[0]) return;
        samples.add(new double[]{meters,wh});
        while(samples.size()>2 && meters-second()[0]>800) samples.removeFirst();
    }
    private double[] second() { java.util.Iterator<double[]> it=samples.iterator();it.next();return it.next(); }
    boolean ready() { return distance>=250 && energy>0; }
    double whPerKm(double windowMeters) {
        if(!Double.isFinite(windowMeters)||windowMeters<=0||energy<=0||samples.size()<2)return Double.NaN;
        double target=distance-windowMeters;
        if(target<samples.peekFirst()[0])return Double.NaN;
        double[] previous=null;
        for(double[] sample:samples){
            if(sample[0]>=target){
                if(previous==null&&Math.abs(sample[0]-target)>1e-9)return Double.NaN;
                double startEnergy=sample[1];
                if(previous!=null){double fraction=(target-previous[0])/Math.max(1e-9,sample[0]-previous[0]);startEnergy=previous[1]+fraction*(sample[1]-previous[1]);}
                double used=energy-startEnergy;
                return used>0?used/(windowMeters/1000.0):Double.NaN;
            }
            previous=sample;
        }
        return Double.NaN;
    }
    double estimate(double reference) {
        if (!ready()) return reference;
        double trip=energy/(distance/1000);
        double[] first=samples.peekFirst();
        double span=distance-first[0], used=energy-first[1];
        double recent=span>=200 && used>0 ? used/(span/1000):trip;
        return Math.max(1, .75*recent+.25*trip);
    }
}
