package de.wortmonster.jbdtrigger;
public class GridPackingTest {
 public static void main(String[]args){GridPacking.Cell[] c={new GridPacking.Cell(0,0,0,12,5),new GridPacking.Cell(1,0,3,6,2),new GridPacking.Cell(2,6,3,6,3),new GridPacking.Cell(3,0,6,12,2)};
 GridPacking.arrange(c,0,false);if(c[1].y!=5||c[2].y!=5||c[3].y!=8)throw new AssertionError("Cascading displacement");if(c[1].x!=0||c[2].x!=6||c[2].h!=3)throw new AssertionError("Changed size or column");
 c[0].h=2;GridPacking.arrange(c,-1,true);if(c[1].y!=2||c[2].y!=2||c[3].y!=5)throw new AssertionError("Gap compaction");System.out.println("Grid packing: OK");}
}
