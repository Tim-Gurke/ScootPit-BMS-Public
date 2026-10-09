package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Restores complete ScootPit ride folders locally, assigning the rides to the active scooter. */
final class TripFolderImporter {
    private static final int MAX_FILES=5000,MAX_DEPTH=8,MAX_FILE_BYTES=32*1024*1024;
    private static final long MAX_TOTAL_BYTES=512L*1024*1024;
    static final class Summary {
        final int found,imported,duplicates,skipped;
        Summary(int found,int imported,int duplicates,int skipped){this.found=found;this.imported=imported;this.duplicates=duplicates;this.skipped=skipped;}
        String message(){return imported+" Fahrten importiert · "+duplicates+" bereits vorhanden · "+skipped+" unvollständig übersprungen.";}
    }
    private static final class Entry {final String path,name,id,mime;Entry(String p,String n,String i,String m){path=p;name=n;id=i;mime=m;}}
    private static final class RideFile {final Entry metadata,csv,gpx;final JSONObject data;RideFile(Entry m,Entry c,Entry g,JSONObject d){metadata=m;csv=c;gpx=g;data=d;}}
    private TripFolderImporter(){}

    static Summary importFolder(Context context,SharedPreferences prefs,Uri tree)throws Exception{
        String scooterId=prefs.getString(ScooterProfiles.ACTIVE,"legacy"),scooterName=ScooterProfiles.name(prefs);
        List<Entry> entries=new ArrayList<>();int[] seen={0};String rootId=DocumentsContract.getTreeDocumentId(tree);scan(context,tree,rootId,"",0,entries,seen);
        Map<String,Entry> byPath=new HashMap<>();for(Entry e:entries)byPath.put(e.path,e);
        List<RideFile> candidates=new ArrayList<>();int skipped=0;long[] budget={0};
        for(Entry e:entries){if(!e.name.toLowerCase(Locale.ROOT).endsWith(".json"))continue;
            JSONObject metadata;try{metadata=new JSONObject(read(context,tree,e.id,1024*1024,budget));}catch(Exception invalid){continue;}
            if(!metadata.has("started_at")||!metadata.has("ended_at")||!metadata.has("distance_m"))continue;
            String stem=e.name.substring(0,e.name.length()-5),base=parent(e.path);Entry csv=byPath.get(join(base,stem+".csv")),gpx=byPath.get(join(base,stem+".gpx"));
            if(csv==null){skipped++;continue;}
            long start=metadata.optLong("started_at",0),end=metadata.optLong("ended_at",0);double distance=metadata.optDouble("distance_m",Double.NaN);
            if(start<=0||end<start||!Double.isFinite(distance)||distance<0){skipped++;continue;}
            candidates.add(new RideFile(e,csv,gpx,metadata));
        }
        int found=candidates.size();if(found==0)throw new IOException(entries.isEmpty()?"Der ausgewählte Ordner ist leer.":"Keine vollständigen ScootPit-Fahrten gefunden. Benötigt werden die JSON- und CSV-Dateien pro Fahrt.");
        File destination=TripImporter.folder(context,scooterId);if(!destination.exists()&&!destination.mkdirs())throw new IOException("Fahrtenordner konnte nicht angelegt werden.");
        Set<String> fingerprints=new HashSet<>();Set<String> occupied=new HashSet<>();File[] existing=destination.listFiles();
        if(existing!=null)for(File f:existing){occupied.add(f.getName());if(!f.getName().endsWith(".json"))continue;try{
            JSONObject m=new JSONObject(new String(read(f,1024*1024),StandardCharsets.UTF_8));File csv=new File(destination,f.getName().substring(0,f.getName().length()-5)+".csv");if(csv.isFile())fingerprints.add(fingerprint(m,hash(csv)));
        }catch(Exception ignored){}}
        int imported=0,duplicates=0;
        for(RideFile ride:candidates){
            byte[] csvBytes=read(context,tree,ride.csv.id,MAX_FILE_BYTES,budget);String header=firstLine(csvBytes);
            if(!header.startsWith("Zeit;")&&!header.toLowerCase(Locale.ROOT).startsWith("time;")){skipped++;continue;}
            String csvHash=sha256(csvBytes),fingerprint=fingerprint(ride.data,csvHash);if(fingerprints.contains(fingerprint)){duplicates++;continue;}
            byte[] gpxBytes=ride.gpx==null?null:read(context,tree,ride.gpx.id,MAX_FILE_BYTES,budget);
            String original=ride.metadata.name.substring(0,ride.metadata.name.length()-5).replaceAll("[^A-Za-z0-9._-]","_");if(original.length()>100)original=original.substring(0,100);if(original.isEmpty())original="ScootPit_import_"+ride.data.optLong("started_at");
            String stem=original;for(int suffix=2;occupied.contains(stem+".json")||occupied.contains(stem+".csv")||occupied.contains(stem+".gpx");suffix++)stem=original+"_"+suffix;
            ride.data.put("scooter_id",scooterId).put("scooter_name",scooterName);
            File csvFile=new File(destination,stem+".csv"),gpxFile=new File(destination,stem+".gpx"),jsonFile=new File(destination,stem+".json");
            try{
                writeNew(csvFile,csvBytes);if(gpxBytes!=null)writeNew(gpxFile,gpxBytes);writeNew(jsonFile,ride.data.toString(2).getBytes(StandardCharsets.UTF_8));
            }catch(Exception error){csvFile.delete();gpxFile.delete();jsonFile.delete();throw error;}
            occupied.add(stem+".csv");occupied.add(stem+".json");if(gpxBytes!=null)occupied.add(stem+".gpx");fingerprints.add(fingerprint);imported++;
        }
        return new Summary(found,imported,duplicates,skipped);
    }

    private static void scan(Context c,Uri tree,String parentId,String parentPath,int depth,List<Entry> out,int[] seen)throws Exception{
        if(depth>MAX_DEPTH)throw new IOException("Der ausgewählte Ordner ist zu tief verschachtelt.");
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parentId);
        try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cursor==null)throw new IOException("Der ausgewählte Ordner ist nicht lesbar.");
            while(cursor.moveToNext()){
                String id=cursor.getString(0),name=cursor.getString(1),mime=cursor.getString(2);if(id==null||name==null||name.equals(".")||name.equals(".."))continue;
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))scan(c,tree,id,join(parentPath,name),depth+1,out,seen);
                else{if(++seen[0]>MAX_FILES)throw new IOException("Der Ordner enthält mehr als "+MAX_FILES+" Dateien.");out.add(new Entry(join(parentPath,name),name,id,mime));}
            }
        }
    }
    private static String join(String parent,String name){return parent.isEmpty()?name:parent+"/"+name;}
    private static String parent(String path){int at=path.lastIndexOf('/');return at<0?"":path.substring(0,at);}
    private static String firstLine(byte[] data){int end=0;while(end<data.length&&data[end]!='\n'&&data[end]!='\r')end++;return new String(data,0,end,StandardCharsets.UTF_8).replace("\ufeff","");}
    private static byte[] read(Context c,Uri tree,String id,int max,long[] budget)throws Exception{
        try(InputStream in=c.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree,id));ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("Datei im gewählten Ordner ist nicht lesbar.");byte[] buffer=new byte[8192];int n,total=0;
            while((n=in.read(buffer))>=0){total+=n;if(total>max)throw new IOException("Eine Fahrtenbuchdatei ist größer als 32 MB.");if(budget!=null&&((budget[0]+=n)>MAX_TOTAL_BYTES))throw new IOException("Der Fahrtenbuchimport überschreitet 512 MB.");out.write(buffer,0,n);}return out.toByteArray();
        }
    }
    private static byte[] read(File file,int max)throws Exception{
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))>=0){total+=n;if(total>max)throw new IOException("Metadatendatei zu groß");out.write(buffer,0,n);}return out.toByteArray();
        }
    }
    private static String hash(File file)throws Exception{try(InputStream in=new FileInputStream(file)){MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)d.update(b,0,n);return hex(d.digest());}}
    private static String sha256(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    private static String fingerprint(JSONObject metadata,String csvHash){return metadata.optLong("started_at")+":"+metadata.optLong("ended_at")+":"+csvHash;}
    private static void writeNew(File file,byte[] bytes)throws IOException{
        File temp=new File(file.getParentFile(),file.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}
        if(file.exists()||!temp.renameTo(file)){temp.delete();throw new IOException("Fahrtdatei konnte nicht gespeichert werden: "+file.getName());}
    }
}
