package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.location.Location;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class TripRecorder {
    static final class Summary {
        final long startedAt;
        final long endedAt;
        final double distanceMeters;
        final double maxSpeedKmh;
        final double ascentMeters;
        final double energyWh;
        final File gpxFile;
        final File csvFile;
        final File metadataFile;

        Summary(long startedAt, long endedAt, double distanceMeters, double maxSpeedKmh,
                double ascentMeters, double energyWh, File gpxFile, File csvFile) {
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.distanceMeters = distanceMeters;
            this.maxSpeedKmh = maxSpeedKmh;
            this.ascentMeters = ascentMeters;
            this.energyWh = energyWh;
            this.gpxFile = gpxFile;
            this.csvFile = csvFile;
            this.metadataFile=new File(csvFile.getParentFile(),csvFile.getName().replace(".csv",".json"));
        }
    }

    private final long startedAt;
    private final String scooterId,scooterName;
    private final File gpxFile;
    private final File csvFile;
    private BufferedWriter gpx;
    private BufferedWriter csv;

    TripRecorder(Context context, long startedAt) throws IOException {
        this.startedAt = startedAt;
        android.content.SharedPreferences prefs=context.getSharedPreferences("settings",0);
        scooterId=prefs.getString(ScooterProfiles.ACTIVE,"legacy");scooterName=ScooterProfiles.name(prefs);
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (root == null) root = new File(context.getFilesDir(), "trips");
        File directory = new File(new File(root, "ScootPit-BMS"),scooterId);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Fahrtenordner konnte nicht angelegt werden");
        }

        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
                .format(new Date(startedAt));
        gpxFile = new File(directory, "ScootPit_" + stamp+"_"+startedAt + ".gpx");
        csvFile = new File(directory, "ScootPit_" + stamp+"_"+startedAt + ".csv");
        gpx = new BufferedWriter(new FileWriter(gpxFile, false));
        csv = new BufferedWriter(new FileWriter(csvFile, false));

        gpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        gpx.write("<gpx version=\"1.1\" creator=\"ScootPit BMS\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        gpx.write("<trk><name>" + xml(scooterName)+" " + xml(localTime(startedAt)) + "</name><trkseg>\n");
        csv.write("Zeit;Breite;Laenge;Genauigkeit_m;Geschwindigkeit_kmh;Hoehe_m;BMS_Prozent;Spannung_V;Strom_A;Entnahme_W;Energie_Wh;Maximale_Fahrtleistung_W;Aussentemperatur_C;Temp1_C;Temp2_C\n");
    }

    synchronized void add(Location location, int soc, double voltage, double current,
                          double watts, double energyWh, double maxPowerW, double outside, double[] sensors) {
        if (gpx == null || csv == null) return;
        try {
            String timestamp = isoUtc(location.getTime());
            double speed = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
            double altitude = location.hasAltitude() ? location.getAltitude() : Double.NaN;
            double accuracy = location.hasAccuracy() ? location.getAccuracy() : 0;

            gpx.write(String.format(Locale.US,
                    "<trkpt lat=\"%.7f\" lon=\"%.7f\">%s<time>%s</time></trkpt>\n",
                    location.getLatitude(), location.getLongitude(), Double.isFinite(altitude)?String.format(Locale.US,"<ele>%.2f</ele>",altitude):"", timestamp));
            csv.write(String.format(Locale.GERMANY,
                    "%s;%.7f;%.7f;%.1f;%.2f;%.2f;%d;%.2f;%.2f;%.1f;%.3f;%.1f;%s;%s;%s%n",
                    timestamp, location.getLatitude(), location.getLongitude(), accuracy,
                    speed, altitude, soc, voltage, current, watts, energyWh, maxPowerW,temperature(outside),temperature(sensors.length>0?sensors[0]:Double.NaN),temperature(sensors.length>1?sensors[1]:Double.NaN)));
            gpx.flush();
            csv.flush();
        } catch (IOException ignored) {}
    }

    synchronized Summary finish(long endedAt, double distanceMeters, double maxSpeedKmh,
                                double ascentMeters, double energyWh, double maxPowerW, double outside,double temp1,double temp2,long movingMs,long standingMs) {
        try {
            if (gpx != null) {
                gpx.write("</trkseg></trk></gpx>\n");
                gpx.close();
            }
        } catch (IOException ignored) {}
        try { if (csv != null) csv.close(); } catch (IOException ignored) {}
        gpx = null;
        csv = null;
        Summary summary=new Summary(startedAt, endedAt, distanceMeters, maxSpeedKmh, ascentMeters, energyWh, gpxFile, csvFile);
        try(BufferedWriter metadata=new BufferedWriter(new FileWriter(summary.metadataFile))){metadata.write(new org.json.JSONObject().put("scooter_id",scooterId).put("scooter_name",scooterName).put("started_at",startedAt).put("ended_at",endedAt).put("moving_ms",movingMs).put("standing_ms",standingMs).put("distance_m",distanceMeters).put("max_speed_kmh",maxSpeedKmh).put("ascent_m",ascentMeters).put("energy_wh",energyWh).put("max_power_w",maxPowerW).put("outside_temperature_c",Double.isFinite(outside)?outside:org.json.JSONObject.NULL).put("temp1_c",Double.isFinite(temp1)?temp1:org.json.JSONObject.NULL).put("temp2_c",Double.isFinite(temp2)?temp2:org.json.JSONObject.NULL).toString(2));}catch(Exception e){android.util.Log.e("Joyor","Fahrtzusammenfassung konnte nicht gespeichert werden",e);}
        return summary;
    }

    private static String temperature(double value){return Double.isFinite(value)?String.format(Locale.GERMANY,"%.2f",value):"";}
    private static String isoUtc(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(time));
    }

    private static String localTime(long time) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(new Date(time));
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

