package de.wortmonster.jbdtrigger;

public class ScaleDirectionTest {
    public static void main(String[] args) {
        eq(0, ScaleDirection.position(0, false));
        eq(.25, ScaleDirection.position(.25, false));
        eq(1, ScaleDirection.position(1, false));
        eq(1, ScaleDirection.position(0, true));
        eq(.75, ScaleDirection.position(.25, true));
        eq(0, ScaleDirection.position(1, true));
        eq(1, ScaleDirection.position(-2, true));
        eq(0, ScaleDirection.position(2, true));
        eq(1, ScaleDirection.position(Double.NaN, true));
        System.out.println("Scale direction: OK");
    }
    private static void eq(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-9) throw new AssertionError(expected + " != " + actual);
    }
}
