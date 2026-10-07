package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Local working data is retained. Completed trips and settings snapshots use independently selected SAF trees. */
final class StorageFolders {
    static final String SETTINGS="settings_tree",TRIPS="trips_tree",NAME="Joyor-Cockpit-settings.json";
    private static final Set<String> KEYS=new HashSet<>(Arrays.asList("auto_scooter","stop_seconds","daily_day","daily_km","tour_km","routine_start_title","routine_start_text","routine_end_title","routine_end_text","journal_stats","header_name","header_color","temp1_label","temp2_label","bms_available","temperature_history","device_name","device_address","active_current","active_seconds","idle_seconds","monitor_timeout","connection_policy_version","connect_rssi","connect_confirm_seconds","departure_rssi","scan_absent","scan_weak","scan_good","scan_pause","departure_seconds","gps_max_kmh","cockpit_board_landscape","capacity_ah","nominal_voltage","reserve_percent","reference_wh_km","learned_wh_km","total_km","accent_color","app_background","tile_background","tile_text","cockpit_layout","cockpit_board","weather_enabled"));
    private static StorageFolders instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean importing;
    private volatile long lastSaved;
    private final Runnable saveTask=()->worker.execute(this::save);
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->{
        if(!importing && (KEYS.contains(k)||ScooterProfiles.LIST.equals(k)||ScooterProfiles.ACTIVE.equals(k)) && !p.getString(SETTINGS,"").isEmpty()){
            main.removeCallbacks(saveTask);main.postDelayed(saveTask,System.currentTimeMillis()-lastSaved>10000?0:2000);
        }
    };
    private StorageFolders(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("settings",Context.MODE_PRIVATE);prefs.registerOnSharedPreferenceChangeListener(listener);worker.execute(this::retry);}
    static synchronized StorageFolders install(Context c){if(instance==null)instance=new StorageFolders(c);return instance;}
    interface Result{void done(String error);}
    void run(Runnable task,Result callback){worker.execute(()->{String error=null;try{task.run();}catch(Exception e){error=e.getMessage();}String message=error;main.post(()->callback.done(message));});}
    static Uri document(Context context,String tree,String name,boolean create,String mime)throws Exception{
        Uri uri=Uri.parse(tree);String id=DocumentsContract.getTreeDocumentId(uri);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(uri,id);
        try(Cursor c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(c==null)throw new IOException("Ordner nicht erreichbar");while(c.moveToNext())if(name.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(uri,c.getString(0));
        }
        if(!create)return null;
        Uri result=DocumentsContract.createDocument(context.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(uri,id),mime,name);
        if(result==null)throw new IOException("Datei konnte nicht angelegt werden");return result;
    }
    static String read(Context c,Uri uri)throws Exception{
        try(InputStream input=c.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(input==null)throw new IOException("Datei nicht lesbar");byte[] buffer=new byte[8192];int count,total=0;while((count=input.read(buffer))>=0){total+=count;if(total>32*1024*1024)throw new IOException("Einstellungsdatei zu groß");out.write(buffer,0,count);}return out.toString("UTF-8");
        }
    }
    static void write(Context c,Uri uri,byte[] bytes)throws Exception{
        try(OutputStream output=c.getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("Datei nicht beschreibbar");output.write(bytes);output.flush();}
    }
    JSONObject snapshot()throws Exception{
        synchronized(ScooterProfiles.class){
        JSONObject values=new JSONObject();for(Map.Entry<String,?> e:prefs.getAll().entrySet())if(KEYS.contains(e.getKey()))values.put(e.getKey(),e.getValue());
        if(!values.has("cockpit_board"))values.put("cockpit_board",CockpitBoard.load(prefs).toString());
        return new JSONObject().put("format","Joyor Cockpit settings").put("version",2).put("settings",values).put("profiles",ScooterProfiles.export(prefs)).put("active_profile",prefs.getString(ScooterProfiles.ACTIVE,""));
        }
    }
    void importSettings(String tree)throws Exception{
        Uri file=document(context,tree,NAME,false,"application/json");if(file==null)throw new IOException("Keine Einstellungsdatei in diesem Ordner");
        JSONObject root=new JSONObject(read(context,file));if(!root.optString("format").equals("Joyor Cockpit settings")||(root.optInt("version")!=1&&root.optInt("version")!=2))throw new IOException("Unbekanntes Einstellungsformat");
        JSONObject values=root.getJSONObject("settings");
        validateValues(values,false);
        JSONArray profiles=null;String active="";
        if(root.getInt("version")==2){profiles=root.getJSONArray("profiles");active=root.getString("active_profile");validateProfiles(profiles,active);}
        importing=true;main.removeCallbacks(saveTask);
        try{synchronized(ScooterProfiles.class){if(BmsMonitorService.running)throw new IOException("Zuerst Bereitschaft beenden");SharedPreferences.Editor editor=prefs.edit();
            if(profiles!=null){for(String key:prefs.getAll().keySet())if(ScooterProfiles.scoped(key))editor.remove(key);
                for(int i=0;i<profiles.length();i++)if(profiles.getJSONObject(i).getString("id").equals(active))ScooterProfiles.put(editor,profiles.getJSONObject(i).getJSONObject("settings"));
                editor.putString(ScooterProfiles.LIST,profiles.toString()).putString(ScooterProfiles.ACTIVE,active);
                if(values.has("auto_scooter"))editor.putBoolean("auto_scooter",values.getBoolean("auto_scooter"));
            }else{
                // Legacy files replace only the active scooter, preserving other profiles and existing trip files.
                for(String key:KEYS)editor.remove(key);
                for(String key:KEYS)if(values.has(key)){Object v=values.get(key);if(v instanceof Boolean)editor.putBoolean(key,(Boolean)v);else editor.putString(key,(String)v);}
            }
            if(!editor.commit())throw new IOException("Einstellungen konnten nicht gespeichert werden");
        }}finally{main.post(()->importing=false);}
    }
    static void validateProfiles(JSONArray profiles,String active)throws Exception{
        if(profiles.length()<1||profiles.length()>20)throw new IOException("1–20 Scooter-Profile erforderlich");Set<String> ids=new HashSet<>();boolean found=false;
        for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);String id=p.getString("id"),name=p.getString("name");if(!id.matches("[A-Za-z0-9-]{1,64}")||!ids.add(id)||name.trim().isEmpty()||name.length()>40||name.contains("\n"))throw new IOException("Ungültiges Scooter-Profil");if(id.equals(active))found=true;validateValues(p.getJSONObject("settings"),true);}
        if(!found)throw new IOException("Aktives Scooter-Profil fehlt");
    }
    static void validateValues(JSONObject values,boolean profile)throws Exception{
        Iterator<String> keys=values.keys();while(keys.hasNext()){
            String key=keys.next();if(!KEYS.contains(key)&&!(profile&&ScooterProfiles.scoped(key)))continue;
            Object value=values.get(key);
            if(key.equals("weather_enabled")||key.equals("auto_scooter")){if(!(value instanceof Boolean))throw new IOException("Ungültiger Wetterwert");continue;}
            if(key.equals("weather_at")||key.equals("last_started_at")||key.equals("last_ended_at")){if(!(value instanceof Number)||((Number)value).doubleValue()<0)throw new IOException("Ungültiger Zeitpunkt");continue;}
            if(!(value instanceof String))throw new IOException("Ungültiger Wert: "+key);String text=(String)value;
            if((key.equals("cockpit_board")||key.equals("cockpit_board_landscape"))){CockpitBoard.validate(new JSONArray(text));continue;}
            if(key.equals("cockpit_layout")){JSONArray rows=new JSONArray(text);if(rows.length()<1||rows.length()>30)throw new IOException("Ungültiges altes Layout");for(int i=0;i<rows.length();i++){JSONArray tiles=rows.getJSONObject(i).getJSONArray("tiles");if(tiles.length()<1||tiles.length()>6)throw new IOException("Ungültige Zeile");}continue;}
            if(key.endsWith("color")||key.equals("app_background")||key.equals("tile_background")||key.equals("tile_text")){android.graphics.Color.parseColor(text);continue;}
            if(key.equals("device_address")){if(!android.bluetooth.BluetoothAdapter.checkBluetoothAddress(text))throw new IOException("Ungültige BMS-Adresse");continue;}
            if(key.equals("header_name")||key.equals("temp1_label")||key.equals("temp2_label")){if(text.trim().isEmpty()||text.length()>40||text.contains("\n"))throw new IOException("Ungültige Beschriftung");continue;}
            if(key.equals("temperature_history")){TemperatureHistory.validate(new JSONArray(text));continue;}
            if(key.equals("bms_available")){JSONArray list=new JSONArray(text);if(list.length()>150)throw new IOException("Zu viele BMS-Daten");for(int i=0;i<list.length();i++)if(!BmsExtras.known(list.getString(i)))throw new IOException("Unbekannte BMS-Kachel");continue;}
            if(key.startsWith("routine_")||key.equals("daily_day")){if(text.length()>2000)throw new IOException("Text zu lang");continue;}
            if(key.equals("journal_stats")){new JSONArray(text);continue;}
            if(key.equals("device_name")||key.equals("trips_tree")||key.equals("trips_tree_label")||key.equals("last_gpx")||key.equals("last_csv"))continue;
            if(key.equals("connect_rssi")||key.equals("departure_rssi")){double r=Double.parseDouble(text);if(!Double.isFinite(r)||r< -120||r> -30)throw new IOException("Ungültige Empfangsschwelle");continue;}
            double n=Double.parseDouble(text);if(!Double.isFinite(n)||(!key.equals("outside_temperature")&&n<0)||n>10000000)throw new IOException("Ungültige Zahl: "+key);
            if((key.equals("capacity_ah")||key.equals("nominal_voltage")||key.equals("reference_wh_km"))&&n<=0)throw new IOException("Wert muss positiv sein: "+key);
            if(key.equals("reserve_percent")&&n>=100)throw new IOException("Reserve muss unter 100 % sein");
        }
    }
    void choose(String key,String tree,boolean load)throws Exception{
        // Probe write access without touching an existing settings file.
        String probe="Joyor-Cockpit-test-"+System.currentTimeMillis()+".txt";Uri test=document(context,tree,probe,true,"text/plain");write(context,test,new byte[0]);DocumentsContract.deleteDocument(context.getContentResolver(),test);
        if(load)importSettings(tree);
        String label=DocumentsContract.getTreeDocumentId(Uri.parse(tree));
        try(Cursor c=context.getContentResolver().query(DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree),label),new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())label=c.getString(0);}catch(Exception ignored){}
        String old=prefs.getString(key,"");prefs.edit().putString(key,tree).putString(key+"_label",label).commit();
        try{if(key.equals(SETTINGS)&&!load)saveOrThrow();}catch(Exception e){prefs.edit().putString(key,old).commit();throw e;}
    }
    void saveOrThrow()throws Exception{String tree=prefs.getString(SETTINGS,"");if(tree.isEmpty())return;write(context,document(context,tree,NAME,true,"application/json"),snapshot().toString(2).getBytes(StandardCharsets.UTF_8));lastSaved=System.currentTimeMillis();prefs.edit().remove("settings_storage_error").apply();}
    void save(){try{saveOrThrow();}catch(Exception e){prefs.edit().putString("settings_storage_error","Einstellungen: "+e.getMessage()).apply();}}
    void saveNow(){main.removeCallbacks(saveTask);worker.execute(this::save);}
    void exportTrip(String tree,File... files){if(tree.isEmpty())return;worker.execute(()->{
        try{JSONArray jobs=new JSONArray(prefs.getString("pending_exports","[]"));for(File file:files)jobs.put(new JSONObject().put("tree",tree).put("path",file.getAbsolutePath()));prefs.edit().putString("pending_exports",jobs.toString()).commit();retry();}catch(Exception e){prefs.edit().putString("trip_storage_error",e.getMessage()).apply();}
    });}
    void retry(){
        JSONArray pending=new JSONArray();String error=null;
        try{JSONArray jobs=new JSONArray(prefs.getString("pending_exports","[]"));for(int i=0;i<jobs.length();i++){JSONObject job=jobs.getJSONObject(i);try{
            File file=new File(job.getString("path"));if(!file.isFile())throw new IOException("Lokale Fahrtdatei fehlt: "+file.getName());
            Uri uri=document(context,job.getString("tree"),file.getName(),true,file.getName().endsWith(".gpx")?"application/gpx+xml":file.getName().endsWith(".csv")?"text/csv":"application/json");
            try(InputStream input=new FileInputStream(file);OutputStream output=context.getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("Fahrtenordner nicht beschreibbar");byte[] b=new byte[8192];int n;while((n=input.read(b))>=0)output.write(b,0,n);output.flush();}
        }catch(Exception e){pending.put(job);error=e.getMessage();}}
        }catch(Exception e){error=e.getMessage();}
        SharedPreferences.Editor edit=prefs.edit().putString("pending_exports",pending.toString());if(error==null)edit.remove("trip_storage_error");else edit.putString("trip_storage_error","Fahrt lokal gesichert; Kopie in Zielordner ausstehend: "+error);edit.apply();
    }
    void retryAsync(){worker.execute(()->{retry();save();});}
}
