package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Route is drawn locally; only visible background tiles are requested from OpenStreetMap. */
final class RouteMap extends View {
    private final List<TripJournal.Point> route;
    private final Paint paint=new Paint(3);
    private final Map<String,Bitmap> images=new LinkedHashMap<>();
    private final Set<String> pending=new HashSet<>();
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final File cache;
    private double cx,cy;private int zoom=14;
    private float lastX,lastY;
    private boolean fitted,closed;
    private final ScaleGestureDetector pinch;
    private String tileMessage="";
    RouteMap(Context c,List<TripJournal.Point> points){
        super(c);route=points;cache=new File(c.getCacheDir(),"map-tiles");cache.mkdirs();
        setContentDescription("Fahrtroute: Grün Start, Rot Ziel. Karte verschieben und zoomen.");
        pinch=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            float scale=1;
            @Override public boolean onScaleBegin(ScaleGestureDetector d){scale=1;return true;}
            @Override public boolean onScale(ScaleGestureDetector d){scale*=d.getScaleFactor();if(scale>1.4){changeZoom(1);scale=1;}else if(scale<.7){changeZoom(-1);scale=1;}return true;}
        });
    }
    static double x(double lon){return (lon+180)/360;}
    static double y(double lat){double r=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,lat)));return (1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2;}
    void changeZoom(int amount){zoom=Math.max(2,Math.min(18,zoom+amount));invalidate();}
    void fit(){fitted=false;invalidate();}
    private void fitRoute(){
        if(route.isEmpty()||getWidth()==0)return;
        double minX=1,maxX=0,minY=1,maxY=0;
        for(TripJournal.Point p:route){minX=Math.min(minX,x(p.lon));maxX=Math.max(maxX,x(p.lon));minY=Math.min(minY,y(p.lat));maxY=Math.max(maxY,y(p.lat));}
        cx=(minX+maxX)/2;cy=(minY+maxY)/2;zoom=18;
        while(zoom>2&&((maxX-minX)*size()>getWidth()-80||(maxY-minY)*size()>getHeight()-100))zoom--;
        fitted=true;
    }
    private double size(){return 256.0*(1<<zoom);}
    @Override protected void onDraw(Canvas canvas){
        canvas.drawColor(0xff17212a);if(!fitted)fitRoute();
        if(route.isEmpty()){paint.setColor(Color.WHITE);paint.setTextSize(16*getResources().getDisplayMetrics().density);canvas.drawText("Keine aufgezeichneten GPS-Punkte",16,35,paint);return;}
        double world=size(),originX=cx*world-getWidth()/2.0,originY=cy*world-getHeight()/2.0;
        int left=(int)Math.floor(originX/256),top=(int)Math.floor(originY/256),count=1<<zoom;
        for(int row=top;row<=(originY+getHeight())/256;row++)for(int col=left;col<=(originX+getWidth())/256;col++){
            if(row<0||row>=count)continue;int wrapped=((col%count)+count)%count;String key=zoom+"_"+wrapped+"_"+row;
            Bitmap bitmap=images.get(key);float sx=(float)(col*256-originX),sy=(float)(row*256-originY);
            if(bitmap!=null)canvas.drawBitmap(bitmap,null,new RectF(sx,sy,sx+256,sy+256),paint);
            else{paint.setColor(0xff26333e);paint.setStyle(Paint.Style.STROKE);canvas.drawRect(sx,sy,sx+256,sy+256,paint);request(key,zoom,wrapped,row);}
        }
        Path path=new Path();boolean first=true;
        for(TripJournal.Point p:route){float px=(float)(x(p.lon)*world-originX),py=(float)(y(p.lat)*world-originY);if(first)path.moveTo(px,py);else path.lineTo(px,py);first=false;}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5*getResources().getDisplayMetrics().density);paint.setColor(0xffffa000);canvas.drawPath(path,paint);
        dot(canvas,route.get(0),originX,originY,world,0xff159b5d,"S");dot(canvas,route.get(route.size()-1),originX,originY,world,0xffed3448,"Z");
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xdd101010);canvas.drawRect(0,getHeight()-32*getResources().getDisplayMetrics().density,getWidth(),getHeight(),paint);
        paint.setColor(Color.WHITE);paint.setTextSize(11*getResources().getDisplayMetrics().density);
        canvas.drawText("© OpenStreetMap contributors",8,getHeight()-10,paint);
        if(!tileMessage.isEmpty())canvas.drawText(tileMessage,8,20*getResources().getDisplayMetrics().density,paint);
    }
    private void dot(Canvas c,TripJournal.Point p,double ox,double oy,double world,int color,String label){
        float px=(float)(x(p.lon)*world-ox),py=(float)(y(p.lat)*world-oy),d=getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawCircle(px,py,12*d,paint);paint.setColor(Color.WHITE);paint.setTextSize(13*d);c.drawText(label,px-4*d,py+5*d,paint);
    }
    private void request(String key,int z,int x,int y){
        if(closed||pending.contains(key)||pending.size()>=24)return;pending.add(key);
        worker.execute(()->{
            Bitmap result=null;String error="";
            try{
                File file=new File(cache,key+".png");
                if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<7L*86400000)result=BitmapFactory.decodeFile(file.getPath());
                if(result==null){
                    HttpURLConnection connection=(HttpURLConnection)new URL("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png").openConnection();
                    connection.setRequestProperty("User-Agent","ScootPit-BMS/1.0.0 (https://github.com/Tim-Gurke/ScootPit-BMS-Public)");
                    connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
                    try{if(connection.getResponseCode()!=200)throw new IOException("Kartenhintergrund nicht verfügbar");
                        try(InputStream input=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                            byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))>=0){out.write(buffer,0,n);if(out.size()>2000000)throw new IOException("Kartendatei zu groß");}
                            byte[] bytes=out.toByteArray();result=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            if(result!=null){try(FileOutputStream output=new FileOutputStream(file)){output.write(bytes);}}
                        }
                    }finally{connection.disconnect();}
                }
            }catch(Exception e){error="Karte offline / nicht verfügbar · Route bleibt sichtbar";}
            Bitmap image=result;String message=error;
            post(()->{if(closed)return;pending.remove(key);if(image!=null){images.put(key,image);tileMessage="";while(images.size()>80)images.remove(images.keySet().iterator().next());}else{tileMessage=message;pending.add(key);}invalidate();});
        });
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        getParent().requestDisallowInterceptTouchEvent(true);pinch.onTouchEvent(event);
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){lastX=event.getX();lastY=event.getY();return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&!pinch.isInProgress()){cx-=(event.getX()-lastX)/size();cy=Math.max(0,Math.min(1,cy-(event.getY()-lastY)/size()));lastX=event.getX();lastY=event.getY();invalidate();}
        if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDetachedFromWindow(){closed=true;worker.shutdownNow();super.onDetachedFromWindow();}
}
