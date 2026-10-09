package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Reads local completed recordings. No location or ride data is sent to a server. */
final class TripJournal {
    static final class Point {
        long time;double lat,lon,speed,altitude,power;
        Point(long t,double a,double o,double s,double h,double w){time=t;lat=a;lon=o;speed=s;altitude=h;power=w;}
    }
    static final class Ride {
        JSONObject metadata;File csv,gpx;long start,end,moving,standing;double meters,energy,maxSpeed,maxPower,ascent;
        final List<Point> points=new ArrayList<>();
        double averagePower=Double.NaN;
        double temperature(String key){return metadata.optDouble(key,Double.NaN);}
    }
    static List<Ride> list(Context c,SharedPreferences prefs,int days)throws IOException{
        List<Ride> result=new ArrayList<>();
        File root=c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if(root==null)root=new File(c.getFilesDir(),"trips");
        File folder=new File(new File(root,"ScootPit-BMS"),prefs.getString(ScooterProfiles.ACTIVE,"legacy"));
        File[] files=folder.listFiles((d,n)->n.endsWith(".json"));if(files==null)return result;
        long from=Long.MIN_VALUE;if(days>0){Calendar cutoff=Calendar.getInstance();cutoff.add(Calendar.DAY_OF_YEAR,-days);from=cutoff.getTimeInMillis();}
        for(File file:files){
            if(file.length()>1000000)continue;
            try{JSONObject m=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
                if(days>0&&m.optLong("started_at",0)<from)continue;
                Ride ride=new Ride();ride.metadata=m;ride.start=m.getLong("started_at");ride.end=m.getLong("ended_at");
                ride.meters=m.optDouble("distance_m",0);ride.energy=m.optDouble("energy_wh",Double.NaN);
                ride.maxSpeed=m.optDouble("max_speed_kmh",Double.NaN);ride.ascent=m.optDouble("ascent_m",Double.NaN);
                ride.maxPower=m.optDouble("max_power_w",Double.NaN);ride.moving=m.optLong("moving_ms",-1);ride.standing=m.optLong("standing_ms",-1);
                String stem=file.getName().substring(0,file.getName().length()-5);
                ride.csv=new File(folder,stem+".csv");ride.gpx=new File(folder,stem+".gpx");result.add(ride);
            }catch(Exception e){android.util.Log.w("ScootPit","Unlesbare Fahrt: "+file.getName(),e);}
        }
        result.sort((a,b)->Long.compare(b.start,a.start));return result;
    }
    static void loadPoints(Ride r)throws IOException{
        if(!r.points.isEmpty())return;
        if(!r.csv.isFile())throw new IOException("Die lokale CSV-Datei dieser Fahrt fehlt.");
        long previous=0,inferredMoving=0,inferredStanding=0;double previousPower=Double.NaN,powerIntegral=0,powerTime=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(r.csv),StandardCharsets.UTF_8))){
            String line=reader.readLine();if(line==null||!line.startsWith("Zeit;"))throw new IOException("Unbekanntes Fahrtformat");
            int stride=1,row=0;
            while((line=reader.readLine())!=null){String[] cells=line.split(";",-1);if(cells.length<10)continue;
                try{long time=Instant.parse(cells[0]).toEpochMilli();double lat=number(cells[1]),lon=number(cells[2]),speed=number(cells[4]),altitude=number(cells[5]),power=number(cells[9]);
                    if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;
                    long delta=previous>0?Math.max(0,Math.min(10000,time-previous)):0;
                    if(speed>=2)inferredMoving+=delta;else inferredStanding+=delta;
                    if(Double.isFinite(previousPower)&&delta>0){powerIntegral+=previousPower*delta;powerTime+=delta;}
                    previous=time;previousPower=power;
                    if(row++%stride==0)r.points.add(new Point(time,lat,lon,speed,altitude,power));
                    if(r.points.size()>20000){for(int i=r.points.size()-2;i>=0;i-=2)r.points.remove(i);stride*=2;}
                }catch(Exception ignored){}
            }
        }
        if(r.moving<0)r.moving=inferredMoving;if(r.standing<0)r.standing=inferredStanding;
        if(powerTime>0)r.averagePower=powerIntegral/powerTime;
    }
    static double number(String value){try{return Double.parseDouble(value.replace(',','.'));}catch(Exception e){return Double.NaN;}}
}

