package de.wortmonster.jbdtrigger;

/** Value-based scale colours with configurable solid zones and smooth transitions. */
final class ScaleColorRange {
    private ScaleColorRange() {}

    static int color(double progress, int lowColor, int middleColor, int highColor,
                     int lowFullPercent, int highFullPercent, boolean useMiddle) {
        if (!Double.isFinite(progress)) return lowColor;
        int low = clamp(lowFullPercent, 0, 99);
        int high = clamp(highFullPercent, 1, 100);
        if (low >= high) { low = 20; high = 80; }
        double value = clamp(progress, 0, 1) * 100;
        if (value <= low) return lowColor;
        if (value >= high) return highColor;
        double transition = (value - low) / (high - low);
        if (!useMiddle) return blend(lowColor, highColor, transition);
        return transition <= .5
                ? blend(lowColor, middleColor, transition * 2)
                : blend(middleColor, highColor, (transition - .5) * 2);
    }

    private static int blend(int from, int to, double amount) {
        int a = channel(from, to, 24, amount);
        int r = channel(from, to, 16, amount);
        int g = channel(from, to, 8, amount);
        int b = channel(from, to, 0, amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int channel(int from, int to, int shift, double amount) {
        int a = from >>> shift & 255, b = to >>> shift & 255;
        return clamp((int)Math.round(a + (b - a) * amount), 0, 255);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
