package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Xml;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Imports route files shared from ScootPit without sending them off device. */
final class TripImporter {
    private static final class Point {
        long time;double lat,lon,speed=Double.NaN,alt=Double.NaN,power=Double.NaN,energy=Double.NaN,maxPower=Double.NaN,outside=Double.NaN,temp1=Double.NaN,temp2=Double.NaN;
        Point(long time,double lat,double lon){this.time=time;this.lat=lat;this.lon=lon;}
    }
    private TripImporter(){}

    static void importFile(Context context,SharedPreferences prefs,Uri uri,String displayName)throws Exception{
        byte[] bytes=read(context,uri);String name=displayName==null?"":displayName.toLowerCase(Locale.ROOT);
        String mime=context.getContentResolver().getType(uri);boolean csv=name.endsWith(".csv")||mime!=null&&mime.toLowerCase(Locale.ROOT).contains("csv");
        List<Point> points=csv?readCsv(new String(bytes,StandardCharsets.UTF_8)):readGpx(bytes);
        if(points.size()<2)throw new IOException("Die Datei enthält weniger als zwei gültige GPS-Punkte.");
        points.sort(Comparator.comparingLong(a->a.time));
        String digest=sha256(bytes);File folder=folder(context,prefs);if(!folder.exists()&&!folder.mkdirs())throw new IOException("Fahrtenordner konnte nicht angelegt werden.");
        File[] old=folder.listFiles((dir,file)->file.endsWith(".json"));if(old!=null)for(File f:old)try{if(digest.equals(new JSONObject(new String(read(f),StandardCharsets.UTF_8)).optString("import_source_sha256")))throw new IOException("Diese Fahrt wurde bereits importiert.");}catch(IOException e){if(e.getMessage()!=null&&e.getMessage().contains("bereits importiert"))throw e;}catch(Exception ignored){}
        Stats stats=stats(points);String stamp=new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ROOT).format(new Date(stats.start));
        String stem="ScootPit_import_"+stamp+"_"+digest.substring(0,8);File csvFile=new File(folder,stem+".csv"),gpxFile=new File(folder,stem+".gpx"),jsonFile=new File(folder,stem+".json");
        String csvText=csvText(points);write(csvFile,csvText.getBytes(StandardCharsets.UTF_8));
        write(gpxFile,csv?gpxText(points).getBytes(StandardCharsets.UTF_8):bytes);
        JSONObject metadata=new JSONObject().put("scooter_id",prefs.getString(ScooterProfiles.ACTIVE,"legacy")).put("scooter_name",ScooterProfiles.name(prefs)).put("started_at",stats.start).put("ended_at",stats.end).put("moving_ms",stats.moving).put("standing_ms",stats.standing).put("distance_m",stats.distance).put("max_speed_kmh",safe(stats.maxSpeed)).put("ascent_m",safe(stats.ascent)).put("energy_wh",safe(stats.energy)).put("max_power_w",safe(stats.maxPower)).put("outside_temperature_c",safe(stats.averageOutside)).put("temp1_c",safe(stats.averageTemp1)).put("temp2_c",safe(stats.averageTemp2)).put("imported",true).put("import_source_sha256",digest);
        write(jsonFile,metadata.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    static File folder(Context c,SharedPreferences p){return folder(c,p.getString(ScooterProfiles.ACTIVE,"legacy"));}
    static File folder(Context c,String profileId){File root=c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);if(root==null)root=new File(c.getFilesDir(),"trips");return new File(new File(root,"ScootPit-BMS"),profileId);}
    private static byte[] read(Context c,Uri uri)throws Exception{try(InputStream in=c.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException("Datei nicht lesbar.");byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))>=0){total+=n;if(total>32*1024*1024)throw new IOException("Fahrtdatei ist größer als 32 MB.");out.write(b,0,n);}return out.toByteArray();}}
    private static byte[] read(File f)throws IOException{try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}}
    private static void write(File f,byte[] bytes)throws IOException{File temp=new File(f.getParentFile(),f.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}if(!temp.renameTo(f)){temp.delete();throw new IOException("Importdatei konnte nicht gespeichert werden.");}}
    private static String sha256(byte[] bytes)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    private static Object safe(double n){return Double.isFinite(n)?n:JSONObject.NULL;}
    private static int column(String[] h,String... names){for(int i=0;i<h.length;i++){String v=h[i].trim().toLowerCase(Locale.ROOT).replace("\ufeff","").replace(" ","").replace("_","");for(String n:names)if(v.equals(n))return i;}return -1;}
    private static List<Point> readCsv(String text)throws Exception{
        String[] lines=text.replace("\r","").split("\n");if(lines.length<3)throw new IOException("CSV enthält keine aufgezeichnete Fahrt.");String header=lines[0].replace("\ufeff","");char delimiter=header.chars().filter(ch->ch==';').count()>=header.chars().filter(ch->ch==',').count()?';':',';String[] h=splitRow(header,delimiter);
        int time=column(h,"zeit","time","timestamp","datetime"),lat=column(h,"breite","latitude","lat"),lon=column(h,"laenge","länge","longitude","lon","lng");
        if(time<0||lat<0||lon<0)throw new IOException("CSV-Spalten für Zeit, Breite und Länge fehlen.");
        int speed=column(h,"geschwindigkeit_kmh","speed_kmh","speed"),alt=column(h,"hoehe_m","höhe_m","altitude_m","altitude","elevation"),power=column(h,"entnahme_w","power_w","power"),energy=column(h,"energie_wh","energy_wh","energy"),maxPower=column(h,"maximale_fahrtleistung_w","max_power_w"),outside=column(h,"aussentemperatur_c","außentemperatur_c","outside_temperature_c"),temp1=column(h,"temp1_c"),temp2=column(h,"temp2_c");
        List<Point> out=new ArrayList<>();for(int i=1;i<lines.length&&out.size()<200000;i++){String[] c=splitRow(lines[i],delimiter);try{Point p=new Point(parseTime(value(c,time)),number(value(c,lat)),number(value(c,lon)));if(!valid(p))continue;p.speed=number(value(c,speed));p.alt=number(value(c,alt));p.power=number(value(c,power));p.energy=number(value(c,energy));p.maxPower=number(value(c,maxPower));p.outside=number(value(c,outside));p.temp1=number(value(c,temp1));p.temp2=number(value(c,temp2));out.add(p);}catch(Exception ignored){}}
        if(out.size()<2)throw new IOException("CSV enthält keine gültigen GPS-Punkte.");return out;
    }
    private static String[] splitRow(String row,char delimiter){List<String> cells=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;for(int i=0;i<row.length();i++){char ch=row.charAt(i);if(ch=='"'){if(quoted&&i+1<row.length()&&row.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;}else if(ch==delimiter&&!quoted){cells.add(cell.toString());cell.setLength(0);}else cell.append(ch);}cells.add(cell.toString());return cells.toArray(new String[0]);}
    private static String value(String[] cells,int index){return index>=0&&index<cells.length?cells[index].trim():"";}
    private static double number(String s){try{return Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private static long parseTime(String s)throws Exception{try{return Instant.parse(s).toEpochMilli();}catch(Exception ignored){}try{return OffsetDateTime.parse(s).toInstant().toEpochMilli();}catch(Exception ignored){}for(String pattern:new String[]{"yyyy-MM-dd HH:mm:ss","dd.MM.yyyy HH:mm:ss","yyyy-MM-dd'T'HH:mm:ss"})try{return LocalDateTime.parse(s,DateTimeFormatter.ofPattern(pattern,Locale.ROOT)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}catch(Exception ignored){}throw new IOException("Zeitstempel nicht lesbar: "+s);}
    private static List<Point> readGpx(byte[] bytes)throws Exception{
        List<Point> out=new ArrayList<>();XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);p.setInput(new ByteArrayInputStream(bytes),"UTF-8");Point active=null;int event;
        while((event=p.next())!=XmlPullParser.END_DOCUMENT&&out.size()<200000){if(event==XmlPullParser.START_TAG&&p.getName().equalsIgnoreCase("trkpt")){try{active=new Point(0,Double.parseDouble(p.getAttributeValue(null,"lat")),Double.parseDouble(p.getAttributeValue(null,"lon")));}catch(Exception e){active=null;}}
            else if(event==XmlPullParser.START_TAG&&active!=null&&(p.getName().equalsIgnoreCase("time")||p.getName().equalsIgnoreCase("ele"))){String tag=p.getName().toLowerCase(Locale.ROOT),v=p.nextText();if(tag.equals("time"))try{active.time=parseTime(v);}catch(Exception ignored){}else active.alt=number(v);}
            else if(event==XmlPullParser.END_TAG&&p.getName().equalsIgnoreCase("trkpt")&&active!=null){if(active.time>0&&valid(active))out.add(active);active=null;}}
        if(out.size()<2)throw new IOException("GPX enthält keine gültigen Trackpunkte mit Zeitangaben.");return out;
    }
    private static boolean valid(Point p){return p.time>0&&Double.isFinite(p.lat)&&Double.isFinite(p.lon)&&Math.abs(p.lat)<=90&&Math.abs(p.lon)<=180;}
    private static final class Stats {long start,end,moving,standing;double distance,maxSpeed,ascent,energy=Double.NaN,maxPower=Double.NaN,averageOutside=Double.NaN,averageTemp1=Double.NaN,averageTemp2=Double.NaN;}
    private static Stats stats(List<Point> p){Stats s=new Stats();s.start=p.get(0).time;s.end=p.get(p.size()-1).time;double ascent=0,previousAlt=Double.NaN,energy=Double.NaN,maxPower=Double.NaN;double[] sums=new double[3];int[] counts=new int[3];
        for(int i=0;i<p.size();i++){Point a=p.get(i);if(Double.isFinite(a.outside)){sums[0]+=a.outside;counts[0]++;}if(Double.isFinite(a.temp1)){sums[1]+=a.temp1;counts[1]++;}if(Double.isFinite(a.temp2)){sums[2]+=a.temp2;counts[2]++;}if(Double.isFinite(a.energy))energy=a.energy;if(Double.isFinite(a.maxPower))maxPower=Math.max(Double.isFinite(maxPower)?maxPower:0,a.maxPower);
            if(Double.isFinite(a.alt)){if(Double.isFinite(previousAlt)&&a.alt>previousAlt)ascent+=a.alt-previousAlt;previousAlt=a.alt;}
            if(i==0)continue;Point b=p.get(i-1);long dt=Math.max(0,Math.min(10000,a.time-b.time));double meters=distance(b.lat,b.lon,a.lat,a.lon);s.distance+=meters;double derived=dt>0?meters*3600/(dt):Double.NaN;if(!Double.isFinite(a.speed)||a.speed<0)a.speed=derived;s.maxSpeed=Math.max(s.maxSpeed,Double.isFinite(a.speed)?a.speed:0);if(a.speed>=2)s.moving+=dt;else s.standing+=dt;
        }
        s.ascent=ascent;s.energy=energy;s.maxPower=maxPower;s.averageOutside=counts[0]>0?sums[0]/counts[0]:Double.NaN;s.averageTemp1=counts[1]>0?sums[1]/counts[1]:Double.NaN;s.averageTemp2=counts[2]>0?sums[2]/counts[2]:Double.NaN;return s;
    }
    private static double distance(double lat1,double lon1,double lat2,double lon2){double r=6371000,dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);return r*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
    private static String csvText(List<Point> points){StringBuilder out=new StringBuilder("Zeit;Breite;Laenge;Genauigkeit_m;Geschwindigkeit_kmh;Hoehe_m;BMS_Prozent;Spannung_V;Strom_A;Entnahme_W;Energie_Wh;Maximale_Fahrtleistung_W;Aussentemperatur_C;Temp1_C;Temp2_C\n");for(Point p:points){out.append(Instant.ofEpochMilli(p.time)).append(';').append(String.format(Locale.US,"%.7f;%.7f;",p.lat,p.lon)).append(';').append(fmt(p.speed)).append(';').append(fmt(p.alt)).append(";;;;").append(fmt(p.power)).append(';').append(fmt(p.energy)).append(';').append(fmt(p.maxPower)).append(';').append(fmt(p.outside)).append(';').append(fmt(p.temp1)).append(';').append(fmt(p.temp2)).append('\n');}return out.toString();}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.GERMANY,"%.3f",v):"";}
    private static String gpxText(List<Point> points){StringBuilder out=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"ScootPit BMS\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Importierte Fahrt</name><trkseg>");for(Point p:points){out.append(String.format(Locale.US,"<trkpt lat=\"%.7f\" lon=\"%.7f\">",p.lat,p.lon));if(Double.isFinite(p.alt))out.append(String.format(Locale.US,"<ele>%.2f</ele>",p.alt));out.append("<time>").append(Instant.ofEpochMilli(p.time)).append("</time></trkpt>");}return out.append("</trkseg></trk></gpx>").toString();}
}
