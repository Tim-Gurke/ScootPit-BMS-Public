package de.wortmonster.jbdtrigger;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

public class BmsMonitorService extends Service implements LocationListener {
    public static final String ACTION_NEAR = "de.wortmonster.jbdtrigger.BMS_NEAR";
    public static final String ACTION_FAR = "de.wortmonster.jbdtrigger.BMS_FAR";
    public static final String ACTION_ACTIVE = "de.wortmonster.jbdtrigger.DISCHARGE_ACTIVE";
    public static final String ACTION_IDLE = "de.wortmonster.jbdtrigger.DISCHARGE_IDLE";
    public static final String ACTION_TIMEOUT = "de.wortmonster.jbdtrigger.MONITOR_TIMEOUT";
    public static final String ACTION_STATUS = "de.wortmonster.jbdtrigger.STATUS";

    public static final String ACTION_END_TRIP = "de.wortmonster.jbdtrigger.END_TRIP";
    public static final String ACTION_REFRESH_TEMPERATURE = "de.wortmonster.jbdtrigger.REFRESH_TEMPERATURE";
    public static final String ACTION_SET_DISCHARGE = "de.wortmonster.jbdtrigger.SET_DISCHARGE";
    private PowerManager.WakeLock wakeLock;
    private long wakeRenewed, quietSince;
    private boolean chargeEnabled, dischargeEnabled, controlPending, controlAcknowledged, desiredDischarge, writeBusy;
    private String controlMessage = "Bereitschaft starten, um den BMS-Lastausgang zu lesen";
    static volatile Intent latestStatus;
    static volatile boolean running;
    static volatile BmsMonitorService liveService;
    private SharedPreferences prefs;
    private WeatherClient weather;
    private double remainingAh, fullAh;
    private final BmsExtras extras=new BmsExtras();
    private int optionalPoll;
    private double[] temperatures = new double[0];
    private double outsideTemperature = Double.NaN;
    private long weatherAt, movingMs, standingMs, lastClock, gpsAt;
    private long tripPausedAt;
    private long pauseEndMs=600_000, disconnectedPauseGraceMs=120_000, slowUnpoweredStopMs=15_000;
    private double pausedGpsSpeedKmh;
    private final RideResumeGate resumeGate=new RideResumeGate();
    private double pausedEnergyWh, pausedMaxPowerW;
    private boolean manualHold;
    private final java.io.ByteArrayOutputStream packetBuffer = new java.io.ByteArrayOutputStream();
    private double averageConsumption;
    private final Runnable clock = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            if(wakeLock!=null && (!wakeLock.isHeld() || now-wakeRenewed>300000)){wakeLock.acquire(600000);wakeRenewed=now;}
            if (recorder != null && tripPausedAt==0 && lastClock > 0) {
                long elapsed = Math.min(5000, Math.max(0, now - lastClock));
                if (now - gpsAt < 5000 && currentSpeedKmh >= 2) movingMs += elapsed;
                else standingMs += elapsed;
            }
            if(recorder!=null && tripPausedAt==0 && (motion.stopped(android.os.SystemClock.elapsedRealtime(),stopMs)||motion.walking(android.os.SystemClock.elapsedRealtime(),slowUnpoweredStopMs))){
                active=false;activeSince=0;idleSince=0;tripPausedAt=now;pausedGpsSpeedKmh=0;pausedEnergyWh=energyWh;pausedMaxPowerW=maxPowerW;motion.reset();resumeGate.reset();
                emit(ACTION_IDLE);setState("Fahrt pausiert – BMS-Verbindung bleibt aktiv");
            }
            if(recorder!=null && tripPausedAt>0 && ((gatt==null&&now-tripPausedAt>=disconnectedPauseGraceMs)||now-tripPausedAt>=pauseEndMs)){
                long endAt=tripPausedAt;active=false;activeSince=0;idleSince=0;
                finishTrip(endAt);if(gatt==null)scheduleScan(1000);setState("Fahrt nach längerer Pause beendet");
            }
            lastClock = now;
            sendStatus();
            handler.postDelayed(this, 1000);
        }
    };

    private int connectRssi = -75;
    private boolean autoSelection;
    private java.util.Map<String,String> knownBms=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,ConnectionPolicy.InitialSignal> signals=new java.util.HashMap<>();
    private final java.util.Map<String,Long> tried=new java.util.HashMap<>();
    private long stopMs=45000;
    private final RideMotion motion=new RideMotion();
    private long connectConfirmMs = 3000;
    private final ConnectionPolicy.InitialSignal initialSignal = new ConnectionPolicy.InitialSignal();
    private int departureRssi = -95;
    private long reconnectMs = 5_000, departureGraceMs = 30_000, missingSince;
    private double gpsMaxKmh = 45;
    private int weakRssi = -95;
    private static final long SCAN_WINDOW_MS = 10_000;
    private long absentDelayMs = 5_000;
    private long weakDelayMs = 30_000;
    private long goodDelayMs = 10_000;
    private static final long DEPARTURE_SCAN_MS = 3_000;
    private static final int FAR_CONFIRMATIONS = 3;
    private static final long BMS_POLL_MS = 1_000;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final byte[] BASIC_INFO = {
            (byte) 0xDD, (byte) 0xA5, 0x03, 0x00, (byte) 0xFF, (byte) 0xFD, 0x77
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private LocationManager locationManager;
    private TripRecorder recorder;

    private String address = "";
    private String stateText = "Bereit";
    private int rssi = -127;
    private int bestScanRssi = -127;
    private int soc = -1;
    private boolean scanRunning;
    private boolean near;
    private boolean active;
    private boolean departureMode;
    private int farConfirmations;

    private double activeAmps = 0.30;
    private long activeMs = 1_000;
    private long idleMs = 5_000;
    private long monitorTimeoutMs = 90_000;
    private long activeSince;
    private long idleSince;
    private long monitorStartedAt;
    private long lastPacket;
    private long lastPowerAt;

    private double voltage;
    private double current;
    private double dischargeWatts;
    private double energyWh,maxPowerW;
    private final RecentConsumption recentConsumption=new RecentConsumption();
    private String tripTree="";
    private double temperatureSum;private int temperatureSamples;
    private final double[] sensorSums=new double[2];private final int[] sensorSamples=new int[2];
    private org.json.JSONArray temperatureHistory=new org.json.JSONArray();
    private double distanceMeters;
    private double maxSpeedKmh;
    private double currentSpeedKmh;
    private double altitudeMeters;
    private double ascentMeters;
    private long tripStartedAt;
    private Location lastLocation;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        ScooterProfiles.install(prefs);
        weather = new WeatherClient(handler);
        outsideTemperature = number(prefs.getString("outside_temperature", "NaN"), Double.NaN);
        weatherAt = prefs.getLong("weather_at", 0);
        averageConsumption = number(prefs.getString("learned_wh_km", "0"), 0);
        try{temperatureHistory=new org.json.JSONArray(prefs.getString("temperature_history","[]"));}catch(Exception ignored){}
        running = true;liveService=this;
        locationManager = getSystemService(LocationManager.class);
        PowerManager power=getSystemService(PowerManager.class);
        wakeLock=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Joyor:Readiness");
        wakeLock.setReferenceCounted(false);
        createChannels();
        startForeground(11, serviceNotification("Adaptive BMS-Suche aktiv"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        autoSelection=prefs.getBoolean("auto_scooter",true);
        knownBms=ScooterProfiles.knownBms(prefs);
        if((!autoSelection&&!BluetoothAdapter.checkBluetoothAddress(prefs.getString("device_address",""))) || (autoSelection&&knownBms.isEmpty())){
            setState("Bitte zuerst ein BMS auswählen");stopSelf();return START_NOT_STICKY;
        }
        if(intent!=null&&ACTION_REFRESH_TEMPERATURE.equals(intent.getAction())){
            requestBasicInfo();
            if(prefs.getBoolean("weather_enabled",true)&&lastLocation!=null){
                weather.update(lastLocation.getLatitude(),lastLocation.getLongitude(),true,(temp,at)->{outsideTemperature=temp;weatherAt=at;prefs.edit().putString("outside_temperature",Double.toString(temp)).putLong("weather_at",at).apply();sendStatus();});
            }
            sendStatus();return START_STICKY;
        }
        if(intent!=null && ACTION_SET_DISCHARGE.equals(intent.getAction())){
            setDischarge(intent.getBooleanExtra("enabled",true));return START_STICKY;
        }
        if (intent != null && ACTION_END_TRIP.equals(intent.getAction())) {
            active = false; manualHold = true; activeSince = 0;
            tripPausedAt=0;
            finishTrip();
            closeGatt();
            near = false;
            scheduleScan(goodDelayMs);
            emit(ACTION_IDLE);
            setState("Fahrt manuell beendet – wartet auf Ruhe");
            return START_STICKY;
        }
        if (lastClock > 0) { sendStatus(); return START_STICKY; }
        lastClock = System.currentTimeMillis();
        SharedPreferences preferences = prefs;
        address = preferences.getString("device_address", "");
        loadProfileSettings();

        handler.removeCallbacksAndMessages(null);
        stopScan();
        closeGatt();
        departureMode = false;
        near = false;
        active = false;
        scheduleScan(0);
        handler.post(clock);
        return START_STICKY;
    }

    private void loadProfileSettings(){
        // Change former shipped defaults once; retain user-entered alternatives.
        if(!"3".equals(prefs.getString("connection_policy_version",""))){
            SharedPreferences.Editor e=prefs.edit().putString("connection_policy_version","3");
            if(number(prefs.getString("connect_rssi","-70"),-70)==-70)e.putString("connect_rssi","-75");
            if(number(prefs.getString("scan_absent","60"),60)==60)e.putString("scan_absent","5");
            if(number(prefs.getString("scan_good","5"),5)==5)e.putString("scan_good","2");
            if(number(prefs.getString("scan_pause","5"),5)==5)e.putString("scan_pause","2");
            if(number(prefs.getString("active_seconds","3"),3)==3)e.putString("active_seconds","1");
            e.apply();
        }
        address=prefs.getString("device_address","");
        activeAmps=setting("active_current",.30,.01,100);activeMs=(long)(setting("active_seconds",1,1,3600)*1000);
        idleMs=(long)(setting("idle_seconds",5,1,3600)*1000);
        monitorTimeoutMs=(long)(setting("monitor_timeout",90,1,3600)*1000);
        connectRssi=(int)setting("connect_rssi",-75,-110,-30);
        connectConfirmMs=(long)(setting("connect_confirm_seconds",3,1,15)*1000);
        departureRssi=(int)setting("departure_rssi",-95,-120,connectRssi);weakRssi=departureRssi;
        absentDelayMs=(long)(setting("scan_absent",5,5,3600)*1000);
        weakDelayMs=(long)(setting("scan_weak",15,5,3600)*1000);
        goodDelayMs=(long)(setting("scan_good",2,1,3600)*1000);
        reconnectMs=(long)(setting("scan_pause",2,1,60)*1000);
        departureGraceMs=(long)(setting("departure_seconds",30,10,600)*1000);
        stopMs=(long)(setting("stop_seconds",45,15,3600)*1000);
        slowUnpoweredStopMs=(long)(setting("slow_unpowered_stop_seconds",15,5,120)*1000);
        pauseEndMs=(long)(setting("pause_end_seconds",600,60,7200)*1000);
        disconnectedPauseGraceMs=(long)(setting("pause_disconnect_seconds",120,0,7200)*1000);
        gpsMaxKmh=setting("gps_max_kmh",45,10,150);
        averageConsumption=number(prefs.getString("learned_wh_km","0"),0);
        outsideTemperature=number(prefs.getString("outside_temperature","NaN"),Double.NaN);weatherAt=prefs.getLong("weather_at",0);
        try{temperatureHistory=new org.json.JSONArray(prefs.getString("temperature_history","[]"));}catch(Exception e){temperatureHistory=new org.json.JSONArray();}
    }

    private double number(String value, double fallback) {
        try { return Double.parseDouble(value.replace(',', '.')); }
        catch (Exception ignored) { return fallback; }
    }

    private double setting(String key,double fallback,double min,double max){
        double v=number(prefs.getString(key,Double.toString(fallback)),fallback);
        return Double.isFinite(v)?Math.max(min,Math.min(max,v)):fallback;
    }

    private long seconds(String value, double fallback) {
        return Math.max(1_000L, (long) (number(value, fallback) * 1_000));
    }

    private void scheduleScan(long delayMs) {
        handler.removeCallbacks(startScanRunnable);
        handler.postDelayed(startScanRunnable, delayMs);
        setState(delayMs == 0 ? "Suche BMS" : "Nächste Suche in " + delayMs / 1000 + " s");
    }

    private final Runnable startScanRunnable = this::startScan;

    private void startScan() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { scheduleScan(absentDelayMs); return; }
        if (scanRunning || gatt != null || (!autoSelection && address.isEmpty())) return;
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            scheduleScan(absentDelayMs);
            return;
        }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            scheduleScan(absentDelayMs);
            return;
        }

        bestScanRssi = -127;
        initialSignal.reset();signals.clear();
        java.util.List<ScanFilter> filters=new java.util.ArrayList<>();
        if(autoSelection && recorder==null){knownBms=ScooterProfiles.knownBms(prefs);for(String mac:knownBms.keySet())filters.add(new ScanFilter.Builder().setDeviceAddress(mac).build());}
        else if(BluetoothAdapter.checkBluetoothAddress(address))filters.add(new ScanFilter.Builder().setDeviceAddress(address).build());
        if(filters.isEmpty()){setState("Keine eindeutigen gespeicherten BMS-Profile");scheduleScan(absentDelayMs);return;}
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        try {
            scanner.startScan(filters, settings, scanCallback);
            scanRunning = true;
            handler.postDelayed(finishScanRunnable,
                    departureMode ? DEPARTURE_SCAN_MS : Math.max(SCAN_WINDOW_MS,connectConfirmMs+5000));
            setState(departureMode ? "Prüfe Entfernung" : "Suche BMS");
        } catch (SecurityException ignored) {
            scheduleScan(absentDelayMs);
        }
    }

    private final Runnable finishScanRunnable = this::finishScan;

    private void finishScan() {
        if (!scanRunning) return;
        stopScan();

        if (departureMode) {
            if (missingSince == 0) missingSince = System.currentTimeMillis();
            // A weak advertisement still proves the selected BMS is nearby.
            if (bestScanRssi >= departureRssi) missingSince = System.currentTimeMillis();
            if (ConnectionPolicy.departed(System.currentTimeMillis(),missingSince,departureGraceMs,bestScanRssi,departureRssi)) completeDeparture();
            else scheduleScan(reconnectMs);
            return;
        }

        long delay = bestScanRssi == -127 ? absentDelayMs
                : bestScanRssi < weakRssi ? weakDelayMs
                : bestScanRssi < connectRssi ? goodDelayMs
                : goodDelayMs;
        scheduleScan(delay);
    }

    private void stopScan() {
        handler.removeCallbacks(finishScanRunnable);
        if (scanner != null && scanRunning) {
            try { scanner.stopScan(scanCallback); } catch (SecurityException ignored) {}
        }
        scanRunning = false;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            handler.post(() -> processScanResult(result));
        }
        private void processScanResult(ScanResult result) {
            if (!running || !scanRunning) return;
            String mac=result.getDevice().getAddress().toUpperCase(Locale.ROOT);
            if(recorder!=null || !autoSelection){if(!address.equalsIgnoreCase(mac))return;}
            else if(!knownBms.containsKey(mac))return;
            long scanNow=android.os.SystemClock.elapsedRealtime();
            if(autoSelection&&recorder==null&&scanNow-tried.getOrDefault(mac,-30000L)<10000)return;
            int value = result.getRssi();
            bestScanRssi = Math.max(bestScanRssi, value);
            rssi = value;
            sendStatus();

            int candidateRssi=connectRssi;long candidateConfirm=connectConfirmMs;
            if(autoSelection&&recorder==null){org.json.JSONObject candidate=ScooterProfiles.settingsFor(prefs,knownBms.get(mac));
                candidateRssi=(int)number(candidate.optString("connect_rssi","-75"),-75);
                if(!"3".equals(candidate.optString("connection_policy_version"))&&candidateRssi==-70)candidateRssi=-75;
                candidateRssi=Math.max(-110,Math.min(-30,candidateRssi));candidateConfirm=(long)(Math.max(1,Math.min(15,number(candidate.optString("connect_confirm_seconds","3"),3)))*1000);
            }
            boolean accepted = recorder != null ? value >= departureRssi
                    : signals.computeIfAbsent(mac,k->new ConnectionPolicy.InitialSignal()).accept(scanNow,value,candidateRssi,candidateConfirm);
            if (accepted && gatt == null) {
                if(autoSelection && recorder==null){
                    String id=knownBms.get(mac);
                    try{if(!id.equals(prefs.getString(ScooterProfiles.ACTIVE,""))){
                        ScooterProfiles.switchForService(prefs,id);loadProfileSettings();
                        manualHold=false;motion.reset();lastLocation=null;gpsAt=0;extras.values.clear();extras.times.clear();soc=-1;voltage=current=dischargeWatts=0;
                        temperatures=new double[0];distanceMeters=energyWh=maxPowerW=maxSpeedKmh=0;movingMs=standingMs=0;
                    }}catch(Exception e){setState("Profil konnte nicht gewählt werden: "+e.getMessage());return;}
                    address=mac;tried.put(mac,scanNow);
                }
                stopScan();
                departureMode = false;
                farConfirmations = 0;
                if (!near) {
                    near = true;
                    emit(ACTION_NEAR);
                }
                connect(result.getDevice());
            }
        }

        @Override public void onScanFailed(int errorCode) {
            scanRunning = false;
            scheduleScan(absentDelayMs);
        }
    };

    private void connect(BluetoothDevice device) {
        setState("Verbinde mit BMS");
        try {
            gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            BluetoothGatt attempt=gatt;
            handler.postDelayed(()->{if(gatt==attempt && lastPacket==0)handleDisconnected();},15_000);
        } catch (SecurityException ignored) {
            gatt = null;
            scheduleScan(goodDelayMs);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            handler.post(() -> {
            if (gatt != bluetoothGatt || !running) return;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setState("BMS verbunden");
                try { bluetoothGatt.discoverServices(); }
                catch (SecurityException ignored) { handleDisconnected(); }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (gatt == bluetoothGatt) handleDisconnected();
                else try { bluetoothGatt.close(); } catch (SecurityException ignored) {}
            }
        
            });
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            handler.post(() -> {
            if (gatt != bluetoothGatt || !running) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleDisconnected();
                return;
            }
            BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
            if (service == null) {
                handleDisconnected();
                return;
            }
            BluetoothGattCharacteristic notify = service.getCharacteristic(NOTIFY_UUID);
            writeCharacteristic = service.getCharacteristic(WRITE_UUID);
            if (notify == null || writeCharacteristic == null) {
                handleDisconnected();
                return;
            }
            enableNotifications(bluetoothGatt, notify);
            monitorStartedAt = System.currentTimeMillis();
            startLocationTracking();
            activeSince = 0;
            idleSince = 0;
            farConfirmations = 0;
            handler.removeCallbacks(pollRunnable);
            handler.postDelayed(pollRunnable, 800);
            setState(recorder == null ? "Warte auf Stromentnahme" :
                    active ? "Fahrt läuft" : "Fahrt pausiert");
        
            });
        }

        @Override public void onCharacteristicChanged(BluetoothGatt bluetoothGatt,
                                                        BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            if (value != null) handler.post(() -> { if (gatt == bluetoothGatt && running) parse(value); });
        }

        @Override public void onCharacteristicChanged(BluetoothGatt bluetoothGatt,
                                                        BluetoothGattCharacteristic characteristic,
                                                        byte[] value) {
            handler.post(() -> { if (gatt == bluetoothGatt && running) parse(value); });
        }

        @Override public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, int status) {
            handler.post(()->{if(gatt!=bluetoothGatt || !running)return;writeBusy=false;
                if(controlPending && status!=BluetoothGatt.GATT_SUCCESS)controlFinished("BLE-Schreibfehler – Zustand nicht bestätigt");});
        }

        @Override public void onReadRemoteRssi(BluetoothGatt bluetoothGatt, int value, int status) {
            handler.post(() -> {
            if (gatt != bluetoothGatt || !running) return;
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            rssi = value;
            // RSSI alone never ends a trip while valid packets keep arriving.
            if (recorder != null && !active && value < departureRssi && lastPacket > 0
                    && System.currentTimeMillis() - lastPacket >= departureGraceMs) handleDisconnected();
            sendStatus();
        
            });
        }
    };

    private void handleDisconnected() {
        closeGatt();
        if (recorder != null) {
            if(active){active=false;emit(ACTION_IDLE);}
            if(missingSince==0)missingSince=System.currentTimeMillis();
            departureMode = true;
            farConfirmations = 0;
            scheduleScan(0);
        } else {
            scheduleScan(goodDelayMs);
        }
    }

    private void enableNotifications(BluetoothGatt bluetoothGatt,
                                     BluetoothGattCharacteristic characteristic) {
        try {
            bluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= 33) {
                    bluetoothGatt.writeDescriptor(descriptor,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(descriptor);
                }
            }
        } catch (SecurityException ignored) {}
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            if (gatt == null || writeCharacteristic == null) return;
            long now = System.currentTimeMillis();

            if(controlPending && !controlAcknowledged){handler.postDelayed(this,BMS_POLL_MS);return;}
            if(autoSelection && recorder==null && knownBms.size()>1 && monitorStartedAt>0 && now-monitorStartedAt>=6000 && activeSince==0){
                closeGatt();near=false;scheduleScan(1000);return;
            }
            if (recorder == null && monitorStartedAt > 0
                    && now - monitorStartedAt >= monitorTimeoutMs) {
                emit(ACTION_TIMEOUT);
                closeGatt();
                scheduleScan(goodDelayMs);
                return;
            }

            if (lastPacket > 0 && now - lastPacket > 8_000) {
                handleDisconnected();
                return;
            }
            if(lastPacket==0 && now-monitorStartedAt>15_000){handleDisconnected();return;}
            requestBasicInfo();
            handler.removeCallbacks(optionalRead);handler.postDelayed(optionalRead,550);
            handler.removeCallbacks(readRssiRunnable);
            handler.postDelayed(readRssiRunnable, 300);
            handler.postDelayed(this, BMS_POLL_MS);
        }
    };

    private final Runnable readRssiRunnable = () -> {
        if (gatt != null) {
            try { gatt.readRemoteRssi(); } catch (SecurityException ignored) {}
        }
    };

    private boolean write(byte[] value) {
        if(gatt==null || writeCharacteristic==null || writeBusy)return false;
        try {
            boolean accepted;
            if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeCharacteristic(writeCharacteristic,value,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)==android.bluetooth.BluetoothStatusCodes.SUCCESS;
            else{writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                writeCharacteristic.setValue(value);accepted=gatt.writeCharacteristic(writeCharacteristic);}
            writeBusy=accepted;return accepted;
        }catch(SecurityException e){return false;}
    }
    private final Runnable optionalRead=()->{
        if(gatt==null||writeCharacteristic==null||controlPending||lastPacket==0||System.currentTimeMillis()-lastPacket>8000)return;
        int step=optionalPoll++%30;int command=step==0?5:step==1?0xaa:4;
        write(BmsExtras.readCommand(command));
    };
    private void acceptExtras(byte[] frame){
        if(!extras.accept(frame,System.currentTimeMillis()))return;
        String found=new org.json.JSONArray(extras.values.keySet()).toString();
        if(!found.equals(prefs.getString("bms_available","")))prefs.edit().putString("bms_available",found).apply();
        sendStatus();
    }
    private void requestBasicInfo(){write(BASIC_INFO);}
    private void setDischarge(boolean enabled){
        if(controlPending)return;
        long now=System.currentTimeMillis();
        String error=JbdControl.rejection(gatt!=null && writeCharacteristic!=null && lastPacket>0 && now-lastPacket<4000,
            recorder!=null,chargeEnabled,enabled,current,quietSince>0?now-quietSince:0,currentSpeedKmh);
        if(error!=null){controlMessage=error;sendStatus();return;}
        desiredDischarge=enabled;controlPending=true;controlAcknowledged=false;
        controlMessage="BMS-Bestätigung wird geprüft …";sendStatus();
        handler.removeCallbacks(pollRunnable);handler.removeCallbacks(readRssiRunnable);
        handler.postDelayed(controlTimeout,10000);
        // Let any preceding read write/RSSI operation finish; retry only BEFORE submission.
        handler.postDelayed(submitControl,800);
    }
    private final Runnable submitControl=new Runnable(){public void run(){
        if(!controlPending)return;
        long now=System.currentTimeMillis();
        String error=JbdControl.rejection(gatt!=null && writeCharacteristic!=null && now-lastPacket<4000,
            recorder!=null,chargeEnabled,desiredDischarge,current,quietSince>0?now-quietSince:0,currentSpeedKmh);
        if(error!=null){controlFinished(error);return;}
        if(writeBusy){handler.postDelayed(this,100);return;}
        if(!write(JbdControl.discharge(desiredDischarge))){controlFinished("BMS-Befehl konnte nicht gesendet werden");return;}
        handler.postDelayed(pollRunnable,BMS_POLL_MS);
    }};
    private final Runnable controlTimeout=()->controlFinished("Keine Bestätigung – Lastausgang prüfen, erneut verbinden");
    private void controlFinished(String message){
        controlPending=false;controlAcknowledged=false;controlMessage=message;
        handler.removeCallbacks(controlTimeout);handler.removeCallbacks(submitControl);
        handler.removeCallbacks(pollRunnable);if(gatt!=null)handler.postDelayed(pollRunnable,500);
        sendStatus();
    }

    private void parse(byte[] fragment) {
        if (fragment == null) return;
        try { packetBuffer.write(fragment); } catch (java.io.IOException ignored) { return; }
        byte[] pending = packetBuffer.toByteArray();
        int cursor = 0;
        while (cursor < pending.length) {
            if ((pending[cursor] & 255) != 0xdd) { cursor++; continue; }
            if (pending.length - cursor < 4) break;
            int size = (pending[cursor + 3] & 255) + 7;
            if (pending.length - cursor < size) break;
            byte[] frame = java.util.Arrays.copyOfRange(pending, cursor, cursor + size);
            if(!JbdControl.validReply(frame)){cursor++;continue;}
            if((frame[1]&255)==0xe1 && controlPending && frame[3]==0){
                if(frame[2]==0){controlAcknowledged=true;handler.removeCallbacks(pollRunnable);handler.postDelayed(pollRunnable,250);}
                else controlFinished("BMS hat den Schaltbefehl abgelehnt ("+(frame[2]&255)+")");
            }
            BmsPacket packet = BmsPacket.decode(frame);
            if(packet!=null)acceptPacket(packet);
            acceptExtras(frame);
            cursor += size;
        }
        packetBuffer.reset();
        packetBuffer.write(pending, cursor, pending.length - cursor);
    }

    private void acceptPacket(BmsPacket packet) {
        long now = System.currentTimeMillis();
        if (recorder != null && lastPowerAt > 0 && now > lastPowerAt && now - lastPowerAt <= 8000) {
            energyWh += dischargeWatts * (now - lastPowerAt) / 3_600_000.0;
        }
        if(recorder!=null && lastPowerAt>0 && now-lastPowerAt>8000)recentConsumption.resetAt(distanceMeters,energyWh);
        lastPowerAt = now;
        voltage = packet.voltage;
        current = packet.current;
        remainingAh = packet.remainingAh;
        fullAh = packet.fullAh;
        temperatures = packet.temperatures;
        soc = packet.soc;
        dischargeWatts = current < 0 ? -current * voltage : 0;
        if(recorder!=null){maxPowerW=Math.max(maxPowerW,dischargeWatts);recentConsumption.add(distanceMeters,energyWh);}
        if(lastPacket==0 || now-lastPacket>4000 || Math.abs(current)>=0.15)quietSince=0;
        if(Math.abs(current)<0.15 && quietSince==0)quietSince=now;
        chargeEnabled=packet.chargeEnabled;dischargeEnabled=packet.dischargeEnabled;
        lastPacket = now;
        missingSince = 0;
        if(controlPending && controlAcknowledged && dischargeEnabled==desiredDischarge)
            controlFinished("Schaltvorgang vom BMS bestätigt");
        evaluateCurrent();
        sendStatus();
    }

    private void evaluateCurrent() {
        long now = System.currentTimeMillis();
        double dischargeAmps = current < 0 ? -current : 0;
        double idleThreshold = Math.max(0.05, activeAmps * 0.5);

        if (manualHold) {
            if (dischargeAmps < idleThreshold) {
                if (idleSince == 0) idleSince = now;
                if (now - idleSince >= idleMs) { manualHold = false; idleSince = 0; }
            } else idleSince = 0;
            return;
        }
        if(recorder!=null&&tripPausedAt>0){
            boolean confirmed=resumeGate.update(now,dischargeAmps,activeAmps,pausedGpsSpeedKmh,now-gpsAt<=5000);
            if(confirmed){resumeTrip(now);active=true;idleSince=0;emit(ACTION_ACTIVE);setState("Fahrt fortgesetzt");}
            activeSince=0;return;
        }
        if (!active && dischargeAmps >= activeAmps) {
            if (activeSince == 0) activeSince = now;
            if (now - activeSince >= activeMs) {
                active = true;
                idleSince = 0;
                farConfirmations = 0;
                boolean newTrip = recorder == null;
                if (newTrip) startTrip();
                if (recorder == null) { active = false; return; }
                emit(ACTION_ACTIVE);
                setState(newTrip ? "Fahrt gestartet" : "Fahrt fortgesetzt");
            }
        } else if (!active) {
            activeSince = 0;
        }

        if (active && dischargeAmps < idleThreshold) {
            if (idleSince == 0) idleSince = now;
            if (now - idleSince >= idleMs) {
                active = false;
                activeSince = 0;
                farConfirmations = 0;
                emit(ACTION_IDLE);

                setState("Fahrt pausiert – BMS wird weiter überwacht");
            }
        } else if (active) {
            idleSince = 0;
        }
    }

    private void startTrip() {
        long now = System.currentTimeMillis();
        try {
            recorder = new TripRecorder(this, now);
        } catch (IOException exception) {
            setState("Speicherfehler: " + exception.getMessage());
            return;
        }
        tripTree=prefs.getString(StorageFolders.TRIPS,"");
        StorageFolders.install(this);
        tripStartedAt = now;
        distanceMeters = 0;
        maxSpeedKmh = 0;
        currentSpeedKmh = 0;
        altitudeMeters = Double.NaN;
        ascentMeters = 0;
        energyWh = 0;maxPowerW=dischargeWatts;recentConsumption.reset();temperatureSum=0;temperatureSamples=0;java.util.Arrays.fill(sensorSums,0);java.util.Arrays.fill(sensorSamples,0);
        lastPowerAt = now;
        lastLocation = null;
        movingMs = standingMs = 0;
        tripPausedAt=0;pausedGpsSpeedKmh=0;resumeGate.reset();
        lastClock = now;motion.reset();
        startLocationTracking();
        rideNotification(true);
    }

    private void resumeTrip(long now){
        if(recorder==null||tripPausedAt==0)return;
        standingMs+=Math.max(0,now-tripPausedAt);tripPausedAt=0;pausedGpsSpeedKmh=0;resumeGate.reset();lastClock=now;activeSince=0;idleSince=0;motion.reset();
        setState("Fahrt nach Pause fortgesetzt");
    }

    private void completeDeparture() {
        departureMode = false;
        missingSince = 0;
        farConfirmations = 0;
        if (active) {
            active = false;
            emit(ACTION_IDLE);
        }
        if (near) {
            near = false;
            emit(ACTION_FAR);
        }
        if(tripPausedAt>0){near=false;closeGatt();scheduleScan(1000);setState("BMS nicht erreichbar – suche während der Fahrtpause weiter");return;}
        finishTrip();
        closeGatt();
        scheduleScan(1000);
    }

    private void finishTrip() {
        finishTrip(System.currentTimeMillis());
    }
    private void finishTrip(long endedAt) {
        if (recorder == null) return;
        stopLocationTracking();
        if(tripPausedAt>0 && endedAt==tripPausedAt){energyWh=pausedEnergyWh;maxPowerW=pausedMaxPowerW;}
        tripPausedAt=0;
        double averageTemperature=temperatureSamples>0?temperatureSum/temperatureSamples:Double.NaN;
        double temp1=sensorSamples[0]>0?sensorSums[0]/sensorSamples[0]:Double.NaN,temp2=sensorSamples[1]>0?sensorSums[1]/sensorSamples[1]:Double.NaN;
        TripRecorder.Summary summary = recorder.finish(endedAt, distanceMeters,
                maxSpeedKmh, ascentMeters, energyWh, maxPowerW, averageTemperature,temp1,temp2,movingMs,standingMs);
        if(Double.isFinite(averageTemperature)&&distanceMeters>=1000&&energyWh>0&&energyWh/(distanceMeters/1000)<=200)try{temperatureHistory.put(new org.json.JSONObject().put("temperature",averageTemperature).put("km",distanceMeters/1000).put("wh",energyWh));while(temperatureHistory.length()>100)temperatureHistory.remove(0);prefs.edit().putString("temperature_history",temperatureHistory.toString()).apply();}catch(Exception ignored){}
        recorder = null;
        currentSpeedKmh = 0;
        if (distanceMeters >= 500 && energyWh > 0) {
            double actual = energyWh / (distanceMeters / 1000.0);
            averageConsumption = averageConsumption > 0 ? averageConsumption * 0.7 + actual * 0.3 : actual;
            prefs.edit().putString("learned_wh_km", Double.toString(averageConsumption)).apply();
        }
        prefs.edit()
                .putLong("last_started_at", summary.startedAt)
                .putLong("last_ended_at", summary.endedAt)
                .putString("last_distance_m", Double.toString(summary.distanceMeters))
                .putString("last_max_speed", Double.toString(summary.maxSpeedKmh))
                .putString("last_ascent_m", Double.toString(summary.ascentMeters))
                .putString("last_energy_wh", Double.toString(summary.energyWh))
                .putString("last_max_power",Double.toString(maxPowerW))
                .putString("last_gpx", summary.gpxFile.getAbsolutePath())
                .putString("last_csv", summary.csvFile.getAbsolutePath())
                .apply();
        StorageFolders.install(this).exportTrip(tripTree,summary.gpxFile,summary.csvFile,summary.metadataFile);
        StorageFolders.install(this).saveNow();
        rideNotification(false);
        sendStatus();
    }

    private void startLocationTracking() {
        if (locationManager == null || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            setState("Fahrt läuft – Standortberechtigung fehlt");
            return;
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        500, 0, this, Looper.getMainLooper());
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        5_000, 2f, this, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {}
    }

    private void stopLocationTracking() {
        if (locationManager == null) return;
        try { locationManager.removeUpdates(this); } catch (SecurityException ignored) {}
    }

    @Override public void onLocationChanged(Location location) {
        if (recorder == null || location == null) return;
        long now = System.currentTimeMillis();
        if(!LocationManager.GPS_PROVIDER.equals(location.getProvider()) && now-gpsAt<5000)return;
        if (!location.hasAccuracy() || location.getAccuracy() > 35) return;
        if (now - location.getTime() > 10000) return;
        if (lastLocation != null && location.getElapsedRealtimeNanos() <= lastLocation.getElapsedRealtimeNanos()) return;

        double speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
        double deltaMeters=lastLocation==null?0:lastLocation.distanceTo(location);
        double elapsedSeconds=lastLocation==null?0:(location.getElapsedRealtimeNanos()-lastLocation.getElapsedRealtimeNanos())/1e9;
        if(!location.hasSpeed()&&elapsedSeconds>0)speedKmh=deltaMeters/elapsedSeconds*3.6;
        if(!GpsPlausibility.accept(speedKmh, location.hasSpeed(), deltaMeters, elapsedSeconds,
                location.getAccuracy(), lastLocation==null?location.getAccuracy():lastLocation.getAccuracy(), currentSpeedKmh, gpsMaxKmh)) return;
        boolean reliableStop=LocationManager.GPS_PROVIDER.equals(location.getProvider()) && location.getAccuracy()<=15
            && (location.hasSpeed()?(!location.hasSpeedAccuracy()||location.getSpeedAccuracyMetersPerSecond()<=1):elapsedSeconds>0&&elapsedSeconds<=5);
        boolean packetFresh=lastPacket>0&&now-lastPacket<=8000;
        boolean motorPowered=packetFresh&&current<0&&-current>=Math.max(.8,activeAmps);
        long elapsedRealtime=android.os.SystemClock.elapsedRealtime();
        if(tripPausedAt>0){
            gpsAt=now;pausedGpsSpeedKmh=reliableStop?speedKmh:0;currentSpeedKmh=0;lastLocation=new Location(location);motion.reset();sendStatus();return;
        }
        motion.fix(elapsedRealtime,speedKmh,reliableStop,motorPowered);
        if(RideMotion.likelyWalking(speedKmh,reliableStop,motorPowered)){
            // Reliable walking-speed GPS without corresponding BMS load is not scooter travel.
            gpsAt=now;currentSpeedKmh=0;lastLocation=new Location(location);sendStatus();return;
        }
        if (lastLocation != null) {
            long elapsed = location.getTime() - lastLocation.getTime();
            float delta = lastLocation.distanceTo(location);
            double maximumPlausible = Math.max(50, elapsed * 0.020);
            if (elapsed > 0 && delta >= 0.8 && delta <= maximumPlausible) {
                if (speedKmh >= 2 || (!location.hasSpeed() && delta / (elapsed / 1000.0) >= 0.56)) {
                    distanceMeters += delta;
                    DistanceCounters.add(prefs,delta,location.getTime());
                }
                if (location.hasAltitude() && lastLocation.hasAltitude()) {
                    double climb = location.getAltitude() - lastLocation.getAltitude();
                    if (climb > 2.0 && climb < 30) ascentMeters += climb;
                }
            }
        }
        gpsAt = now;
        if (prefs.getBoolean("weather_enabled", true)) {
            weather.update(location.getLatitude(), location.getLongitude(), (temp, at) -> {
                outsideTemperature = temp; weatherAt = at;
                prefs.edit().putString("outside_temperature", Double.toString(temp)).putLong("weather_at", at).apply();
                sendStatus();
            });
        }
        currentSpeedKmh = Math.max(0, speedKmh);
        maxSpeedKmh = Math.max(maxSpeedKmh, currentSpeedKmh);
        if (location.hasAltitude()) altitudeMeters = location.getAltitude();
        lastLocation = new Location(location);
        if(lastPacket>0 && System.currentTimeMillis()-lastPacket<=8000)recentConsumption.add(distanceMeters,energyWh);
        if(prefs.getBoolean("weather_enabled",true)&&Double.isFinite(outsideTemperature)&&weatherAt>0&&System.currentTimeMillis()-weatherAt<=1800000){temperatureSum+=outsideTemperature;temperatureSamples++;}
        if(lastPacket>0&&System.currentTimeMillis()-lastPacket<=8000)for(int i=0;i<Math.min(2,temperatures.length);i++)if(Double.isFinite(temperatures[i])){sensorSums[i]+=temperatures[i];sensorSamples[i]++;}
        Location recorded=new Location(location);recorded.setSpeed((float)(currentSpeedKmh/3.6));
        recorder.add(recorded, packetFresh?soc:-1, packetFresh?voltage:Double.NaN, packetFresh?current:Double.NaN, packetFresh?dischargeWatts:Double.NaN, energyWh, maxPowerW, prefs.getBoolean("weather_enabled",true)&&System.currentTimeMillis()-weatherAt<=1800000?outsideTemperature:Double.NaN,lastPacket>0&&System.currentTimeMillis()-lastPacket<=8000?temperatures:new double[0]);
        sendStatus();
    }

    @Override public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && recorder != null) {
            setState("Fahrt läuft – GPS ausgeschaltet");
        }
    }

    @Override public void onProviderEnabled(String provider) {}

    @Override @SuppressWarnings("deprecation")
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private void closeGatt() {
        if(recorder==null)stopLocationTracking();
        handler.removeCallbacks(pollRunnable);
        handler.removeCallbacks(readRssiRunnable);handler.removeCallbacks(optionalRead);
        handler.removeCallbacks(controlTimeout);handler.removeCallbacks(submitControl);
        if(controlPending)controlMessage="Verbindung abgebrochen – Zustand nicht bestätigt";
        controlPending=false;controlAcknowledged=false;writeBusy=false;quietSince=0;
        BluetoothGatt old = gatt;
        gatt = null;
        writeCharacteristic = null;
        optionalPoll=0;
        monitorStartedAt = 0;
        lastPacket = 0;
        activeSince = 0;
        idleSince = 0;
        packetBuffer.reset();
        if (old != null) {
            try { old.disconnect(); } catch (SecurityException ignored) {}
            try { old.close(); } catch (SecurityException ignored) {}
        }
    }

    private void emit(String action) {
        sendBroadcast(baseIntent(action));
    }

    private Intent baseIntent(String action) {
        return new Intent(action)
                .putExtra("scooter_id",prefs.getString(ScooterProfiles.ACTIVE,""))
                .putExtra("device_address", address)
                .putExtra("rssi", rssi)
                .putExtra("soc", soc)
                .putExtra("voltage", voltage)
                .putExtra("current", current)
                .putExtra("discharge_watts", dischargeWatts)
                .putExtra("energy_wh", energyWh)
                .putExtra("max_power_w",maxPowerW)
                .putExtra("range_recent",recentConsumption.ready())
                .putExtra("speed_kmh", currentSpeedKmh)
                .putExtra("gps_at", gpsAt)
                .putExtra("max_speed_kmh", maxSpeedKmh)
                .putExtra("distance_m", distanceMeters)
                .putExtra("altitude_m", altitudeMeters)
                .putExtra("ascent_m", ascentMeters)
                .putExtra("trip_started_at", tripStartedAt)
                .putExtra("charge_enabled",chargeEnabled)
                .putExtra("discharge_enabled",dischargeEnabled)
                .putExtra("control_pending",controlPending)
                .putExtra("control_message",controlMessage)
                .putExtra("bms_at", lastPacket)
                .putExtra("bms_connected", gatt != null && writeCharacteristic != null)
                .putExtra("temperatures", temperatures)
                .putExtra("bms_values",new org.json.JSONObject(extras.values).toString())
                .putExtra("bms_times",new org.json.JSONObject(extras.times).toString())
                .putExtra("outside_temperature", prefs.getBoolean("weather_enabled", true) ? outsideTemperature : Double.NaN)
                .putExtra("weather_at", weatherAt)
                .putExtra("moving_ms", movingMs)
                .putExtra("standing_ms", standingMs)
                .putExtra("average_speed_kmh", movingMs > 0 ? distanceMeters / movingMs * 3600 : 0)
                .putExtra("total_km", number(prefs.getString("total_km", "0"), 0))
                .putExtra("wh_km", consumption())
                .putExtra("wh_500m", recentConsumption.whPerKm(consumptionWindowMeters()))
                .putExtra("consumption_window_m",consumptionWindowMeters())
                .putExtra("trip_time_ms", movingMs + standingMs + (tripPausedAt>0?Math.max(0,System.currentTimeMillis()-tripPausedAt):0))
                .putExtra("range_km", rangeKm())
                .putExtra("trip_active", recorder != null)
                .putExtra("trip_paused", recorder != null && tripPausedAt>0)
                .putExtra("timestamp", System.currentTimeMillis());
    }

    private void setState(String state) {
        stateText = state;
        sendStatus();
    }

    private void sendStatus() {
        Intent status = baseIntent(ACTION_STATUS)
                .setPackage(getPackageName())
                .putExtra("state", stateText);
        latestStatus = new Intent(status);
        sendBroadcast(status);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && running && (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) manager.notify(11, serviceNotification(notificationLine()));
    }

    private double consumption() {
        double reference = averageConsumption > 0 ? averageConsumption : number(prefs.getString("reference_wh_km", "20"), 20);
        double adjusted=TemperatureHistory.estimate(temperatureHistory,prefs.getBoolean("weather_enabled",true)&&weatherAt>0&&System.currentTimeMillis()-weatherAt<=1800000?outsideTemperature:Double.NaN,reference);
        return recentConsumption.estimate(adjusted);
    }
    private int consumptionWindowMeters(){return Math.max(100,Math.min(1000,prefs.getInt("consumption_window_m",500)));}
    private double rangeKm() {
        if (voltage <= 0) return Double.NaN;
        return RangeEstimator.range(soc, remainingAh, fullAh,
                number(prefs.getString("capacity_ah", "26"), 26),
                number(prefs.getString("nominal_voltage", "48"), 48),
                number(prefs.getString("reserve_percent", "10"), 10), consumption());
    }

    private String notificationLine() {
        if (recorder != null) {
            return String.format(Locale.GERMANY, "%.1f km/h · %.2f km · %.0f W",
                    currentSpeedKmh, distanceMeters / 1000.0, dischargeWatts);
        }
        return stateText;
    }

    private Notification serviceNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, "monitor")
                .setContentTitle("ScootPit BMS")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void rideNotification(boolean started) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        String title=prefs.getString(started?"routine_start_title":"routine_end_title",started?"T6E Fahrt gestartet":"T6E Fahrt beendet");
        String text = started ? "GPS-Aufzeichnung und BMS-Fahrtenbuch laufen."
                : String.format(Locale.GERMANY, "%.2f km · maximal %.1f km/h · %.1f Wh",
                distanceMeters / 1000.0, maxSpeedKmh, energyWh);
        String custom=prefs.getString(started?"routine_start_text":"routine_end_text","");
        if(!custom.isEmpty())text=custom.replace("{scooter}",ScooterProfiles.name(prefs)).replace("{km}",String.format(Locale.GERMANY,"%.2f",distanceMeters/1000)).replace("{wh}",String.format(Locale.GERMANY,"%.1f",energyWh));
        manager.notify(started ? 21 : 22, new Notification.Builder(this, "ride_events")
                .setSmallIcon(started ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build());
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannel(new NotificationChannel(
                "monitor", "Hintergrundbetrieb (separat ausblendbar)", NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(
                "ride_events", "Routine-Signale: Fahrtbeginn und Fahrtende", NotificationManager.IMPORTANCE_DEFAULT));
    }

    @Override public void onDestroy() {
        running = false;liveService=null;
        if(wakeLock!=null && wakeLock.isHeld())wakeLock.release();
        weather.close();
        handler.removeCallbacksAndMessages(null);
        stopScan();
        closeGatt();
        if (recorder != null) finishTrip();
        stopLocationTracking();
        latestStatus = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}


