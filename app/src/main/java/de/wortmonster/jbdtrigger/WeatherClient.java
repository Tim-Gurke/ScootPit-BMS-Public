package de.wortmonster.jbdtrigger;

import android.os.Handler;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class WeatherClient {
    interface Callback { void received(double temperature, long retrievedAt); }
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler;
    private long lastAttempt, lastSuccess;
    private boolean busy;
    private volatile boolean closed;
    WeatherClient(Handler handler) { this.handler = handler; }
    void update(double lat, double lon, Callback callback) {
        update(lat,lon,false,callback);
    }
    void update(double lat,double lon,boolean force,Callback callback) {
        long now = System.currentTimeMillis();
        if(closed || busy || (!force&&(now-lastAttempt<300_000 || now-lastSuccess<900_000)))return;
        busy=true; lastAttempt=now;
        worker.execute(() -> {
            double value=Double.NaN;
            HttpURLConnection connection=null;
            try {
                URL url=new URL(String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m",lat,lon));
                connection=(HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                try(InputStream in=connection.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] buf=new byte[4096]; int n;
                    while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>100_000)throw new java.io.IOException("Response too large");}
                    value=new JSONObject(out.toString(StandardCharsets.UTF_8.name())).getJSONObject("current").getDouble("temperature_2m");
                }
            } catch(Exception ignored) {} finally { if(connection!=null)connection.disconnect(); }
            final double result=value;
            handler.post(() -> { busy=false; if(closed)return;
                if(Double.isFinite(result)){lastSuccess=System.currentTimeMillis(); callback.received(result,lastSuccess);}
            });
        });
    }
    void close(){closed=true;worker.shutdownNow();}
}
