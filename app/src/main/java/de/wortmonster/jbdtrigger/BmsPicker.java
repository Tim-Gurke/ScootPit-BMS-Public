package de.wortmonster.jbdtrigger;

import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.io.ByteArrayOutputStream;

/** Separate scrollable BLE picker; selection is saved only after a valid read-only JBD probe. */
final class BmsPicker {
    interface Chosen{void selected(String address,String name);}
    private static final UUID SERVICE=UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb"),NOTIFY=UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"),WRITE=UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb"),CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static final class Candidate{final String address;String name;int rssi;boolean service;Candidate(String a,String n,int r,boolean s){address=a;name=n;rssi=r;service=s;}boolean likely(){return service||name.toUpperCase(Locale.ROOT).matches(".*(BMS|JBD|XIAOXIANG|SP[0-9]{2}S[0-9]).*");}}
    private final Activity activity;private final Chosen chosen;private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,Candidate> devices=new LinkedHashMap<>();private final List<Candidate> shown=new ArrayList<>();
    final ListView list;private final TextView status;private final ArrayAdapter<String> adapter;private final CheckBox all;private final Button scan;private AlertDialog dialog;
    private BluetoothLeScanner scanner;private BluetoothGatt gatt;private BluetoothGattCharacteristic write;
    private Candidate checking;private boolean busy,closed,scanning,subscribed,writeBusy;private int attempts,receivedBytes;private String phase="Verbindungsaufbau";private int generation;private final ByteArrayOutputStream buffer=new ByteArrayOutputStream();
    BmsPicker(Activity a,Chosen c){activity=a;chosen=c;
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);int pad=dp(12);root.setPadding(pad,pad,pad,pad);
        status=new TextView(a);status.setTextColor(android.graphics.Color.WHITE);root.addView(status);
        all=new CheckBox(a);all.setText("Alle BLE-Geräte anzeigen");all.setTextColor(android.graphics.Color.WHITE);all.setOnCheckedChangeListener((v,on)->refresh());root.addView(all);
        LinearLayout buttons=new LinearLayout(a);scan=new Button(a);scan.setText("Suche starten");scan.setOnClickListener(v->{if(scanning)stopScan();else startScan();});buttons.addView(scan,new LinearLayout.LayoutParams(0,-2,1));
        Button manual=new Button(a);manual.setText("Adresse eingeben");manual.setOnClickListener(v->manual());buttons.addView(manual,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
        list=new ListView(a);list.setContentDescription("Gefundene Bluetooth-Geräte");adapter=new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,new ArrayList<>());list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        list.setOnItemClickListener((parent,view,index,id)->{if(index<shown.size())probe(shown.get(index));});
        dialog=new AlertDialog.Builder(a).setTitle("BMS auswählen").setView(root).setNegativeButton("Schließen",null).create();dialog.setOnDismissListener(d->close());
    }
    void show(){dialog.show();dialog.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.85));startScan();}
    void add(String address,String name,int rssi,boolean service){Candidate old=devices.get(address);if(old==null)devices.put(address,new Candidate(address,name,rssi,service));else{if(!name.equals("Unbenanntes BLE-Gerät"))old.name=name;old.rssi=rssi;old.service|=service;}refresh();}
    private void refresh(){shown.clear();for(Candidate c:devices.values())if(all.isChecked()||c.likely())shown.add(c);shown.sort(Comparator.comparing((Candidate c)->!c.likely()).thenComparing(Comparator.comparingInt((Candidate c)->c.rssi).reversed()).thenComparing(c->c.address));
        adapter.clear();for(Candidate c:shown)adapter.add(c.name+"\n"+c.address+" · "+c.rssi+" dBm"+(c.likely()?" · BMS-Kandidat":""));adapter.notifyDataSetChanged();if(!busy)status.setText(shown.size()+" Kandidaten · "+devices.size()+" BLE-Geräte gefunden. Antippen prüft das BMS.");
    }
    private final ScanCallback callback=new ScanCallback(){@Override public void onScanResult(int type,ScanResult r){handler.post(()->{if(closed||!scanning)return;try{String name=r.getDevice().getName();ScanRecord record=r.getScanRecord();if(name==null&&record!=null)name=record.getDeviceName();boolean service=record!=null&&record.getServiceUuids()!=null&&record.getServiceUuids().contains(new ParcelUuid(SERVICE));add(r.getDevice().getAddress(),name==null?"Unbenanntes BLE-Gerät":name,r.getRssi(),service);}catch(SecurityException e){status.setText("Bluetooth-Berechtigung fehlt");stopScan();}});}@Override public void onScanFailed(int error){handler.post(()->{stopScan();status.setText("Suche fehlgeschlagen ("+error+") – erneut versuchen");});}};
    private void startScan(){if(busy)return;try{BluetoothAdapter bt=activity.getSystemService(BluetoothManager.class).getAdapter();if(bt==null||!bt.isEnabled()){status.setText("Bitte Bluetooth einschalten");return;}scanner=bt.getBluetoothLeScanner();scanner.startScan(callback);scanning=true;scan.setText("Suche stoppen");status.setText("Suche läuft … unbekannte Geräte über Alle BLE-Geräte anzeigen");handler.postDelayed(stopScan,20000);}catch(SecurityException e){status.setText("Bitte Bluetooth- und Standortberechtigungen erteilen");}}
    private final Runnable stopScan=this::stopScan;
    private void stopScan(){handler.removeCallbacks(stopScan);if(scanner!=null)try{scanner.stopScan(callback);}catch(SecurityException ignored){}scanning=false;scan.setText("Suche starten");}
    private void manual(){if(busy)return;EditText field=new EditText(activity);field.setSingleLine(true);field.setHint("AA:BB:CC:DD:EE:FF");new AlertDialog.Builder(activity).setTitle("BMS-Adresse").setView(field).setPositiveButton("Prüfen",(d,w)->{String address=field.getText().toString().trim().toUpperCase(Locale.ROOT);if(!BluetoothAdapter.checkBluetoothAddress(address)){status.setText("Ungültige Bluetooth-Adresse");return;}probe(new Candidate(address,"BMS",0,true));}).setNegativeButton("Zurück",null).show();}
    private void probe(Candidate candidate){if(busy||closed)return;stopScan();disconnect();busy=true;checking=candidate;list.setEnabled(false);scan.setEnabled(false);buffer.reset();attempts=0;receivedBytes=0;subscribed=false;writeBusy=false;phase="Verbindungsaufbau";status.setText("Verbinde "+candidate.name+" … bitte andere BMS-App trennen");int token=++generation;
        try{BluetoothAdapter bt=activity.getSystemService(BluetoothManager.class).getAdapter();if(bt==null||!bt.isEnabled()){fail("Bitte Bluetooth einschalten");return;}gatt=bt.getRemoteDevice(candidate.address).connectGatt(activity,false,new BluetoothGattCallback(){
            private void dispatch(Runnable task){handler.post(()->{if(!closed&&busy&&generation==token)task.run();});}
            @Override public void onConnectionStateChange(BluetoothGatt g,int result,int state){dispatch(()->{if(result!=BluetoothGatt.GATT_SUCCESS||state==BluetoothProfile.STATE_DISCONNECTED){fail("Verbindung fehlgeschlagen – andere BMS-App trennen und erneut prüfen");return;}if(state==BluetoothProfile.STATE_CONNECTED)try{phase="Dienstsuche";status.setText("Bluetooth verbunden · prüfe JBD-Schnittstelle …");if(!g.discoverServices())fail("Dienstsuche konnte nicht gestartet werden");}catch(SecurityException e){fail("Bluetooth-Berechtigung fehlt");}});}
            @Override public void onServicesDiscovered(BluetoothGatt g,int result){dispatch(()->{try{BluetoothGattService service=g.getService(SERVICE);if(result!=0||service==null){fail("Keine passende JBD-Schnittstelle gefunden");return;}BluetoothGattCharacteristic notify=service.getCharacteristic(NOTIFY);write=service.getCharacteristic(WRITE);if(notify==null||write==null){fail("JBD-Schnittstelle unvollständig");return;}phase="Antwortkanal";BluetoothGattDescriptor descriptor=notify.getDescriptor(CCCD);if(descriptor==null||!g.setCharacteristicNotification(notify,true)){fail("BMS-Antworten nicht aktivierbar");return;}if(Build.VERSION.SDK_INT>=33){if(g.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)!=BluetoothStatusCodes.SUCCESS)fail("BMS-Antworten nicht aktivierbar");}else{descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);if(!g.writeDescriptor(descriptor))fail("BMS-Antworten nicht aktivierbar");}}catch(SecurityException e){fail("Bluetooth-Berechtigung fehlt");}});}
            @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int result){dispatch(()->{if(!CCCD.equals(d.getUuid()))return;if(result!=0){fail("BMS-Antwortkanal abgelehnt ("+result+")");return;}subscribed=true;phase="Datenabfrage";status.setText("Antwortkanal bereit · warte auf BMS-Daten …");handler.removeCallbacks(readBasic);handler.postDelayed(readBasic,800);});}
            @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int result){dispatch(()->{writeBusy=false;if(result!=BluetoothGatt.GATT_SUCCESS)fail("Bluetooth-Schreiben fehlgeschlagen ("+result+") – näher herangehen und erneut prüfen");});}
            @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] data){if(NOTIFY.equals(c.getUuid()))dispatch(()->receive(data));}
            @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] data=c.getValue();if(NOTIFY.equals(c.getUuid()))dispatch(()->receive(data));}
        },BluetoothDevice.TRANSPORT_LE);handler.postDelayed(()->{if(busy&&generation==token)fail(phase.equals("Datenabfrage")?(receivedBytes==0?"Bluetooth verbunden, aber keine BMS-Daten erhalten. Overkill trennen, näher herangehen und erneut prüfen.":"BMS antwortet, aber keine gültigen Basisdaten erhalten. Erneut prüfen."):"Zeitüberschreitung bei "+phase+" – andere BMS-App trennen und erneut prüfen");},30000);}catch(SecurityException|IllegalArgumentException e){fail("BMS-Verbindung nicht möglich");}
    }
    // Read-only retries match the established monitor cadence. A missing first reply must not reject a compatible BMS.
    private final Runnable readBasic=new Runnable(){@Override public void run(){
        if(closed||!busy||!subscribed||gatt==null||write==null)return;
        if(attempts>=8){fail(receivedBytes==0?"Bluetooth verbunden, aber keine BMS-Daten erhalten. Overkill trennen und näher herangehen.":"BMS antwortet, aber keine gültigen Basisdaten erhalten.");return;}
        if(!writeBusy)try{byte[] command=BmsExtras.readCommand(3);int type=(write.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)!=0?BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            boolean sent;if(Build.VERSION.SDK_INT>=33)sent=gatt.writeCharacteristic(write,command,type)==BluetoothStatusCodes.SUCCESS;else{write.setWriteType(type);write.setValue(command);sent=gatt.writeCharacteristic(write);}
            if(sent){writeBusy=true;attempts++;status.setText("Bluetooth verbunden · BMS-Abfrage "+attempts+" von 8 …");}
        }catch(SecurityException e){fail("Bluetooth-Berechtigung fehlt");return;}
        handler.postDelayed(this,2000);
    }};
    private void receive(byte[] data){if(data==null)return;receivedBytes+=data.length;buffer.write(data,0,data.length);byte[] pending=buffer.toByteArray();if(pending.length>1024){fail("Ungültige BMS-Antwort");return;}int cursor=0;while(cursor<pending.length){if((pending[cursor]&255)!=0xdd){cursor++;continue;}if(pending.length-cursor<4)break;int n=(pending[cursor+3]&255)+7;if(pending.length-cursor<n)break;byte[] frame=Arrays.copyOfRange(pending,cursor,cursor+n);if(!JbdControl.validReply(frame)){cursor++;continue;}if(BmsPacket.decode(frame)!=null){Candidate verified=checking;busy=false;generation++;handler.removeCallbacks(readBasic);disconnect();chosen.selected(verified.address,verified.name);dialog.dismiss();return;}cursor+=n;}buffer.reset();buffer.write(pending,cursor,pending.length-cursor);}
    private void fail(String text){generation++;busy=false;handler.removeCallbacks(readBasic);disconnect();list.setEnabled(true);scan.setEnabled(true);status.setText(text);}
    private void disconnect(){if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(SecurityException ignored){}gatt=null;}write=null;subscribed=false;writeBusy=false;}
    void close(){closed=true;generation++;busy=false;stopScan();handler.removeCallbacksAndMessages(null);disconnect();}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
}
