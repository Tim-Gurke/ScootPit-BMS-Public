package de.wortmonster.jbdtrigger;

public final class ScaleDirection {
    private ScaleDirection() {}

    /** Maps a normalized value to its position along the visible scale. */
    public static double position(double progress, boolean reversed) {
        double value = Double.isFinite(progress) ? Math.max(0d, Math.min(1d, progress)) : 0d;
        return reversed ? 1d - value : value;
    }
}
