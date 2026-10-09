package de.wortmonster.jbdtrigger;

public class ScaleColorRangeTest {
    private static final int RED=0xffff0000, ORANGE=0xffff8000, GREEN=0xff00ff00;
    public static void main(String[] args) {
        eq(ScaleColorRange.color(1,RED,ORANGE,GREEN,20,80,false),GREEN);
        eq(ScaleColorRange.color(.8,RED,ORANGE,GREEN,20,80,false),GREEN);
        eq(ScaleColorRange.color(.2,RED,ORANGE,GREEN,20,80,false),RED);
        eq(ScaleColorRange.color(0,RED,ORANGE,GREEN,20,80,false),RED);
        eq(ScaleColorRange.color(.5,RED,ORANGE,GREEN,20,80,true),ORANGE);
        eq(ScaleColorRange.color(.5,RED,ORANGE,GREEN,20,80,false),0xff808000);
        eq(ScaleColorRange.color(.5,RED,ORANGE,GREEN,60,40,false),0xff808000);
        System.out.println("Scale color zones and gradients: OK");
    }
    private static void eq(int actual,int expected){if(actual!=expected)throw new AssertionError(Integer.toHexString(actual)+" != "+Integer.toHexString(expected));}
}
