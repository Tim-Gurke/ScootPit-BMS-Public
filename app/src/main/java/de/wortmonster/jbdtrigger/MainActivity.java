package de.wortmonster.jbdtrigger;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 42;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BmsPicker picker;
    private SharedPreferences prefs;
    private TextView stateView, lastTripView, logView;
    private LinearLayout dashboard;
    private Switch dischargeSwitch;
    private TextView dischargeNote;
    private boolean updatingDischarge;
    private static final int GREEN=0xff21c879, RED=0xffff535d;
    private JSONArray boardTiles;
    private boolean editingBoard;
    private JSONArray savedBoard;
    private CockpitBoard board;
    private Intent lastStatus;
    private boolean logExpanded;
    private final ArrayDeque<String> log = new ArrayDeque<>();
    private String lastLogState="";
    private final List<TextView> tileValues=new ArrayList<>();
    private final List<String> tileKeys=new ArrayList<>();
    private final List<TextView> tileNotes=new ArrayList<>();
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if(BmsMonitorService.ACTION_STATUS.equals(intent.getAction()))renderStatus(intent);
        }
    };
    private AlertDialog tileDialog, settingsDialog, batteryDialog;
    private boolean editorFromSettings;
    private String pendingImageTile="", pendingImageLayout="", draftCacheFile="";

    private final java.util.HashMap<String,String> orientationDrafts=new java.util.HashMap<>(),orientationSaved=new java.util.HashMap<>();
    private String boardKey(){return getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?"cockpit_board_landscape":"cockpit_board";}
    private JSONArray loadBoard(){return CockpitBoard.load(prefs,boardKey());}
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        ScooterProfiles.install(prefs);
        boardTiles=loadBoard();
        if(state!=null){
            pendingImageTile=state.getString("pending_image_tile","");pendingImageLayout=state.getString("pending_image_layout","");editorFromSettings=state.getBoolean("editor_from_settings",false);
            logExpanded=state.getBoolean("log_expanded",false);
            String cache=state.getString("editor_cache","");
            if(cache.matches("cockpit-draft-[A-Za-z0-9-]+\\.json"))try{
                File file=new File(getCacheDir(),cache);if(file.length()>32*1024*1024)throw new Exception();
                JSONObject drafts=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8));
                for(String key:new String[]{"cockpit_board","cockpit_board_landscape"}){
                    if(drafts.getJSONObject("drafts").has(key))orientationDrafts.put(key,drafts.getJSONObject("drafts").getString(key));
                    if(drafts.getJSONObject("saved").has(key))orientationSaved.put(key,drafts.getJSONObject("saved").getString(key));
                }draftCacheFile=cache;
            }catch(Exception ignored){}

            for(String key:new String[]{"cockpit_board","cockpit_board_landscape"}){
                if(state.containsKey("draft_"+key))orientationDrafts.put(key,state.getString("draft_"+key));
                if(state.containsKey("saved_"+key))orientationSaved.put(key,state.getString("saved_"+key));
            }
            if(state.getBoolean("editing_board",false))try{
                savedBoard=new JSONArray(orientationSaved.getOrDefault(boardKey(),boardTiles.toString()));
                boardTiles=new JSONArray(orientationDrafts.getOrDefault(boardKey(),boardTiles.toString()));
                CockpitBoard.validate(boardTiles);editingBoard=true;
            }catch(Exception ignored){}
        }
        StorageFolders.install(this);
        requestNeededPermissions(); rebuild();
        if(state!=null&&state.getBoolean("settings_open",false)&&!editingBoard)showSettings();
    }
    private int color(String value,int fallback){try{return Color.parseColor(value);}catch(Exception e){return fallback;}}
    private int accent(){return color(prefs.getString("accent_color","#FFB300"),0xffffb300);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void rebuild() {
        LinearLayout root=column();root.setPadding(dp(16),dp(12),dp(16),dp(24));
        root.setBackgroundColor(color(prefs.getString("app_background","#0C1014"),0xff0c1014));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setContentDescription("ScootPit BMS Logo");header.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView title=label("ScootPit BMS",22,color(prefs.getString("header_color",prefs.getString("accent_color","#FFB300")),accent()));title.setPadding(dp(8),0,0,0);title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton menu=new ImageButton(this);menu.setImageResource(R.drawable.settings_bolt);menu.setContentDescription("Einstellungen öffnen");
        GradientDrawable menuBackground=new GradientDrawable();menuBackground.setColor(0xff302719);menuBackground.setCornerRadius(dp(14));menuBackground.setStroke(dp(1),0xff7b6020);menu.setBackground(menuBackground);menu.setPadding(dp(12),dp(12),dp(12),dp(12));
        menu.setEnabled(!editingBoard);menu.setOnClickListener(v->showSettings());header.addView(menu,new LinearLayout.LayoutParams(dp(64),dp(56)));root.addView(header);
        if(!editingBoard){Button profile=button("Scooter: "+ScooterProfiles.name(prefs)+" ▾");profile.setContentDescription("Scooter-Profil auswählen");profile.setOnClickListener(v->profileMenu());root.addView(profile);}
        stateView=label(BmsMonitorService.running?"Bereitschaft aktiv – wartet auf Fahrt":"Bereitschaft aus",15,Color.LTGRAY);root.addView(stateView);
        if(!hasSelectedBms()&&!editingBoard){
            root.addView(label("Bitte zuerst das BMS deines Rollers auswählen.",14,Color.LTGRAY));
            Button select=button("BMS auswählen");select.setOnClickListener(v->openBmsPicker());root.addView(select);
        }
        if(editingBoard){
            root.addView(label("Layout: "+(boardKey().equals("cockpit_board")?"Hochformat":"Querformat")+" · Kachel lange drücken und ziehen · unten rechts Größe ziehen · antippen für Inhalt/Farbe.",12,Color.LTGRAY));
            LinearLayout tools=new LinearLayout(this);
            Button save=button("Speichern"),cancel=button("Zurück"),add=button("+ Kachel");
            tools.addView(save,new LinearLayout.LayoutParams(0,-2,1));tools.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));tools.addView(add,new LinearLayout.LayoutParams(0,-2,1));root.addView(tools);
            Button compact=button("Lücken schließen");root.addView(compact);compact.setOnClickListener(v->{board.compact();rebuild();});
            Button reset=button("Standardlayout");root.addView(reset);reset.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Layoutentwurf auf Standard zurücksetzen?").setPositiveButton("Zurücksetzen",(d,w)->{boardTiles=CockpitBoard.defaults();rebuild();}).setNegativeButton("Zurück",null).show());
            save.setOnClickListener(v->{try{CockpitBoard.validate(boardTiles);orientationDrafts.put(boardKey(),boardTiles.toString());SharedPreferences.Editor layouts=prefs.edit();for(java.util.Map.Entry<String,String> draft:orientationDrafts.entrySet()){CockpitBoard.validate(new JSONArray(draft.getValue()));layouts.putString(draft.getKey(),draft.getValue());}layouts.apply();editingBoard=false;orientationDrafts.clear();orientationSaved.clear();rebuild();returnAfterEditor();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});
            cancel.setOnClickListener(v->{boardTiles=savedBoard;editingBoard=false;orientationDrafts.clear();orientationSaved.clear();rebuild();returnAfterEditor();});
            add.setOnClickListener(v->addTileMenu());
        }
        dashboard=column();root.addView(dashboard);buildTiles();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
        if(lastStatus!=null)renderStatus(lastStatus);
        else renderStatus(new Intent().putExtra("outside_temperature",prefs.getBoolean("weather_enabled",true)&&prefs.contains("outside_temperature")?numberPref("outside_temperature"):Double.NaN).putExtra("weather_at",prefs.getLong("weather_at",0)));
    }
    private void endReadiness(){stopService(new Intent(this,BmsMonitorService.class));handler.postDelayed(()->{
        lastStatus=null;stateView.setText("Bereitschaft aus");buildTiles();renderStatus(new Intent().putExtra("outside_temperature",Double.NaN));},250);}
    private void confirmEndReadiness(){
        if(lastStatus!=null && lastStatus.getBooleanExtra("trip_active",false))new AlertDialog.Builder(this)
            .setTitle("Bereitschaft beenden?").setMessage("Die laufende Fahrt wird gespeichert und die automatische Erkennung beendet.")
            .setPositiveButton("Beenden",(d,w)->endReadiness()).setNegativeButton("Zurück",null).show();else endReadiness();
    }
    private void buildTiles(){
        dashboard.removeAllViews();tileValues.clear();tileKeys.clear();tileNotes.clear();dischargeSwitch=null;dischargeNote=null;logView=null;
        board=new CockpitBoard(this,boardTiles,editingBoard,()->{});dashboard.addView(board);
        for(int i=0;i<boardTiles.length();i++){
            JSONObject cell=boardTiles.optJSONObject(i);if(cell==null)continue;
            String key=cell.optString("key","speed");
            LinearLayout box=column();box.setGravity(Gravity.CENTER);box.setPadding(dp(6),dp(4),dp(6),dp(4));
            GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(12));bg.setColor(color(cell.optBoolean("custom_colors",false)?cell.optString("background"):prefs.getString("tile_background","#1C2228"),0xff1c2228));box.setBackground(bg);
            int text=color(cell.optBoolean("custom_colors",false)?cell.optString("text"):prefs.getString("tile_text","#FFFFFF"),Color.WHITE);
            if(key.equals("ready_start")||key.equals("ready_end")||key.equals("trip_end")){
                Button action=button(cell.optString("caption",CockpitLayout.title(key)));
                action.setTextSize(Math.max(12,Math.min(30,cell.optInt("font",18))));action.setMaxLines(Math.max(1,Math.min(3,cell.optInt("lines",2))));action.setAutoSizeTextTypeUniformWithConfiguration(10,Math.max(12,Math.min(30,cell.optInt("font",18))),1,android.util.TypedValue.COMPLEX_UNIT_SP);
                if(!key.equals("trip_end")){action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(key.equals("ready_start")?GREEN:RED));action.setTextColor(Color.BLACK);}
                action.setOnClickListener(v->{if(key.equals("ready_start"))startMonitoring();else if(key.equals("ready_end"))confirmEndReadiness();else if(BmsMonitorService.running && lastStatus!=null && lastStatus.getBooleanExtra("trip_active",false))startService(new Intent(this,BmsMonitorService.class).setAction(BmsMonitorService.ACTION_END_TRIP));else Toast.makeText(this,"Keine laufende Fahrt",Toast.LENGTH_SHORT).show();});
                box.addView(action,new LinearLayout.LayoutParams(-1,-1));
            }else if(key.equals("bms_output")){
                dischargeSwitch=new Switch(this);dischargeSwitch.setText(cell.optString("caption","BMS-Lastausgang"));dischargeSwitch.setTextColor(Color.GRAY);dischargeSwitch.setShowText(false);dischargeSwitch.setEnabled(false);
                dischargeSwitch.setOnCheckedChangeListener((v,on)->{if(updatingDischarge)return;updatingDischarge=true;dischargeSwitch.setChecked(!on);updatingDischarge=false;requestDischarge(on);});
                box.addView(dischargeSwitch);dischargeNote=label("Auf frische BMS-Daten warten",10,Color.GRAY);box.addView(dischargeNote);
            }else if(key.equals("image")){
                String data=cell.optString("image_data");android.graphics.Bitmap image=data.isEmpty()?null:TileImage.decode(data);
                if(cell.optBoolean("show_title",false))box.addView(label(cell.optString("caption","Bild"),12,text));
                if(image!=null){ImageView photo=new ImageView(this);photo.setImageBitmap(image);photo.setScaleType(cell.optInt("image_mode",0)==1?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.FIT_CENTER);photo.setContentDescription(cell.optString("caption","Rollerfoto"));box.addView(photo,new LinearLayout.LayoutParams(-1,0,1));}
                else box.addView(label("Bild auswählen im Kacheleditor",12,text));
            }else if(key.equals("free_text")){
                if(cell.optBoolean("show_title",false))box.addView(label(cell.optString("caption","Freitext"),12,text));
                TextView custom=label(cell.optString("free_text","Dein Text"),Math.max(12,Math.min(80,cell.optInt("font",20))),text);custom.setGravity(Gravity.CENTER);custom.setPadding(dp(4),dp(4),dp(4),dp(4));custom.setContentDescription(custom.getText());ScrollView textScroll=new ScrollView(this);textScroll.setFillViewport(true);textScroll.addView(custom,new ScrollView.LayoutParams(-1,-2));textScroll.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN||event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE)view.getParent().requestDisallowInterceptTouchEvent(true);else view.getParent().requestDisallowInterceptTouchEvent(false);return false;});box.addView(textScroll,new LinearLayout.LayoutParams(-1,0,1));
            }else if(key.equals("log")){
                TextView toggle=label("Statusdetails / Log "+(logExpanded?"▾":"▸"),12,Color.GRAY);box.addView(toggle);
                logView=label(String.join("\n",log),11,Color.GRAY);androidx.core.widget.NestedScrollView details=new androidx.core.widget.NestedScrollView(this);details.setNestedScrollingEnabled(false);details.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN||event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE)view.getParent().requestDisallowInterceptTouchEvent(true);else view.getParent().requestDisallowInterceptTouchEvent(false);return false;});details.addView(logView);details.setVisibility(logExpanded?View.VISIBLE:View.GONE);box.addView(details,new LinearLayout.LayoutParams(-1,0,1));
                toggle.setOnClickListener(v->{logExpanded=!logExpanded;details.setVisibility(logExpanded?View.VISIBLE:View.GONE);toggle.setText("Statusdetails / Log "+(logExpanded?"▾":"▸"));});
            }else{
                JSONObject renderCell=cell;
                if((key.equals("temp1")||key.equals("temp2"))&&!cell.has("caption"))try{renderCell=new JSONObject(cell.toString()).put("caption",prefs.getString(key+"_label",CockpitLayout.title(key)));}catch(Exception ignored){}
                MetricTile value=new MetricTile(this,renderCell,text,color(cell.optString("instrument_color"),accent()),color(cell.optString("scale_color"),0xff35434d),bg.getColor().getDefaultColor());box.setPadding(0,0,0,0);box.addView(value,new LinearLayout.LayoutParams(-1,-1));tileKeys.add(key);tileValues.add(value);tileNotes.add(new TextView(this));

            }
            final int index=i;board.addTile(box,cell,()->editBoardTile(index));
        }
        if(lastStatus!=null)renderStatus(lastStatus);
    }
    private void editBoardTile(int index){
        JSONObject cell=boardTiles.optJSONObject(index);LinearLayout l=column();
        String tileKey=cell.optString("key");boolean personal=tileKey.equals("image")||tileKey.equals("free_text");
        EditText caption=textField(l,"Beschriftung",cell.optString("caption",CockpitLayout.title(cell.optString("key"))));
        caption.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);caption.setMaxLines(3);caption.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        EditText freeText=null;Button chooseImage=null;Spinner imageMode=null;
        if(tileKey.equals("free_text")){freeText=textField(l,"Freitext (max. 4000 Zeichen)",cell.optString("free_text",""));freeText.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);freeText.setMinLines(3);freeText.setMaxLines(12);freeText.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});}
        if(tileKey.equals("image")){
            chooseImage=button(cell.optString("image_data").isEmpty()?"Bild auswählen":"Bild ersetzen");l.addView(chooseImage);
            imageMode=new Spinner(this);imageMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Ganzes Bild einpassen","Kachel füllen (Zuschneiden)"}));imageMode.setSelection(cell.optInt("image_mode",0));l.addView(imageMode);
        }
        final EditText customText=freeText;final Spinner photoMode=imageMode;
        Spinner arrangement=new Spinner(this);arrangement.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Automatisch","Einzeilig","Untereinander"}));arrangement.setSelection(Math.max(0,Math.min(2,cell.optInt("arrangement",0))));l.addView(arrangement);
        Spinner display=new Spinner(this);display.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Zahl","Balken","Rundinstrument"}));display.setSelection(Math.max(0,Math.min(2,cell.optInt("display",cell.optString("key").equals("speed")?2:cell.optString("key").equals("soc")?1:0))));l.addView(display);
        if(personal){arrangement.setVisibility(View.GONE);display.setVisibility(View.GONE);}
        CheckBox showTitle=new CheckBox(this);showTitle.setText("Überschrift anzeigen");showTitle.setTextColor(Color.WHITE);showTitle.setChecked(cell.optBoolean("show_title",!personal));l.addView(showTitle);
        CheckBox showNote=new CheckBox(this);showNote.setText("Zusatztext anzeigen");showNote.setTextColor(Color.WHITE);showNote.setChecked(cell.optBoolean("show_note",true));l.addView(showNote);
        EditText lines=field(l,"Überschrift / Button: maximal 1–3 Zeilen",""+cell.optInt("lines",2));
        EditText scale=field(l,"Skalenmaximum (Balken / Rundinstrument)",""+cell.optDouble("scale_max",cell.optString("key").equals("speed")?CockpitLayout.DEFAULT_SPEED_SCALE_MAX:cell.optString("key").contains("power")?1200:100));
        EditText font=field(l,"Schriftgröße (12–80)",""+cell.optInt("font",28));
        EditText x=field(l,"Spalte (0–11)",""+cell.optInt("x")),y=field(l,"Zeile im Raster (0–300)",""+cell.optInt("y"));
        EditText w=field(l,"Breite (1–12 Rasterspalten)",""+cell.optInt("w")),h=field(l,"Höhe (2–12, je 40 dp)",""+cell.optInt("h"));
        EditText bg=textField(l,"Hintergrund (#RRGGBB)",cell.optBoolean("custom_colors",false)?cell.optString("background"):prefs.getString("tile_background","#1C2228"));
        EditText instrument=personal?new EditText(this):textField(l,"Instrument / Balken: Farbe (#RRGGBB)",cell.optString("instrument_color",prefs.getString("accent_color","#FFB300")));
        EditText track=personal?new EditText(this):textField(l,"Skala / Hintergrundbogen: Farbe (#RRGGBB)",cell.optString("scale_color","#35434D"));
        if(personal){instrument.setText(cell.optString("instrument_color",prefs.getString("accent_color","#FFB300")));track.setText(cell.optString("scale_color","#35434D"));}
        EditText fg=textField(l,"Textfarbe (#RRGGBB)",cell.optBoolean("custom_colors",false)?cell.optString("text"):prefs.getString("tile_text","#FFFFFF"));
        android.widget.SeekBar bgAlpha=opacity(l,"Hintergrund",bg.getText().toString());
        android.widget.SeekBar fgAlpha=opacity(l,"Text",fg.getText().toString());
        ScrollView sv=new ScrollView(this);sv.addView(l);
        LinearLayout content=column();content.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);Button apply=button("Übernehmen"),back=button("Zurück"),remove=button("Entfernen");
        for(Button action:new Button[]{apply,back,remove}){action.setSingleLine(true);action.setMinWidth(0);action.setPadding(dp(2),0,dp(2),0);action.setAutoSizeTextTypeUniformWithConfiguration(9,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);actions.addView(action,new LinearLayout.LayoutParams(0,dp(52),1));}content.addView(actions);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(CockpitLayout.title(cell.optString("key"))).setView(content).create();
        tileDialog=dialog;
        back.setOnClickListener(v->dialog.dismiss());remove.setOnClickListener(v->{if(boardTiles.length()>1){boardTiles.remove(index);dialog.dismiss();rebuild();}});
        apply.setOnClickListener(v->{Double ff=valid(font,12,80),xx=valid(x,0,11),yy=valid(y,0,300),ww=valid(w,1,12),hh=valid(h,2,12);Double ll=valid(lines,1,3),ss=valid(scale,.1,10000000);if(ff==null||xx==null||yy==null||ww==null||hh==null||ll==null||ss==null||!validColor(bg)||!validColor(fg)||!validColor(instrument)||!validColor(track))return;
            JSONArray before;try{before=new JSONArray(boardTiles.toString());cell.put("x",xx.intValue()).put("y",yy.intValue()).put("w",ww.intValue()).put("h",hh.intValue());try{board.push(cell);CockpitBoard.validate(boardTiles);}catch(Exception e){for(int i=0;i<boardTiles.length();i++){JSONObject target=boardTiles.getJSONObject(i),original=before.getJSONObject(i);target.put("x",original.getInt("x")).put("y",original.getInt("y")).put("w",original.getInt("w")).put("h",original.getInt("h"));}Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return;}
                cell.put("arrangement",arrangement.getSelectedItemPosition()).put("display",display.getSelectedItemPosition()).put("show_title",showTitle.isChecked()).put("show_note",showNote.isChecked()).put("lines",ll.intValue()).put("scale_max",ss).put("caption",caption.getText().toString()).put("font",ff.intValue()).put("background",alphaColor(bg.getText().toString(),bgAlpha.getProgress())).put("text",alphaColor(fg.getText().toString(),fgAlpha.getProgress())).put("custom_colors",true).put("instrument_color",instrument.getText().toString()).put("scale_color",track.getText().toString());if(customText!=null)cell.put("free_text",customText.getText().toString());if(photoMode!=null)cell.put("image_mode",photoMode.getSelectedItemPosition());dialog.dismiss();rebuild();
            }catch(Exception e){Toast.makeText(this,"Kachel konnte nicht geändert werden",Toast.LENGTH_SHORT).show();}});dialog.show();
        dialog.getWindow().setLayout(-1,Math.round(getResources().getDisplayMetrics().heightPixels*.9f));
        if(chooseImage!=null)chooseImage.setOnClickListener(v->{apply.performClick();if(dialog.isShowing())return;try{
            if(!cell.has("tile_id"))cell.put("tile_id",UUID.randomUUID().toString());pendingImageTile=cell.getString("tile_id");pendingImageLayout=boardKey();
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),93);
        }catch(Exception e){Toast.makeText(this,"Bildauswahl nicht verfügbar",Toast.LENGTH_LONG).show();}});
    }
    private android.widget.SeekBar opacity(LinearLayout parent,String name,String color){
        TextView value=label(name+" – Transparenz",12,Color.LTGRAY);parent.addView(value);
        android.widget.SeekBar slider=new android.widget.SeekBar(this);slider.setMax(100);slider.setProgress(Math.round((255-Color.alpha(Color.parseColor(color)))*100f/255));parent.addView(slider);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar s,int n,boolean user){value.setText(name+" – Transparenz: "+n+" %");}public void onStartTrackingTouch(android.widget.SeekBar s){}public void onStopTrackingTouch(android.widget.SeekBar s){}});
        value.setText(name+" – Transparenz: "+slider.getProgress()+" %");return slider;
    }
    private String alphaColor(String color,int transparent){return String.format(Locale.ROOT,"#%08X",(Color.parseColor(color)&0xffffff)|(Math.round((100-transparent)*255f/100)<<24));}
    private String f(double n,String format){return Double.isFinite(n)?String.format(Locale.GERMANY,format,n):"–";}
    private void renderStatus(Intent intent){
        lastStatus=new Intent(intent);
        boolean trip=intent.getBooleanExtra("trip_active",false),paused=intent.getBooleanExtra("trip_paused",false);
        stateView.setTextColor(BmsMonitorService.running?GREEN:RED);
        renderDischarge(intent);
        stateView.setText(!BmsMonitorService.running?"Bereitschaft aus":trip?(paused?"Fahrt wird aufgezeichnet · Pause":"Fahrt wird aufgezeichnet"):"Bereitschaft aktiv – wartet auf Fahrt");
        String detail=intent.getStringExtra("state");
        String storageError=prefs.getString("trip_storage_error",prefs.getString("settings_storage_error",""));
        if(!storageError.isEmpty())detail=storageError;
        if(detail!=null && !detail.equals(lastLogState)){
            lastLogState=detail;log.addLast(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date())+" · "+detail);
            while(log.size()>60)log.removeFirst();
        }
        if(logView!=null)logView.setText(String.join("\n",log));
        long packet=intent.getLongExtra("bms_at",0);
        long now=System.currentTimeMillis(),gps=intent.getLongExtra("gps_at",0);
        boolean bmsFresh=BmsMonitorService.running && packet>0 && intent.getBooleanExtra("bms_connected",false) && now-packet<=8000;
        boolean gpsFresh=BmsMonitorService.running && trip && gps>0 && now-gps<=5000;
        for(int i=0;i<tileKeys.size();i++){
            String key=tileKeys.get(i),value="–",note="";
            if(key.startsWith("bms_")){
                try{JSONObject values=new JSONObject(intent.getStringExtra("bms_values")==null?"{}":intent.getStringExtra("bms_values"));JSONObject times=new JSONObject(intent.getStringExtra("bms_times")==null?"{}":intent.getStringExtra("bms_times"));
                    long at=times.optLong(key,0);value=values.optString(key,"–");boolean inactive=!bmsFresh||at==0||now-at>BmsExtras.expiry(key);
                    ((MetricTile)tileValues.get(i)).reading(value,inactive?"Keine aktuellen BMS-Daten":"BMS",inactive);tileNotes.get(i).setText(inactive?"Keine aktuellen BMS-Daten":"BMS");
                }catch(Exception e){((MetricTile)tileValues.get(i)).reading("–","Keine aktuellen BMS-Daten",true);}
                continue;
            }
            double distance=intent.getDoubleExtra("distance_m",0)/1000.0;
            switch(key){
                case "speed":value=f(intent.getDoubleExtra("speed_kmh",0),"%.1f");note="km/h";break;
                case "soc":int soc=intent.getIntExtra("soc",-1);value=soc<0?"–":soc+" %";break;
                case "range":value=f(intent.getDoubleExtra("range_km",Double.NaN),"%.1f km");note=(intent.getBooleanExtra("range_recent",false)?"jüngster Fahrtverbrauch":"Startschätzung")+" · "+prefs.getString("reserve_percent","10")+" % Reserve";break;
                case "distance":value=f(distance,"%.2f km");break;
                case "total":value=f(numberPref("total_km"),"%.2f km");break;
                case "power":value=packet==0?"–":f(intent.getDoubleExtra("discharge_watts",0),"%.0f W");break;
                case "voltage":value=packet==0?"–":f(intent.getDoubleExtra("voltage",0),"%.2f V");break;
                case "current":value=packet==0?"–":f(intent.getDoubleExtra("current",0),"%.2f A");break;
                case "energy":value=f(intent.getDoubleExtra("energy_wh",0),"%.1f Wh");break;
                case "consumption":value=f(intent.getDoubleExtra("wh_km",Double.NaN),"%.1f Wh/km");break;
                case "moving":value=formatDuration(intent.getLongExtra("moving_ms",0));break;
                case "standing":value=formatDuration(intent.getLongExtra("standing_ms",0));break;
                case "average":value=f(intent.getDoubleExtra("average_speed_kmh",0),"%.1f km/h");break;
                case "maximum":value=f(intent.getDoubleExtra("max_speed_kmh",0),"%.1f km/h");break;
                case "altitude":value=f(intent.getDoubleExtra("altitude_m",0),"%.0f m");break;
                case "ascent":value=f(intent.getDoubleExtra("ascent_m",0),"%.0f m");break;
                case "temp1":case "temp2":double[] ts=intent.getDoubleArrayExtra("temperatures");int sensor=key.equals("temp1")?0:1;if(ts!=null && ts.length>sensor)value=f(ts[sensor],"%.1f °C");break;
                case "max_power":value=f(intent.getDoubleExtra("max_power_w",0),"%.0f W");note="elektrisch · BMS-Messwerte";break;
                case "outside":value=f(intent.getDoubleExtra("outside_temperature",Double.NaN),"%.1f °C");
                    long at=intent.getLongExtra("weather_at",0);note=at>0?"Open-Meteo · "+DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at)):"Wetterdaten warten auf Standort";
                    if(at>0 && System.currentTimeMillis()-at>1800000)note+=" · veraltet";break;
            }
            boolean inactive=value.equals("–");
            if(Arrays.asList("soc","range","power","voltage","current","temp1","temp2").contains(key)){
                inactive|=!bmsFresh;
                if(!bmsFresh)note=packet==0?"Keine aktuellen BMS-Daten":!BmsMonitorService.running?"Inaktiv · letzter Messwert":"Veraltet · "+Math.max(0,(now-packet)/1000)+" s";
            }else if(key.equals("speed")||key.equals("altitude"))inactive|=!gpsFresh;
            else if(key.equals("outside")){long at=intent.getLongExtra("weather_at",0);inactive|=!prefs.getBoolean("weather_enabled",true)||at==0||now-at>1800000;}
            else if(!key.equals("total"))inactive|=!BmsMonitorService.running||!trip;
            ((MetricTile)tileValues.get(i)).reading(value,note,inactive);tileNotes.get(i).setText(note);
        }
    }
    private void renderDischarge(Intent intent){
        if(dischargeSwitch==null)return;
        long at=intent.getLongExtra("bms_at",0);
        boolean fresh=BmsMonitorService.running && intent.getBooleanExtra("bms_connected",false) && at>0 && System.currentTimeMillis()-at<4000;
        boolean pending=intent.getBooleanExtra("control_pending",false),on=intent.getBooleanExtra("discharge_enabled",false);
        int tint=!fresh || pending?Color.GRAY:on?GREEN:RED;
        updatingDischarge=true;dischargeSwitch.setChecked(fresh && on);updatingDischarge=false;
        dischargeSwitch.setTextColor(tint);dischargeSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(tint));
        dischargeSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(tint));
        dischargeSwitch.setEnabled(fresh && !pending);
        String message=intent.getStringExtra("control_message");
        dischargeNote.setText(pending?"Schaltbefehl gesendet · Bestätigung wird geprüft":!fresh?"Bereitschaft starten / auf frische BMS-Daten warten":message==null||message.startsWith("Bereitschaft starten")?"Wegfahrsperre · nur im Stand schalten":message);
    }
    private void requestDischarge(boolean on){
        if(!BmsMonitorService.running)return;
        new AlertDialog.Builder(this).setTitle(on?"Lastausgang einschalten?":"Wegfahrsperre einschalten?")
            .setMessage(on?"Der BMS-Lastausgang wird freigegeben. Nur am stehenden Roller schalten.":"Nur im Stand: Roller ausschalten, Fahrt beenden und mindestens 5 Sekunden ohne Last warten. Die Sperre blockiert die Akkuentladung. Die App wartet auf die BMS-Bestätigung.")
            .setPositiveButton(on?"Freigeben":"Sperren",(d,w)->{
                if(BmsMonitorService.running)startService(new Intent(this,BmsMonitorService.class).setAction(BmsMonitorService.ACTION_SET_DISCHARGE).putExtra("enabled",on));})
            .setNegativeButton("Zurück",null).show();
    }
    private void showSettings(){
        String[] items={"Cockpit bearbeiten","App-Farben","Gesamtkilometer korrigieren","BMS und Fahrt-Erkennung","Akku und Restreichweite","Benachrichtigungen","Außentemperatur","Letzte Fahrt / Export","Akkuoptimierung","Speicherorte","Scooter-Profile","Temperatur-Beschriftungen"};
        if(settingsDialog!=null&&settingsDialog.isShowing())return;
        settingsDialog=new AlertDialog.Builder(this).setTitle("Einstellungen").setItems(items,null).setNegativeButton("Schließen",null).create();
        settingsDialog.show();
        settingsDialog.getListView().setOnItemClickListener((parent,view,w,id)->{
            switch(w){case 0:editorFromSettings=true;settingsDialog.dismiss();editLayout();break;case 1:editColors();break;case 2:editOdometer();break;
                case 3:editBms();break;case 4:editBattery();break;case 5:notificationSettings();break;
                case 6:weatherSettings();break;case 7:tripDialog();break;case 8:requestBatteryExemption();break;case 9:storageSettings();break;case 10:profileMenu();break;case 11:editTemperatures();break;}
        });
    }
    private void returnAfterEditor(){if(!draftCacheFile.isEmpty()){new File(getCacheDir(),draftCacheFile).delete();draftCacheFile="";}if(editorFromSettings){editorFromSettings=false;showSettings();}}
    private void changedProfile(){lastStatus=null;log.clear();lastLogState="";logExpanded=false;boardTiles=loadBoard();rebuild();StorageFolders.install(this).saveNow();}
    private void profileMenu(){
        try{JSONArray profiles=ScooterProfiles.export(prefs);String[] names=new String[profiles.length()];for(int i=0;i<names.length;i++)names[i]=profiles.getJSONObject(i).getString("name")+(profiles.getJSONObject(i).getString("id").equals(prefs.getString(ScooterProfiles.ACTIVE,""))?" ✓":"");
            new AlertDialog.Builder(this).setTitle("Scooter auswählen").setItems(names,(d,w)->{try{ScooterProfiles.switchTo(prefs,profiles.getJSONObject(w).getString("id"));changedProfile();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}})
                .setPositiveButton("Neu",(d,w)->profileName(false,false)).setNeutralButton("Verwalten",(d,w)->new AlertDialog.Builder(this).setTitle(ScooterProfiles.name(prefs)).setItems(new String[]{"Umbenennen","Design kopieren → neuer Scooter","Profil entfernen"},(v,i)->{if(i==0)profileName(true,false);else if(i==1)profileName(false,true);else new AlertDialog.Builder(this).setMessage("Profil entfernen? Gespeicherte Fahrtdateien bleiben erhalten.").setPositiveButton("Entfernen",(a,b)->{try{ScooterProfiles.deleteActive(prefs);changedProfile();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}).setNegativeButton("Zurück",null).show();}).setNegativeButton("Zurück",null).show()).setNegativeButton("Schließen",null).show();
        }catch(Exception e){Toast.makeText(this,"Profile konnten nicht gelesen werden",Toast.LENGTH_LONG).show();}
    }
    private void profileName(boolean rename,boolean copy){
        if(!rename&&BmsMonitorService.running){Toast.makeText(this,"Zuerst Bereitschaft beenden",Toast.LENGTH_LONG).show();return;}
        LinearLayout l=column();EditText name=textField(l,"Scooter-Name",rename?ScooterProfiles.name(prefs):"");name.setSingleLine(true);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
        if(copy)l.addView(label("Übernimmt Design und Akku-Einstellungen. BMS, Kilometer, Fahrten und Verbrauchshistorie beginnen leer.",13,Color.LTGRAY));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(rename?"Profil umbenennen":"Neuer Scooter").setView(l).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{if(rename)ScooterProfiles.rename(prefs,name.getText().toString());else ScooterProfiles.add(prefs,name.getText().toString(),copy);dialog.dismiss();changedProfile();}catch(Exception e){name.setError(e.getMessage());}}));dialog.show();
    }
    private void editTemperatures(){
        LinearLayout l=column();l.addView(label("Sensorreihenfolge unverändert. Temp1 = Platine und Temp2 = Akkupack ist bei deinem BMS bisher eine Vermutung. Eigene Kachelbeschriftungen haben Vorrang.",13,Color.LTGRAY));
        EditText first=textField(l,"Temp1-Beschriftung",prefs.getString("temp1_label","Temp1")),second=textField(l,"Temp2-Beschriftung",prefs.getString("temp2_label","Temp2"));first.setSingleLine(true);second.setSingleLine(true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Temperatursensoren").setView(l).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(first.getText().toString().trim().isEmpty()||second.getText().toString().trim().isEmpty()||first.length()>40||second.length()>40){first.setError("Jeweils 1–40 Zeichen");return;}prefs.edit().putString("temp1_label",first.getText().toString().trim()).putString("temp2_label",second.getText().toString().trim()).apply();dialog.dismiss();rebuild();}));dialog.show();
    }
    private void addTileMenu(){
        String[] titles=Arrays.copyOf(CockpitLayout.TITLES,CockpitLayout.TITLES.length+1);titles[titles.length-1]="BMS-Daten (optional)";
        new AlertDialog.Builder(this).setTitle("Kachel hinzufügen").setItems(titles,(d,i)->{if(i<CockpitLayout.KEYS.length)addTile(CockpitLayout.KEYS[i]);else optionalTiles();}).setNegativeButton("Zurück",null).show();
    }
    private void addTile(String key){
        if(boardTiles.length()>=100){Toast.makeText(this,"Maximal 100 Kacheln",Toast.LENGTH_LONG).show();return;}
        if(Arrays.asList("bms_output","log").contains(key))for(int i=0;i<boardTiles.length();i++)if(boardTiles.optJSONObject(i).optString("key").equals(key)){Toast.makeText(this,"Diese Kachel ist bereits vorhanden",Toast.LENGTH_SHORT).show();return;}
        try{boardTiles.put(CockpitBoard.position(CockpitLayout.tile(key).put("show_title",!key.equals("image")&&!key.equals("free_text")),0,board.bottom(),6,3));rebuild();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void optionalTiles(){
        ArrayList<String> keys=new ArrayList<>();try{JSONArray available=new JSONArray(prefs.getString("bms_available","[]"));for(int i=0;i<available.length();i++){String key=available.getString(i);if(BmsExtras.known(key))keys.add(key);}}catch(Exception ignored){}
        keys.sort(Comparator.comparing(CockpitLayout::title));
        if(keys.isEmpty()){new AlertDialog.Builder(this).setTitle("Optionale BMS-Daten").setMessage("Für dieses Profil wurden noch keine Zusatzdaten erkannt. BMS auswählen und Bereitschaft aktivieren; nach einer Verbindung erneut öffnen.").setPositiveButton("OK",null).show();return;}
        String[] names=new String[keys.size()];for(int i=0;i<names.length;i++)names[i]=CockpitLayout.title(keys.get(i));
        new AlertDialog.Builder(this).setTitle("Erkannte BMS-Daten dieses Scooters").setItems(names,(d,i)->addTile(keys.get(i))).setNegativeButton("Zurück",null).show();
    }

    private void notificationSettings(){new AlertDialog.Builder(this).setTitle("Benachrichtigungen")
        .setMessage("Nur den Kanal Hintergrundbetrieb ausschalten, um die dauerhafte Anzeige auszublenden. Routine-Signale aktiviert lassen. Android kann die App weiterhin unter Aktive Apps zeigen.")
        .setPositiveButton("Hintergrundbetrieb",(d,w)->openChannel("monitor"))
        .setNeutralButton("Routine-Signale",(d,w)->openChannel("ride_events")).setNegativeButton("Zurück",null).show();}
    private void openChannel(String id){
        android.app.NotificationManager nm=getSystemService(android.app.NotificationManager.class);
        nm.createNotificationChannel(new android.app.NotificationChannel(id,id.equals("monitor")?"Hintergrundbetrieb (separat ausblendbar)":"Routine-Signale: Fahrtbeginn und Fahrtende",id.equals("monitor")?android.app.NotificationManager.IMPORTANCE_LOW:android.app.NotificationManager.IMPORTANCE_DEFAULT));
        startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,id));
    }
    private void weatherSettings(){new AlertDialog.Builder(this).setTitle("Außentemperatur")
        .setMessage("Wetterwert von Open-Meteo, kein Rollersensor. Während der Fahrt werden gerundete Standortkoordinaten höchstens alle 15 Minuten an Open-Meteo gesendet; Internet erforderlich.")
        .setPositiveButton("Aktivieren",(d,w)->prefs.edit().putBoolean("weather_enabled",true).apply())
        .setNeutralButton("Deaktivieren",(d,w)->prefs.edit().putBoolean("weather_enabled",false).apply()).setNegativeButton("Zurück",null).show();}
    private void editOdometer(){LinearLayout l=column();EditText km=field(l,"Aktueller Gesamtkilometerstand (km)",prefs.getString("total_km","0"));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Gesamtkilometer korrigieren").setView(l).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            Double n=valid(km,0,10000000);if(n==null)return;prefs.edit().putString("total_km",n.toString()).apply();if(lastStatus!=null)renderStatus(lastStatus);else buildTiles();d.dismiss();}));d.show();}
    private void editColors(){
        LinearLayout l=column();
        EditText bg=textField(l,"App-Hintergrund (#RRGGBB)",prefs.getString("app_background","#0C1014"));
        EditText ac=textField(l,"Akzentfarbe (#RRGGBB)",prefs.getString("accent_color","#FFB300"));
        EditText hc=textField(l,"Appkopf-Farbe (#RRGGBB)",prefs.getString("header_color",prefs.getString("accent_color","#FFB300")));
        EditText tileBg=textField(l,"Kachelhintergrund (#RRGGBB)",prefs.getString("tile_background","#1C2228"));
        EditText tileText=textField(l,"Kacheltext (#RRGGBB)",prefs.getString("tile_text","#FFFFFF"));
        l.addView(label("Einzeln eingefärbte Kacheln behalten ihre eigenen Farben.",12,Color.GRAY));
        ScrollView sv=new ScrollView(this);sv.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("App-Farben").setView(sv).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            if(!validColor(bg)||!validColor(ac)||!validColor(hc)||!validColor(tileBg)||!validColor(tileText))return;
            prefs.edit().putString("app_background",bg.getText().toString()).putString("accent_color",ac.getText().toString())
                .putString("header_color",hc.getText().toString()).putString("tile_background",tileBg.getText().toString()).putString("tile_text",tileText.getText().toString()).apply();
            d.dismiss();rebuild();
        }));d.show();
    }
    private boolean validColor(EditText e){try{Color.parseColor(e.getText().toString());return true;}catch(Exception ex){e.setError("Farbe z. B. #FFB300");return false;}}
    private Double valid(EditText e,double min,double max){try{double n=Double.parseDouble(e.getText().toString().replace(',','.'));if(!Double.isFinite(n)||n<min||n>max)throw new Exception();return n;}catch(Exception ex){e.setError("Wert zwischen "+min+" und "+max);return null;}}
    private EditText textField(LinearLayout l,String title,String value){l.addView(label(title,13,Color.LTGRAY));EditText e=new EditText(this);e.setText(value);e.setTextColor(Color.WHITE);l.addView(e);
        if(title.contains("#RRGGBB")){
            Button choose=button("Farbe auswählen");l.addView(choose);
            choose.setOnClickListener(v->{String[] names={"Schwarz","Dunkelblau","Anthrazit","Weiß","Gelb","Orange","Rot","Grün","Blau","Türkis","Violett"};String[] colors={"#0C1014","#10283F","#1C2228","#FFFFFF","#FFB300","#FF8A00","#EF5350","#66BB6A","#42A5F5","#26C6DA","#AB47BC"};
                new AlertDialog.Builder(this).setTitle("Farbe auswählen").setItems(names,(d,w)->e.setText(colors[w])).setNegativeButton("Zurück",null).show();});
        }
        return e;
    }
    private void editBattery(){LinearLayout l=column();String[] keys={"capacity_ah","nominal_voltage","reserve_percent","reference_wh_km"};String[] titles={"Kapazität (Ah, Ersatzwert ohne BMS-Kapazität)","Nennspannung (V)","Restreserve (%)","Startwert Verbrauch (Wh/km)"};String[] values={"26","48","10","20"};List<EditText> es=new ArrayList<>();for(int i=0;i<keys.length;i++)es.add(field(l,titles[i],prefs.getString(keys[i],values[i])));
        l.addView(label("Ab 250 m wird der aktuelle Fahrtverbrauch verwendet; die jüngsten ungefähr 800 m werden stärker gewichtet. Reichweite ist eine Schätzung.",12,Color.GRAY));
        ScrollView scroll=new ScrollView(this);scroll.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Akku und Restreichweite").setView(scroll).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{double[] mins={0.1,1,0,1},maxs={1000,100,99,200};SharedPreferences.Editor edit=prefs.edit();for(int i=0;i<keys.length;i++){Double n=valid(es.get(i),mins[i],maxs[i]);if(n==null)return;edit.putString(keys[i],n.toString());}edit.apply();d.dismiss();}));d.show();}
    private boolean hasSelectedBms(){return BluetoothAdapter.checkBluetoothAddress(prefs.getString("device_address",""));}
    private void editBms(){LinearLayout l=column();TextView selected=panel(hasSelectedBms()?"Ausgewählt: "+prefs.getString("device_name","BMS")+"\n"+prefs.getString("device_address",""):"Noch kein BMS ausgewählt");l.addView(selected);
        Button scan=button(hasSelectedBms()?"Anderes BMS auswählen":"BMS auswählen");scan.setOnClickListener(v->openBmsPicker());l.addView(scan);
        String[] keys={"active_current","active_seconds","idle_seconds","monitor_timeout","connect_rssi","departure_rssi","scan_absent","scan_weak","scan_good","scan_pause","departure_seconds","gps_max_kmh","connect_confirm_seconds"};
        String[] titles={"Fahrt ab Entladestrom (A)","Startverzögerung (s)","Pause ohne Entnahme nach (s)","Ohne Fahrt zurück zur Suche nach (s)","Verbinden ab Empfang (dBm)","Entfernung unter Empfang (dBm)","Suchpause: BMS fehlt (s)","Suchpause: schwacher Empfang (s)","Suchpause: guter Empfang (s)","Wiederverbindung während Fahrtpause (s)","Entfernung bestätigen ohne BMS-Daten (s)","Maximal plausible GPS-Geschwindigkeit (km/h)","Starken Empfang vor Erstverbindung bestätigen (s)"};
        String[] defs={"0.30","3","5","90","-70","-95","60","15","5","5","30","45","3"};
        List<EditText> es=new ArrayList<>();for(int i=0;i<keys.length;i++){EditText input=field(l,titles[i],prefs.getString(keys[i],defs[i]));if(i==4||i==5)input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);es.add(input);}
        l.addView(label("Die Erstverbindung benötigt mindestens drei aufeinanderfolgende starke Empfangsmessungen über die Bestätigungsdauer. Schwacher Empfang setzt diese Prüfung zurück. Frische BMS-Daten erhalten die Verbindung auch bei schwachem Empfang. In Pausen wird weiter Strom abgefragt. Fahrtende erst bei bestätigter Entfernung. Ohne Stromentnahme werden GPS-Strecken nach 3 s nicht weiter gezählt. Änderungen gelten nach Neustart der Bereitschaft.",12,Color.GRAY));ScrollView sv=new ScrollView(this);sv.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("BMS und Fahrt-Erkennung").setView(sv).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            double[] mins={.01,1,1,1,-110,-120,5,5,5,5,10,10,1},maxs={100,3600,3600,3600,-30,-30,3600,3600,3600,60,600,150,15};
            Double[] values=new Double[keys.length];for(int i=0;i<keys.length;i++){values[i]=valid(es.get(i),mins[i],maxs[i]);if(values[i]==null)return;}
            if(values[5]>values[4]){es.get(5).setError("Entfernungsschwelle muss gleich oder schwächer als Verbindungsschwelle sein");return;}
            SharedPreferences.Editor edit=prefs.edit();for(int i=0;i<keys.length;i++)edit.putString(keys[i],values[i].toString());edit.apply();d.dismiss();
        }));d.show();}

    private void tripDialog(){LinearLayout l=column();lastTripView=panel("");l.addView(lastTripView);showLastTrip();Button share=button("Letzte GPX- und CSV-Datei teilen");share.setOnClickListener(v->shareLastTrip());l.addView(share);new AlertDialog.Builder(this).setTitle("Letzte Fahrt").setView(l).setNegativeButton("Schließen",null).show();}
    private void storageSettings(){
        String settings=prefs.getString(StorageFolders.SETTINGS,""),trips=prefs.getString(StorageFolders.TRIPS,"");
        String status="Einstellungen: "+(settings.isEmpty()?"App-Speicher":prefs.getString(StorageFolders.SETTINGS+"_label",settings))+"\n\nFahrtenbuch: "+(trips.isEmpty()?"App-Speicher":prefs.getString(StorageFolders.TRIPS+"_label",trips));
        String error=prefs.getString("settings_storage_error","")+"\n"+prefs.getString("trip_storage_error","");
        LinearLayout l=column();l.addView(label(status,12,Color.LTGRAY));if(!error.trim().isEmpty())l.addView(label(error.trim(),12,RED));
        Button settingsButton=button("Ordner für Einstellungen wählen"),tripButton=button("Ordner für Fahrtenbuch wählen"),load=button("Einstellungen aus gewähltem Ordner laden"),retry=button("Speichern / ausstehende Kopien erneut versuchen");
        for(Button b:new Button[]{settingsButton,tripButton,load,retry})l.addView(b);
        settingsButton.setOnClickListener(v->chooseFolder(91));tripButton.setOnClickListener(v->chooseFolder(92));
        load.setOnClickListener(v->{if(settings.isEmpty())return;if(BmsMonitorService.running){Toast.makeText(this,"Vor dem Laden Bereitschaft beenden",Toast.LENGTH_LONG).show();return;}
            new AlertDialog.Builder(this).setMessage("Aktuelle Einstellungen durch die Datei im gewählten Ordner ersetzen?").setPositiveButton("Laden",(d,w)->StorageFolders.install(this).run(()->{try{StorageFolders.install(this).importSettings(settings);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},errorMessage->{if(errorMessage==null){boardTiles=loadBoard();rebuild();}Toast.makeText(this,errorMessage==null?"Einstellungen geladen":errorMessage,Toast.LENGTH_LONG).show();})).setNegativeButton("Zurück",null).show();});
        retry.setOnClickListener(v->{StorageFolders.install(this).retryAsync();Toast.makeText(this,"Speicherung wird erneut versucht. Ergebnis hier nach erneutem Öffnen.",Toast.LENGTH_LONG).show();});
        l.addView(label("Fahrten werden während der Aufzeichnung lokal gepuffert und nach Abschluss in den gewählten Ordner kopiert. Ordnerwechsel gilt ab der nächsten Fahrt. Die lokale Kopie bleibt zum Teilen erhalten.",12,Color.GRAY));
        new AlertDialog.Builder(this).setTitle("Separate Speicherorte").setView(l).setNegativeButton("Schließen",null).show();
    }
    private void chooseFolder(int request){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,request);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==93){receiveTileImage(result,data);return;}if((request!=91&&request!=92)||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri tree=data.getData();int required=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;try{if((data.getFlags()&required)!=required)throw new SecurityException("Lese- und Schreibzugriff erforderlich");getContentResolver().takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception e){Toast.makeText(this,"Dauerhafter Ordnerzugriff fehlgeschlagen",Toast.LENGTH_LONG).show();return;}
        String key=request==91?StorageFolders.SETTINGS:StorageFolders.TRIPS;
        StorageFolders store=StorageFolders.install(this);
        if(request==91){store.run(()->{try{if(StorageFolders.document(this,tree.toString(),StorageFolders.NAME,false,"application/json")!=null)throw new IllegalStateException("existing");}catch(IllegalStateException e){throw e;}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{
            if("existing".equals(error))new AlertDialog.Builder(this).setTitle("Einstellungsdatei vorhanden").setMessage("Vorhandene Einstellungen laden oder mit den aktuellen Einstellungen überschreiben?").setPositiveButton("Laden",(d,w)->{if(BmsMonitorService.running){Toast.makeText(this,"Zuerst Bereitschaft beenden",Toast.LENGTH_LONG).show();return;}applyFolder(key,tree,true);}).setNeutralButton("Überschreiben",(d,w)->applyFolder(key,tree,false)).setNegativeButton("Zurück",null).show();
            else if(error==null)applyFolder(key,tree,false);else Toast.makeText(this,error,Toast.LENGTH_LONG).show();});
        }else applyFolder(key,tree,false);
    }
    private void receiveTileImage(int result,Intent data){
        final String id=pendingImageTile,key=pendingImageLayout;pendingImageTile="";pendingImageLayout="";
        if(result!=RESULT_OK||data==null||data.getData()==null||id.isEmpty())return;
        Uri uri=data.getData();StorageFolders.install(this).run(()->{try{
            String photo=TileImage.importPhoto(this,uri);
            handler.post(()->{if(!editingBoard)return;try{
                JSONArray tiles=key.equals(boardKey())?boardTiles:new JSONArray(orientationDrafts.getOrDefault(key,"[]"));
                for(int i=0;i<tiles.length();i++)if(id.equals(tiles.getJSONObject(i).optString("tile_id"))){tiles.getJSONObject(i).put("image_data",photo);orientationDrafts.put(key,tiles.toString());if(key.equals(boardKey())){rebuild();editBoardTile(i);}break;}
            }catch(Exception e){Toast.makeText(this,"Bildkachel nicht mehr vorhanden",Toast.LENGTH_LONG).show();}});
        }catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{if(error!=null)Toast.makeText(this,error,Toast.LENGTH_LONG).show();});
    }
    private void applyFolder(String key,Uri tree,boolean load){StorageFolders.install(this).run(()->{try{StorageFolders.install(this).choose(key,tree.toString(),load);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{if(error==null){boardTiles=loadBoard();rebuild();}Toast.makeText(this,error==null?"Speicherort übernommen":error,Toast.LENGTH_LONG).show();});}
    private void editLayout(){try{savedBoard=new JSONArray(boardTiles.toString());editingBoard=true;rebuild();}catch(Exception ignored){}}
    private TextView label(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setPadding(0, dp(7), 0, dp(7));
        return view;
    }

    private TextView section(String value) {
        TextView view = label(value, 15, Color.rgb(255, 179, 0));
        view.setPadding(0, dp(24), 0, dp(7));
        return view;
    }

    private TextView panel(String value) {
        TextView view = label(value, 16, Color.WHITE);
        view.setBackgroundColor(Color.rgb(28, 34, 40));
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private Button button(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(5), 0, dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private EditText field(LinearLayout root, String title, String value) {
        root.addView(label(title, 13, Color.LTGRAY));
        EditText edit = new EditText(this);
        edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit.setText(value);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.GRAY);
        root.addView(edit);
        return edit;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatDuration(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.GERMANY, "%02d:%02d:%02d",
                seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private void requestNeededPermissions() {
        ArrayList<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), REQ_PERMS);
    }

    private void openBmsPicker(){
        if(BmsMonitorService.running){Toast.makeText(this,"Zuerst Bereitschaft beenden",Toast.LENGTH_LONG).show();return;}
        requestNeededPermissions();
        picker=new BmsPicker(this,this::selectBms);picker.show();
    }
    private void selectBms(String address,String name){
        if(BmsMonitorService.running||!BluetoothAdapter.checkBluetoothAddress(address))return;
        SharedPreferences.Editor edit=prefs.edit();
        if(!address.equals(prefs.getString("device_address","")))edit.remove("bms_available");
        edit.putString("device_address",address).putString("device_name",name).apply();
        lastStatus=null;rebuild();Toast.makeText(this,"BMS ausgewählt",Toast.LENGTH_SHORT).show();
    }

    private void startMonitoring() {
        if (BmsMonitorService.running) { Toast.makeText(this,"Bereitschaft ist bereits aktiv",Toast.LENGTH_SHORT).show(); return; }
        if(!hasSelectedBms()){Toast.makeText(this,"Bitte zuerst ein BMS auswählen",Toast.LENGTH_LONG).show();openBmsPicker();return;}
        requestNeededPermissions();
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT>=31 && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED))){Toast.makeText(this,"Bitte Standort und Bluetooth erlauben und erneut starten",Toast.LENGTH_LONG).show();return;}
        Intent service=new Intent(this,BmsMonitorService.class);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);
        stateView.setText("Bereitschaft startet …");
    }

    private void requestBatteryExemption() {
        PowerManager power=getSystemService(PowerManager.class);
        showBatterySettings(power!=null && power.isIgnoringBatteryOptimizations(getPackageName()));
    }
    private void showBatterySettings(boolean exempt) {
        String message=exempt
            ?"ScootPit BMS ist bereits von der Android-Akkuoptimierung ausgenommen."
            :"Die Android-Akkuoptimierung ist für ScootPit BMS aktiv. Für zuverlässige Fahrt-Erkennung im Hintergrund kannst du eine Ausnahme erlauben.";
        batteryDialog=new AlertDialog.Builder(this).setTitle("Akkuoptimierung")
            .setMessage(message+"\n\nZusätzliche Beschränkungen deines Handys findest du in der App-Info unter Akku.")
            .setPositiveButton(exempt?"Android-Einstellungen":"Freigabe anfragen",(d,w)->openBatterySettings(!exempt))
            .setNeutralButton("App-Info",(d,w)->openAppBatteryInfo())
            .setNegativeButton("Zurück",null).show();
    }
    private boolean tryOpenSettings(Intent intent) {
        try { startActivity(intent);return true; }
        catch(android.content.ActivityNotFoundException | SecurityException exception){return false;}
    }
    private void openBatterySettings(boolean request) {
        if(request && tryOpenSettings(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:"+getPackageName()))))return;
        if(tryOpenSettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))return;
        openAppBatteryInfo();
    }
    private void openAppBatteryInfo() {
        if(!tryOpenSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
            Toast.makeText(this,"Bitte Android-Einstellungen → Apps → ScootPit BMS → Akku öffnen.",Toast.LENGTH_LONG).show();
    }

    private void showLastTrip() {
        if (lastTripView == null) return;
        long started = prefs.getLong("last_started_at", 0);
        if (started == 0) {
            lastTripView.setText("Noch keine aufgezeichnete Fahrt");
            return;
        }
        double distance = numberPref("last_distance_m");
        double max = numberPref("last_max_speed");
        double ascent = numberPref("last_ascent_m");
        double energy = numberPref("last_energy_wh");
        lastTripView.setText(String.format(Locale.GERMANY,
                "%s\n%.2f km · max. %.1f km/h · %.0f Hm · %.1f Wh · max. %.0f W",
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                        Locale.GERMANY).format(new Date(started)),
                distance / 1000.0, max, ascent, energy, numberPref("last_max_power")));
    }

    private double numberPref(String key) {
        try { return Double.parseDouble(prefs.getString(key, "0")); }
        catch (Exception ignored) { return 0; }
    }

    private void shareLastTrip() {
        String gpxPath = prefs.getString("last_gpx", "");
        String csvPath = prefs.getString("last_csv", "");
        File gpx = new File(gpxPath);
        File csv = new File(csvPath);
        if (!gpx.exists() || !csv.exists()) {
            Toast.makeText(this, "Noch keine vollständige Fahrt gespeichert", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<Uri> files = new ArrayList<>();
        files.add(FileProvider.getUriForFile(this, getPackageName() + ".files", gpx));
        files.add(FileProvider.getUriForFile(this, getPackageName() + ".files", csv));
        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
        share.setType("application/octet-stream");
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Fahrt teilen"));
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter=new IntentFilter(BmsMonitorService.ACTION_STATUS);
        ContextCompat.registerReceiver(this,statusReceiver,filter,ContextCompat.RECEIVER_NOT_EXPORTED);
        Intent status=BmsMonitorService.latestStatus;if(status!=null)renderStatus(status);
        else renderStatus(new Intent());
    }
    @Override protected void onPause() {super.onPause();try{unregisterReceiver(statusReceiver);}catch(Exception ignored){}}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putString("pending_image_tile",pendingImageTile);state.putString("pending_image_layout",pendingImageLayout);state.putBoolean("editor_from_settings",editorFromSettings);state.putBoolean("settings_open",settingsDialog!=null&&settingsDialog.isShowing());state.putBoolean("log_expanded",logExpanded);state.putBoolean("editing_board",editingBoard);
        if(editingBoard){
            orientationDrafts.put(boardKey(),boardTiles.toString());orientationSaved.put(boardKey(),savedBoard.toString());
            // Photos can exceed Android's Binder state limit; only a small cache reference enters the Bundle.
            try{if(draftCacheFile.isEmpty())draftCacheFile="cockpit-draft-"+UUID.randomUUID()+".json";
                JSONObject content=new JSONObject().put("drafts",new JSONObject(orientationDrafts)).put("saved",new JSONObject(orientationSaved));
                java.nio.file.Files.write(new File(getCacheDir(),draftCacheFile).toPath(),content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));state.putString("editor_cache",draftCacheFile);
            }catch(Exception e){Toast.makeText(this,"Layoutentwurf konnte nicht zwischengesichert werden",Toast.LENGTH_LONG).show();}
        }
    }
    @Override public void onBackPressed(){if(editingBoard){new AlertDialog.Builder(this).setMessage("Ungespeicherten Layoutentwurf verwerfen?").setPositiveButton("Verwerfen",(d,w)->{boardTiles=savedBoard;editingBoard=false;orientationDrafts.clear();orientationSaved.clear();rebuild();returnAfterEditor();}).setNegativeButton("Weiter bearbeiten",null).show();}else super.onBackPressed();}
    @Override protected void onDestroy() {super.onDestroy();handler.removeCallbacksAndMessages(null);if(picker!=null)picker.close();}
}
