package de.wortmonster.jbdtrigger;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class CockpitLayout {
    static final double DEFAULT_SPEED_SCALE_MAX = 22;
    static final String[] KEYS = {"speed","soc","range","distance","total","power","voltage","current","energy","consumption","moving","standing","average","maximum","altitude","ascent","temp1","temp2","outside","max_power","bms_output","ready_start","ready_end","trip_end","log","image","free_text"};
    static final String[] TITLES = {"Geschwindigkeit","Akku","Restreichweite ≈","Fahrtstrecke","Gesamtkilometer","Leistung","Spannung","Strom","Verbrauchte Energie","Verbrauch","Fahrzeit","Standzeit","Durchschnitt","Maximum","Höhe","Höhenmeter","Temp1","Temp2","Außentemperatur","Maximale Fahrtleistung","BMS-Lastausgang","Bereitschaft starten","Bereitschaft beenden","Fahrt beenden","Statusdetails / Log","Bild","Freitext"};
    static JSONObject tile(String key) {
        JSONObject t = new JSONObject();
        try { t.put("key",key).put("font",28).put("weight",1).put("background","#1C2228").put("text","#FFFFFF");
            if(key.equals("speed"))t.put("scale_max",DEFAULT_SPEED_SCALE_MAX); }
        catch(Exception ignored) {}
        return t;
    }
    static JSONObject row(String... keys) {
        JSONObject r = new JSONObject(); JSONArray cells = new JSONArray();
        for(String key:keys)cells.put(tile(key));
        try { r.put("height",120).put("tiles",cells); } catch(Exception ignored) {}
        return r;
    }
    static JSONArray defaults() {
        JSONArray rows = new JSONArray();
        rows.put(row("soc","range"));
        JSONObject speed=row("speed");
        try { speed.put("height",170);speed.getJSONArray("tiles").getJSONObject(0).put("font",56); } catch(Exception ignored) {}
        rows.put(speed);rows.put(row("distance","power"));rows.put(row("temp1","temp2"));
        rows.put(row("total","outside"));rows.put(row("moving","standing"));
        return rows;
    }
    static JSONArray load(SharedPreferences prefs) {
        try {
            JSONArray rows=new JSONArray(prefs.getString("cockpit_layout",""));
            if(rows.length()<1 || rows.length()>30)throw new Exception();
            for(int i=0;i<rows.length();i++){
                JSONArray cells=rows.getJSONObject(i).getJSONArray("tiles");
                if(cells.length()<1 || cells.length()>6)throw new Exception();
            }
            return rows;
        }catch(Exception ignored){return defaults();}
    }
    static String title(String key) {
        for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return TITLES[i];
        String extra=BmsExtras.title(key);return extra==null?key:extra;
    }
}
