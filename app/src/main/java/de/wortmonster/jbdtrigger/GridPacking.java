package de.wortmonster.jbdtrigger;
import java.util.*;
/** Deterministic downward displacement; horizontal choices and sizes are preserved. */
final class GridPacking {
    static final class Cell {int x,y,w,h;final int id;Cell(int id,int x,int y,int w,int h){this.id=id;this.x=x;this.y=y;this.w=w;this.h=h;}}
    private static boolean overlaps(Cell a,Cell b){return a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y;}
    static void arrange(Cell[] cells,int locked,boolean compact){
        Cell[] order=cells.clone();Arrays.sort(order,Comparator.comparingInt((Cell c)->c.y).thenComparingInt(c->c.x).thenComparingInt(c->c.id));
        List<Cell> placed=new ArrayList<>();if(locked>=0)placed.add(cells[locked]);
        for(Cell cell:order){if(cell.id==locked)continue;if(compact)cell.y=0;
            boolean moved;do{moved=false;for(Cell other:placed)if(overlaps(cell,other)){cell.y=other.y+other.h;moved=true;}}while(moved);
            if(cell.y>600)throw new IllegalArgumentException("Raster ist voll");placed.add(cell);
        }
    }
}
