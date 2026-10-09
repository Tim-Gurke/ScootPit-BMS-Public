package de.wortmonster.jbdtrigger;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.*;

final class JournalUi {
    private final Activity activity;
    private final SharedPreferences prefs;
    private int days=7;
    private AlertDialog listDialog;
    private static final String[] KEYS={"distance","times","speed","energy","power","altitude","temperature","speed_chart","power_chart","altitude_chart"};
    private static final String[] NAMES={"Strecke","Fahr- und Standzeit","Geschwindigkeit","Energie und Verbrauch","Leistung","Höhenmeter","Temperaturen","Geschwindigkeitsdiagramm","Leistungsdiagramm","Höhendiagramm"};
    JournalUi(Activity a,SharedPreferences p){activity=a;prefs=p;}
    private int dp(int v){return Math.round(v*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(12),dp(8),dp(12),dp(8));return l;}
    private TextView text(String value){TextView v=new TextView(activity);v.setText(value);v.setTextColor(CockpitTheme.foreground(prefs));v.setTextSize(15);v.setPadding(0,dp(8),0,dp(8));return v;}
    private Button button(String value,Runnable action){Button b=new Button(activity);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    private ScrollView scroll(LinearLayout l){ScrollView s=new ScrollView(activity);s.addView(l);return s;}
    private AlertDialog dialog(String title,LinearLayout l){AlertDialog d=new AlertDialog.Builder(activity).setTitle(title).setView(scroll(l)).setNegativeButton("Zurück",null).create();d.show();d.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.88));return d;}
    private static String format(double value,String unit){return Double.isFinite(value)?String.format(Locale.GERMANY,"%.2f %s",value,unit):"Nicht aufgezeichnet";}
    private static String duration(long ms){if(ms<0)return "Nicht aufgezeichnet";long s=ms/1000;return String.format(Locale.GERMANY,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
    private String date(long time){return new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.GERMANY).format(new Date(time));}
    private Set<String> selected(){Set<String> result=new HashSet<>();try{JSONArray a=new JSONArray(prefs.getString("journal_stats",new JSONArray(Arrays.asList(KEYS)).toString()));for(int i=0;i<a.length();i++)result.add(a.getString(i));}catch(Exception e){result.addAll(Arrays.asList(KEYS));}return result;}
    void show(){
        LinearLayout l=column();l.addView(text("Scooter: "+ScooterProfiles.name(prefs)));
        LinearLayout range=new LinearLayout(activity);
        range.addView(button("Letzte 7 Tage",()->reload(7)),new LinearLayout.LayoutParams(0,-2,1));range.addView(button("Letzte 30 Tage",()->reload(30)),new LinearLayout.LayoutParams(0,-2,1));l.addView(range);
        l.addView(button("Fahrt aus CSV oder GPX importieren",()->{if(activity instanceof MainActivity)((MainActivity)activity).chooseTripImport(this);}));
        TextView loading=text("Fahrten werden geladen …");l.addView(loading);listDialog=dialog("Fahrtenbuch · "+days+" Tage",l);
        AlertDialog target=listDialog;String profile=prefs.getString(ScooterProfiles.ACTIVE,"");final List<TripJournal.Ride> rides=new ArrayList<>();
        StorageFolders.install(activity).run(()->{try{rides.addAll(TripJournal.list(activity,prefs,days));}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{
            if(!target.isShowing()||activity.isFinishing())return;
            if(!profile.equals(prefs.getString(ScooterProfiles.ACTIVE,""))){loading.setText("Scooter wurde gewechselt. Fahrtenbuch erneut öffnen.");return;}
            l.removeView(loading);if(error!=null){l.addView(text(error));return;}
            l.addView(button("Zeitraum auswerten / Fahrten vergleichen",()->overview(rides)));
            if(rides.isEmpty())l.addView(text("In diesem Zeitraum wurden noch keine Fahrten für diesen Scooter gespeichert."));
            for(TripJournal.Ride ride:rides)l.addView(button(date(ride.start)+"\n"+format(ride.meters/1000,"km")+" · "+duration(ride.end-ride.start),()->openRide(ride)));
        });
    }
    void refreshAfterImport(){if(listDialog!=null)listDialog.dismiss();show();}
    private void reload(int amount){days=amount;if(listDialog!=null)listDialog.dismiss();show();}
    private void openRide(TripJournal.Ride ride){
        LinearLayout l=column();l.addView(text(date(ride.start)+" – "+date(ride.end)));
        l.addView(text(format(ride.meters/1000,"km")+" · "+duration(ride.end-ride.start)));
        l.addView(button("Statistiken anzeigen",()->load(ride,()->stats(ride))));
        l.addView(button("Route auf Karte anzeigen",()->load(ride,()->map(ride))));
        l.addView(button("GPX und CSV teilen",()->share(ride)));dialog("Fahrt auswählen",l);
    }
    private void load(TripJournal.Ride ride,Runnable next){
        AlertDialog wait=new AlertDialog.Builder(activity).setMessage("Aufzeichnung wird gelesen …").create();wait.show();
        StorageFolders.install(activity).run(()->{try{TripJournal.loadPoints(ride);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{wait.dismiss();if(activity.isFinishing())return;if(error!=null)Toast.makeText(activity,error,Toast.LENGTH_LONG).show();else next.run();});
    }
    private void chooseStats(Runnable next){
        Set<String> selection=selected();boolean[] checked=new boolean[KEYS.length];for(int i=0;i<checked.length;i++)checked[i]=selection.contains(KEYS[i]);
        new AlertDialog.Builder(activity).setTitle("Statistiken auswählen").setMultiChoiceItems(NAMES,checked,(d,i,on)->checked[i]=on)
            .setPositiveButton("Übernehmen",(d,w)->{JSONArray a=new JSONArray();for(int i=0;i<checked.length;i++)if(checked[i])a.put(KEYS[i]);prefs.edit().putString("journal_stats",a.toString()).apply();next.run();}).setNegativeButton("Zurück",null).show();
    }
    private void stats(TripJournal.Ride r){
        LinearLayout l=column();Set<String> s=selected();AlertDialog[] current=new AlertDialog[1];
        l.addView(button("Angezeigte Werte auswählen",()->chooseStats(()->{current[0].dismiss();stats(r);})));l.addView(text(date(r.start)));
        if(s.contains("distance"))l.addView(text("Strecke: "+format(r.meters/1000,"km")));
        if(s.contains("times"))l.addView(text("Gesamtdauer: "+duration(r.end-r.start)+"\nFahrzeit: "+duration(r.moving)+"\nStandzeit: "+duration(r.standing)));
        if(s.contains("speed"))l.addView(text("Durchschnitt in Bewegung: "+format(r.moving>0?r.meters/r.moving*3600:Double.NaN,"km/h")+"\nMaximum: "+format(r.maxSpeed,"km/h")));
        if(s.contains("energy"))l.addView(text("Energie: "+format(r.energy,"Wh")+"\nVerbrauch: "+format(r.meters>0?r.energy/(r.meters/1000):Double.NaN,"Wh/km")));
        if(s.contains("power"))l.addView(text("Mittlere elektrische Leistung: "+format(r.averagePower,"W")+"\nMaximale Leistung: "+format(r.maxPower,"W")));
        if(s.contains("altitude"))l.addView(text("Anstieg: "+format(r.ascent,"m")));
        if(s.contains("temperature"))l.addView(text("Ø Außentemperatur: "+format(r.temperature("outside_temperature_c"),"°C")+"\nØ "+prefs.getString("temp1_label","Temp1")+": "+format(r.temperature("temp1_c"),"°C")+"\nØ "+prefs.getString("temp2_label","Temp2")+": "+format(r.temperature("temp2_c"),"°C")));
        String[] series={"speed_chart","power_chart","altitude_chart"},units={"km/h","W","m"},titles={"Geschwindigkeit","Elektrische Leistung","GPS-Höhe"};
        for(int i=0;i<series.length;i++)if(s.contains(series[i])){List<double[]> data=new ArrayList<>();for(TripJournal.Point p:r.points)data.add(new double[]{Math.max(0,(p.time-r.start)/1000.0),i==0?p.speed:i==1?p.power:p.altitude});l.addView(text(titles[i]+" · Zeit seit Fahrtbeginn"));l.addView(new JournalChart(activity,data,units[i]),new LinearLayout.LayoutParams(-1,dp(180)));}
        if(s.isEmpty())l.addView(text("Wähle die Werte aus, die du sehen möchtest."));
        current[0]=dialog("Fahrtstatistiken",l);
    }
    private void overview(List<TripJournal.Ride> rides){
        LinearLayout l=column();Set<String> s=selected();AlertDialog[] current=new AlertDialog[1];
        l.addView(button("Angezeigte Werte auswählen",()->chooseStats(()->{current[0].dismiss();overview(rides);})));l.addView(text(rides.size()+" Fahrten · letzte "+days+" Tage"));
        double meters=0,energy=0,maxSpeed=0,maxPower=0,ascent=0;long elapsed=0,moving=0,standing=0;boolean allEnergy=true,allTimes=true;
        Map<String,Double> byDay=new TreeMap<>();
        for(TripJournal.Ride r:rides){meters+=r.meters;elapsed+=Math.max(0,r.end-r.start);if(Double.isFinite(r.energy))energy+=r.energy;else allEnergy=false;
            if(r.moving>=0&&r.standing>=0){moving+=r.moving;standing+=r.standing;}else allTimes=false;
            if(Double.isFinite(r.maxSpeed))maxSpeed=Math.max(maxSpeed,r.maxSpeed);if(Double.isFinite(r.maxPower))maxPower=Math.max(maxPower,r.maxPower);if(Double.isFinite(r.ascent))ascent+=r.ascent;
            String day=new SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(new Date(r.start));byDay.put(day,byDay.getOrDefault(day,0.0)+r.meters/1000);
        }
        if(s.contains("distance"))l.addView(text("Gesamtstrecke: "+format(meters/1000,"km")));
        if(s.contains("times"))l.addView(text("Gesamtdauer: "+duration(elapsed)+"\nFahrzeit: "+duration(allTimes?moving:-1)+"\nStandzeit: "+duration(allTimes?standing:-1)));
        if(s.contains("energy"))l.addView(text("Gesamtenergie: "+format(allEnergy?energy:Double.NaN,"Wh")+"\nVerbrauch: "+format(allEnergy&&meters>0?energy/(meters/1000):Double.NaN,"Wh/km")));
        if(s.contains("speed"))l.addView(text("Durchschnitt in Bewegung: "+format(allTimes&&moving>0?meters/moving*3600:Double.NaN,"km/h")+"\nHöchste Geschwindigkeit: "+format(rides.isEmpty()?Double.NaN:maxSpeed,"km/h")));
        if(s.contains("power"))l.addView(text("Höchste elektrische Leistung: "+format(rides.isEmpty()?Double.NaN:maxPower,"W")));
        if(s.contains("altitude"))l.addView(text("Gesamter Anstieg: "+format(rides.isEmpty()?Double.NaN:ascent,"m")));
        if(s.contains("distance")){l.addView(text("Kilometer pro Tag"));double max=1;for(double km:byDay.values())max=Math.max(max,km);
            for(Map.Entry<String,Double> day:byDay.entrySet()){l.addView(text(day.getKey()+" · "+format(day.getValue(),"km")));ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress((int)(day.getValue()/max*1000));l.addView(bar);}}
        l.addView(text("Fahrten vergleichen"));for(TripJournal.Ride r:rides){String comparison=date(r.start);
            if(s.contains("distance"))comparison+=" · "+format(r.meters/1000,"km");if(s.contains("times"))comparison+=" · "+duration(r.end-r.start);
            if(s.contains("energy"))comparison+=" · "+format(r.meters>0?r.energy/(r.meters/1000):Double.NaN,"Wh/km");if(s.contains("speed"))comparison+=" · max. "+format(r.maxSpeed,"km/h");
            l.addView(button(comparison,()->openRide(r)));}
        current[0]=dialog("Zeitraumauswertung",l);
    }
    private void map(TripJournal.Ride ride){
        LinearLayout l=column();l.addView(text("Kartenhintergrund: OpenStreetMap · Internet nötig. Sichtbare Kartenausschnitte werden angefragt; die Fahrtdatei bleibt auf deinem Handy."));
        RouteMap map=new RouteMap(activity,ride.points);l.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(activity);controls.addView(button("−",()->map.changeZoom(-1)),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("Route",map::fit),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("+",()->map.changeZoom(1)),new LinearLayout.LayoutParams(0,-2,1));l.addView(controls);
        TextView attribution=text("© OpenStreetMap contributors · Grün: Start · Rot: Ziel");attribution.setOnClickListener(v->activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright"))));l.addView(attribution);
        AlertDialog d=new AlertDialog.Builder(activity).setTitle("Fahrtroute · "+date(ride.start)).setView(l).setNegativeButton("Zurück",null).create();d.show();d.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.88));
    }
    private void share(TripJournal.Ride ride){
        try{ArrayList<Uri> files=new ArrayList<>();for(java.io.File f:new java.io.File[]{ride.gpx,ride.csv})if(f.isFile())files.add(FileProvider.getUriForFile(activity,activity.getPackageName()+".files",f));
            if(files.isEmpty())throw new Exception("Keine Fahrtdateien vorhanden");Intent intent=new Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream").putParcelableArrayListExtra(Intent.EXTRA_STREAM,files).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);activity.startActivity(Intent.createChooser(intent,"Fahrt teilen"));
        }catch(Exception e){Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}

