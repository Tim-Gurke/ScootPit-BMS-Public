package de.wortmonster.jbdtrigger;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Active values remain in the original preferences for service and backup compatibility. */
final class ScooterProfiles {
    static final String LIST="scooter_profiles",ACTIVE="active_scooter";
    static final Set<String> KEYS=new HashSet<>(Arrays.asList("scale_color","outline_color","stop_seconds","daily_day","daily_km","tour_km","routine_start_title","routine_start_text","routine_end_title","routine_end_text","journal_stats","header_name","header_color","temp1_label","temp2_label","temperature_history","device_name","device_address","active_current","active_seconds","idle_seconds","monitor_timeout","connection_policy_version","connect_rssi","connect_confirm_seconds","departure_rssi","scan_absent","scan_weak","scan_good","scan_pause","departure_seconds","gps_max_kmh","cockpit_board_landscape","capacity_ah","nominal_voltage","reserve_percent","reference_wh_km","learned_wh_km","total_km","accent_color","app_background","tile_background","tile_text","cockpit_layout","cockpit_board","weather_enabled","outside_temperature","weather_at","bms_available","trips_tree","trips_tree_label"));
    static boolean scoped(String key){return KEYS.contains(key)||key.startsWith("last_");}
    static JSONObject capture(SharedPreferences p)throws JSONException{
        JSONObject values=new JSONObject();for(Map.Entry<String,?> e:p.getAll().entrySet())if(scoped(e.getKey()))values.put(e.getKey(),e.getValue());
        if(!values.has("cockpit_board"))values.put("cockpit_board",CockpitBoard.load(p).toString());return values;
    }
    static synchronized void install(SharedPreferences p){
        if(p.contains(LIST))return;
        try{String id=UUID.randomUUID().toString();JSONObject profile=new JSONObject().put("id",id).put("name","Mein Scooter").put("settings",capture(p));
            p.edit().putString(ACTIVE,id).putString(LIST,new JSONArray().put(profile).toString()).commit();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    static synchronized JSONArray export(SharedPreferences p)throws JSONException{
        install(p);JSONArray profiles=new JSONArray(p.getString(LIST,"[]"));String active=p.getString(ACTIVE,"");
        for(int i=0;i<profiles.length();i++)if(profiles.getJSONObject(i).getString("id").equals(active))profiles.getJSONObject(i).put("settings",capture(p));return profiles;
    }
    static String name(SharedPreferences p){try{JSONArray all=export(p);for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(p.getString(ACTIVE,"")))return all.getJSONObject(i).getString("name");}catch(Exception ignored){}return "Mein Scooter";}
    static synchronized void switchTo(SharedPreferences p,String id)throws Exception{
        if(BmsMonitorService.running)throw new Exception("Zuerst Bereitschaft beenden");
        switchValues(p,id);
    }
    static synchronized void switchForService(SharedPreferences p,String id)throws Exception{
        switchValues(p,id);
    }
    private static void switchValues(SharedPreferences p,String id)throws Exception{
        JSONArray all=export(p);JSONObject target=null;for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(id))target=all.getJSONObject(i);
        if(target==null)throw new Exception("Scooter-Profil fehlt");
        SharedPreferences.Editor edit=p.edit();for(String k:p.getAll().keySet())if(scoped(k))edit.remove(k);
        put(edit,target.getJSONObject("settings"));edit.putString(LIST,all.toString()).putString(ACTIVE,id);if(!edit.commit())throw new Exception("Profil konnte nicht gespeichert werden");
    }
    static JSONObject settingsFor(SharedPreferences p,String id){
        try{JSONArray all=export(p);for(int i=0;i<all.length();i++)if(id.equals(all.getJSONObject(i).getString("id")))return all.getJSONObject(i).getJSONObject("settings");}catch(Exception ignored){}return new JSONObject();
    }
    static synchronized java.util.Map<String,String> knownBms(SharedPreferences p){
        java.util.Map<String,String> result=new java.util.LinkedHashMap<>();
        try{JSONArray all=export(p);for(int i=0;i<all.length();i++){JSONObject profile=all.getJSONObject(i);String address=profile.getJSONObject("settings").optString("device_address","").toUpperCase(java.util.Locale.ROOT);
            if(android.bluetooth.BluetoothAdapter.checkBluetoothAddress(address)){if(result.containsKey(address))result.put(address,"");else result.put(address,profile.getString("id"));}}}
        catch(Exception e){android.util.Log.e("ScootPit","BMS-Profile konnten nicht gelesen werden",e);}
        result.values().removeIf(String::isEmpty);return result;
    }
    static void put(SharedPreferences.Editor edit,JSONObject values)throws JSONException{
        Iterator<String> keys=values.keys();while(keys.hasNext()){String k=keys.next();if(!scoped(k))continue;Object v=values.get(k);
            if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if((k.equals("weather_at")||k.equals("last_started_at")||k.equals("last_ended_at"))&&v instanceof Number)edit.putLong(k,((Number)v).longValue());else if(v instanceof String)edit.putString(k,(String)v);
        }
    }
    static synchronized void add(SharedPreferences p,String name,boolean copy)throws Exception{
        if(BmsMonitorService.running)throw new Exception("Zuerst Bereitschaft beenden");
        if(name.trim().isEmpty()||name.length()>40||name.contains("\n"))throw new Exception("Name mit 1–40 Zeichen erforderlich");
        JSONArray all=export(p);if(all.length()>=20)throw new Exception("Maximal 20 Scooter-Profile");String id=UUID.randomUUID().toString();
        JSONObject values=copy?capture(p):new JSONObject();
        // A copied design must never copy identity, accumulated distance or another scooter's consumption.
        for(String k:new String[]{"daily_day","daily_km","tour_km","device_address","device_name","total_km","learned_wh_km","temperature_history","bms_available","outside_temperature","weather_at","trips_tree","trips_tree_label"})values.remove(k);
        ArrayList<String> remove=new ArrayList<>();Iterator<String> keys=values.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("last_"))remove.add(k);}for(String k:remove)values.remove(k);
        all.put(new JSONObject().put("id",id).put("name",name.trim()).put("settings",values));p.edit().putString(LIST,all.toString()).commit();switchTo(p,id);
    }
    static synchronized void rename(SharedPreferences p,String name)throws Exception{
        if(name.trim().isEmpty()||name.length()>40||name.contains("\n"))throw new Exception("Name mit 1–40 Zeichen erforderlich");JSONArray all=export(p);for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getString("id").equals(p.getString(ACTIVE,"")))all.getJSONObject(i).put("name",name.trim());p.edit().putString(LIST,all.toString()).commit();
    }
    static synchronized void deleteActive(SharedPreferences p)throws Exception{
        if(BmsMonitorService.running)throw new Exception("Zuerst Bereitschaft beenden");JSONArray all=export(p);if(all.length()<2)throw new Exception("Das letzte Profil bleibt erhalten");
        String id=p.getString(ACTIVE,"");JSONArray kept=new JSONArray();for(int i=0;i<all.length();i++)if(!all.getJSONObject(i).getString("id").equals(id))kept.put(all.getJSONObject(i));
        // Switch first, then remove the old profile. Trip files remain available on disk.
        switchTo(p,kept.getJSONObject(0).getString("id"));p.edit().putString(LIST,kept.toString()).commit();
    }
}
