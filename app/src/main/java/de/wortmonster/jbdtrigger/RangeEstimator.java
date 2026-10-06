package de.wortmonster.jbdtrigger;

final class RangeEstimator {
    static double consumption(double reference, double distanceM, double energyWh) {
        if(distanceM < 500 || energyWh <= 0)return reference;
        double weight = Math.min(1, distanceM / 3000.0);
        return reference * (1 - weight) + energyWh / (distanceM / 1000.0) * weight;
    }
    static double range(int soc, double remainingAh, double fullAh, double fallbackAh,
                        double nominalVoltage, double reservePercent, double whPerKm) {
        if(soc < 0 || soc > 100 || nominalVoltage <= 0 || whPerKm <= 0)return Double.NaN;
        double capacity = fullAh > 0 ? fullAh : fallbackAh;
        double available = fullAh > 0 ? Math.min(capacity, Math.max(0, remainingAh)) : capacity * soc / 100.0;
        return Math.max(0, available - capacity * reservePercent / 100.0) * nominalVoltage / whPerKm;
    }
}
