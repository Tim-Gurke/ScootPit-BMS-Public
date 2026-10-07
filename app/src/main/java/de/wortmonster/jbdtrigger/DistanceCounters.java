package de.wortmonster.jbdtrigger;

import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class DistanceCounters {
    static String day(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date(time));}
    static double number(SharedPreferences p,String key){try{return Double.parseDouble(p.getString(key,"0"));}catch(Exception e){return 0;}}
    static double daily(SharedPreferences p,long now){return day(now).equals(p.getString("daily_day",""))?number(p,"daily_km"):0;}
    static void add(SharedPreferences p,double meters,long time){
        if(!Double.isFinite(meters)||meters<=0)return;
        p.edit().putString("total_km",Double.toString(number(p,"total_km")+meters/1000))
            .putString("tour_km",Double.toString(number(p,"tour_km")+meters/1000))
            .putString("daily_km",Double.toString(daily(p,time)+meters/1000)).putString("daily_day",day(time)).apply();
    }
}
