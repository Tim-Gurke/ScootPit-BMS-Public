package de.wortmonster.jbdtrigger;

import android.content.SharedPreferences;
import android.graphics.Color;
import org.json.*;

final class CockpitTheme {
    static int color(SharedPreferences p,String key,String fallback){try{return Color.parseColor(p.getString(key,fallback));}catch(Exception e){return Color.parseColor(fallback);}}
    static boolean light(SharedPreferences p){int c=color(p,"app_background","#0B1015");return Color.red(c)*.2126+Color.green(c)*.7152+Color.blue(c)*.0722>150;}
    static int foreground(SharedPreferences p){return color(p,"tile_text",light(p)?"#172B40":"#FFFFFF");}
    static String scale(SharedPreferences p){return p.getString("scale_color",light(p)?"#CFDAE6":"#35434D");}
    static void apply(SharedPreferences p,boolean light,boolean replaceTiles)throws Exception{
        String accent=light?"#1565C0":"#FF9800",background=light?"#FFFFFF":"#0B1015",tile=light?"#F3F7FC":"#141B22",text=light?"#172B40":"#FFFFFF",scale=light?"#CFDAE6":"#35434D";
        SharedPreferences.Editor e=p.edit().putString("app_background",background).putString("accent_color",accent).putString("header_color",accent).putString("tile_background",tile).putString("tile_text",text).putString("scale_color",scale).putString("outline_color",light?"#D5DFEB":"#2C3843").putBoolean("background_gradient_enabled",false).putString("background_gradient_start",background).putString("background_gradient_end",light?"#DCEBFA":"#26384A");
        if(replaceTiles){for(String key:new String[]{"cockpit_board","cockpit_board_landscape"}){
            JSONArray tiles=CockpitBoard.load(p,key);for(int i=0;i<tiles.length();i++){JSONObject t=tiles.getJSONObject(i);boolean transparent=Color.alpha(Color.parseColor(t.optString("background",tile)))==0;
                t.put("background",transparent?"#00000000":tile).put("text",text).put("instrument_color",accent).put("icon_color",String.format(java.util.Locale.ROOT,"#%08X",CockpitSymbols.defaultColor(t.optString("key"),light))).put("scale_color",scale).put("custom_colors",true);
            }CockpitBoard.validate(tiles);e.putString(key,tiles.toString());
        }}e.apply();
    }
}
