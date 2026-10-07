package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** A local, labelled chart with gaps for missing measurements. */
final class JournalChart extends View {
    private final List<double[]> data;
    private final String unit;
    private final Paint paint=new Paint(3);
    JournalChart(Context c,List<double[]> data,String unit){super(c);this.data=data;this.unit=unit;setContentDescription("Diagramm "+unit);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
        float left=55*d,right=getWidth()-12*d,top=22*d,bottom=getHeight()-32*d;
        paint.setTextSize(11*d);paint.setStrokeWidth(d);paint.setColor(0xff8e9ba5);paint.setStyle(Paint.Style.FILL);
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,maxX=0;
        for(double[] point:data){if(Double.isFinite(point[1])){low=Math.min(low,point[1]);high=Math.max(high,point[1]);}maxX=Math.max(maxX,point[0]);}
        if(!Double.isFinite(low)){canvas.drawText("Keine aufgezeichneten Werte",left,top,paint);return;}
        if(!unit.equals("m"))low=Math.min(0,low);if(high-low<.01)high=low+1;maxX=Math.max(1,maxX);
        for(int i=0;i<=3;i++){float y=bottom-(bottom-top)*i/3;double value=low+(high-low)*i/3;canvas.drawText(String.format(Locale.GERMANY,"%.0f",value),4*d,y+4*d,paint);canvas.drawLine(left,y,right,y,paint);}
        canvas.drawText(unit,left,14*d,paint);canvas.drawText("0",left,bottom+20*d,paint);
        canvas.drawText(String.format(Locale.GERMANY,"%.1f min",maxX/60),Math.max(left,right-70*d),bottom+20*d,paint);
        Path path=new Path();boolean connected=false;
        for(double[] point:data){if(!Double.isFinite(point[1])){connected=false;continue;}
            float x=left+(float)(point[0]/maxX)*(right-left),y=bottom-(float)((point[1]-low)/(high-low))*(bottom-top);
            if(connected)path.lineTo(x,y);else path.moveTo(x,y);connected=true;
        }
        paint.setColor(CockpitTheme.color(getContext().getSharedPreferences("settings",0),"accent_color","#FF9800"));paint.setStrokeWidth(2*d);paint.setStyle(Paint.Style.STROKE);canvas.drawPath(path,paint);
    }
}
