package de.wortmonster.jbdtrigger;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.PathParser;
import org.json.JSONObject;
import java.util.*;

/** One scalable line language for every metric, action and optional BMS field. */
final class CockpitSymbols extends Drawable {
    private final String key;
    private final int color;
    private static final Map<String,Path> CACHE=new HashMap<>();
    CockpitSymbols(String key,int color){this.key=key;this.color=color;}
    static int defaultColor(String key,boolean light){
        switch(family(key)){
            case "play":case "stop":return Color.BLACK;
            case "battery":return light?0xff238b35:0xff73df58;
            case "clock":case "route":return light?0xff087da1:0xff62cce5;
            case "pause":return light?0xff7751a8:0xffbb9ce8;
            case "thermometer":return light?0xffb85441:0xffffa58a;
            case "weather":return light?0xffb6800c:0xffffd166;
            case "mountain":return light?0xff39856e:0xff7cdbb1;
            default:return light?0xff1565c0:0xffffb41f;
        }
    }
    static int mode(JSONObject config){return config.has("heading_mode")?config.optInt("heading_mode",2):config.optBoolean("show_title",!Arrays.asList("image","free_text","speed").contains(config.optString("key")))?2:3;}
    static boolean icon(JSONObject config){int mode=mode(config);return mode==0||mode==2;}
    static boolean title(JSONObject config){int mode=mode(config);return mode==1||mode==2;}
    static String caption(JSONObject config){String key=config.optString("key"),caption=config.optString("caption",CockpitLayout.title(key));return caption.equals("🔋")||caption.equals("🌞🌧️🌤️")?CockpitLayout.title(key):caption;}
    static String family(String key){
        switch(key){
            case "speed":case "average":case "maximum":return "gauge";
            case "soc":return "battery";
            case "range":return "road";
            case "distance":return "route";
            case "total":return "odometer";
            case "daily":return "calendar";
            case "tour":return "tour";
            case "power":case "max_power":return "bolt";
            case "voltage":return "voltage";
            case "current":return "current";
            case "energy":return "energy";
            case "consumption":return "consumption";
            case "moving":return "clock";
            case "standing":return "pause";
            case "altitude":case "ascent":return "mountain";
            case "temp1":case "temp2":return "thermometer";
            case "outside":return "weather";
            case "bms_output":return "switch";
            case "ready_start":return "play";
            case "ready_end":case "trip_end":return "stop";
            case "log":case "free_text":return "text";
            case "image":return "image";
            case "journal":return "journal";
        }
        if(key.startsWith("bms_temp_"))return "thermometer";
        if(key.startsWith("bms_cell_"))return "cell";
        if(key.startsWith("bms_event_")||key.equals("bms_protection"))return "shield";
        if(key.contains("balancing"))return "balance";
        if(key.contains("fet"))return "switch";
        if(key.endsWith("_ah")||key.equals("bms_cells"))return "battery";
        if(key.equals("bms_cycles"))return "cycle";
        if(key.equals("bms_date"))return "calendar";
        return "chip";
    }
    private static String path(String family){switch(family){
        case "gauge":return "M3,19 A10,10 0,1 1,21,19 M5,15 L7,14 M7,7 L9,9 M12,4 L12,7 M17,7 L15,9 M19,15 L17,14 M12,14 L17,10";
        case "tour":return "M3,19 A10,10 0,1 1,21,19 M6,7 L8,9 M12,4 L12,7 M18,7 L16,9 M12,14 L16,11 M8,20 L16,20";
        case "battery":return "M3,6 L19,6 L19,18 L3,18 Z M19,10 L22,10 L22,14 L19,14 M7,9 L7,15 M11,9 L11,15 M15,9 L15,15";
        case "road":return "M8,3 L3,21 M16,3 L21,21 M12,3 L12,7 M12,10 L12,14 M12,17 L12,21";
        case "route":return "M3,5 A3,3 0,1 0,9,5 A3,3 0,1 0,3,5 M15,18 A3,3 0,1 0,21,18 A3,3 0,1 0,15,18 M6,8 L6,13 Q6,16 10,16 L13,16 M11,16 L14,16";
        case "odometer":return "M3,4 L21,4 L21,20 L3,20 Z M7,8 L7,13 M11,8 L11,13 M15,8 L15,13 M6,17 L18,17";
        case "calendar":return "M3,5 L21,5 L21,21 L3,21 Z M7,2 L7,7 M17,2 L17,7 M3,10 L21,10 M7,14 L10,14 M14,14 L17,14 M7,18 L10,18";
        case "bolt":return "M14,2 L5,13 L11,13 L9,22 L20,9 L13,9 Z";
        case "voltage":return "M3,7 L8,19 L13,7 M17,5 L17,11 M14,8 L20,8 M16,18 L20,18";
        case "current":return "M2,12 Q5,2 8,12 T14,12 T20,12 M17,19 L22,19 L20,17 M22,19 L20,21";
        case "energy":return "M3,6 L21,6 L21,20 L3,20 Z M8,3 L16,3 M14,8 L9,14 L13,14 L11,18";
        case "consumption":return "M3,18 L8,18 L8,12 L13,12 L13,7 L18,7 L18,3 M3,21 L21,21 M18,12 L21,15 L18,18";
        case "clock":return "M12,4 A9,9 0,1 0,12,22 A9,9 0,1 0,12,4 M9,1 L15,1 M12,8 L12,13 L17,13";
        case "pause":return "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 M9,8 L9,16 M15,8 L15,16";
        case "mountain":return "M2,20 L10,5 L17,20 Z M13,12 L17,7 L23,20 M7,11 L10,13 L12,10";
        case "thermometer":return "M10,4 A2,2 0,0 1,14,4 L14,14 A5,5 0,1 1,10,14 Z M12,8 L12,18 M17,6 L21,6 M17,10 L20,10";
        case "weather":return "M8,4 A4,4 0,1 0,8,12 A4,4 0,1 0,8,4 M8,1 L8,2 M1,8 L2,8 M3,3 L4,4 M13,3 L12,4 M8,18 Q3,18 5,14 Q7,11 10,13 Q14,6 18,12 Q23,12 22,16 Q22,18 18,18 Z";
        case "switch":return "M7,6 L17,6 A6,6 0,0 1,17,18 L7,18 A6,6 0,0 1,7,6 M7,9 A3,3 0,1 0,7,15 A3,3 0,1 0,7,9";
        case "play":return "M5,3 L21,12 L5,21 Z";
        case "stop":return "M5,5 L19,5 L19,19 L5,19 Z";
        case "text":return "M4,3 L20,3 L20,21 L4,21 Z M8,7 L16,7 M8,11 L16,11 M8,15 L14,15";
        case "image":return "M3,4 L21,4 L21,20 L3,20 Z M3,17 L9,11 L14,16 L17,13 L21,17 M16,7 A2,2 0,1 0,16,11 A2,2 0,1 0,16,7";
        case "journal":return "M2,4 L8,2 L16,5 L22,3 L22,20 L16,22 L8,19 L2,21 Z M8,2 L8,19 M16,5 L16,22 M5,12 L7,12 M10,13 L12,13 M14,14 L15,14 M18,9 A2,2 0,1 0,22,9 A2,2 0,1 0,18,9 M18,11 L20,14 L22,11";
        case "cell":return "M4,5 L20,5 L20,21 L4,21 Z M9,2 L15,2 M8,13 L16,13 M12,9 L12,17";
        case "shield":return "M12,2 L21,6 L20,15 Q18,20 12,22 Q6,20 4,15 L3,6 Z M12,7 L12,13 M12,17 L12,18";
        case "balance":return "M12,3 L12,21 M5,7 L19,7 M5,7 L2,15 L8,15 Z M19,7 L16,15 L22,15 Z M7,21 L17,21";
        case "cycle":return "M4,8 A9,9 0,0 1,21,11 M4,8 L4,3 M4,8 L9,8 M20,16 A9,9 0,0 1,3,13 M20,16 L20,21 M20,16 L15,16";
        default:return "M5,5 L19,5 L19,19 L5,19 Z M9,9 L15,9 L15,15 L9,15 Z M8,2 L8,5 M16,2 L16,5 M8,19 L8,22 M16,19 L16,22 M2,8 L5,8 M2,16 L5,16 M19,8 L22,8 M19,16 L22,16";
    }}
    static void draw(Canvas canvas,String key,float x,float y,float size,int color){
        String family=family(key);Path path=CACHE.get(family);if(path==null){path=PathParser.createPathFromPathData(path(family));CACHE.put(family,path);}
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.65f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        canvas.save();canvas.translate(x,y);canvas.scale(size/24,size/24);canvas.drawPath(path,p);
        String badge=key.equals("average")?"Ø":key.equals("maximum")||key.equals("max_power")?"↑":key.equals("ascent")?"+":key.startsWith("bms_cell_")&&key.substring(9).matches("[0-9]+")?key.substring(9):key.equals("temp1")?"1":key.equals("temp2")?"2":"";
        if(!badge.isEmpty()){p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(7);canvas.drawText(badge,15,23,p);}canvas.restore();
    }
    @Override public void draw(Canvas c){Rect b=getBounds();float size=Math.min(b.width(),b.height());draw(c,key,b.left+(b.width()-size)/2,b.top+(b.height()-size)/2,size,color);}
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
