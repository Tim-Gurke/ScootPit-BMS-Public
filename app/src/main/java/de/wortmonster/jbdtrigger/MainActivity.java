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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private View stateDot;
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
    private AlertDialog tileDialog, settingsDialog, batteryDialog, batteryConfigDialog, colorDialog, tourDialog, storageDialog;
    private boolean editorFromSettings;
    private String displayedProfile="";
    private JournalUi journalAfterImport;
    private String pendingImageTile="", pendingImageLayout="", draftCacheFile="";

    private final java.util.HashMap<String,String> orientationDrafts=new java.util.HashMap<>(),orientationSaved=new java.util.HashMap<>();
    private String boardKey(){return getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?"cockpit_board_landscape":"cockpit_board";}
    private JSONArray loadBoard(){return CockpitBoard.load(prefs,boardKey());}
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        installDefaultCockpit();
        ScooterProfiles.install(prefs);
        boardTiles=loadBoard();displayedProfile=prefs.getString(ScooterProfiles.ACTIVE,"");
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
    private void installDefaultCockpit(){
        if(prefs.contains(ScooterProfiles.LIST)||prefs.contains("cockpit_board")||prefs.contains("cockpit_layout"))return;
        try(java.io.InputStream in=getAssets().open("default_cockpit_settings.json")){
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;while((count=in.read(chunk))!=-1)buffer.write(chunk,0,count);byte[] bytes=buffer.toByteArray();if(bytes.length==0)return;
            JSONObject root=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));JSONObject settings=root.getJSONObject("settings");
            JSONArray portrait=root.getJSONArray("cockpit_board"),landscape=root.getJSONArray("cockpit_board_landscape");
            CockpitBoard.validate(portrait);CockpitBoard.validate(landscape);
            SharedPreferences.Editor edit=prefs.edit().putString("cockpit_board",portrait.toString()).putString("cockpit_board_landscape",landscape.toString()).putString("cockpit_design_version",root.optString("cockpit_design_version","1.2.1")).putString("cockpit_board_landscape_version",root.optString("cockpit_board_landscape_version","1.2.1"));
            java.util.Iterator<String> settingKeys=settings.keys();while(settingKeys.hasNext()){String key=settingKeys.next();Object value=settings.get(key);if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof String)edit.putString(key,(String)value);}
            if(!edit.commit())throw new java.io.IOException("Standard-Cockpit konnte nicht gespeichert werden");
        }catch(Exception e){android.util.Log.e("ScootPit","Standard-Cockpit konnte nicht geladen werden",e);}
    }
    private int color(String value,int fallback){try{return Color.parseColor(value);}catch(Exception e){return fallback;}}
    private int accent(){return color(prefs.getString("accent_color","#FF9800"),0xffff9800);}
    private int foreground(){return CockpitTheme.foreground(prefs);}
    private int muted(){return CockpitTheme.light(prefs)?0xff526579:0xffacb7c1;}
    private void updateStateDot(){if(stateDot==null)return;android.graphics.drawable.GradientDrawable dot=new android.graphics.drawable.GradientDrawable();dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);if(BmsMonitorService.running){dot.setColors(new int[]{GREEN,GREEN,0x0039dc74});dot.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);dot.setGradientRadius(dp(8));}else dot.setColor(muted());stateDot.setBackground(dot);}
    private int iconColor(JSONObject cell){return color(cell.optString("icon_color"),CockpitSymbols.defaultColor(cell.optString("key"),CockpitTheme.light(prefs)));}
    private ImageButton headerAction(String key,String description,Runnable action){ImageButton b=new ImageButton(this);b.setContentDescription(description);b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(dp(10),dp(10),dp(10),dp(10));b.setScaleType(ImageView.ScaleType.FIT_CENTER);if(key.equals("settings")){b.setImageResource(R.drawable.settings_bolt);b.setImageTintList(android.content.res.ColorStateList.valueOf(accent()));}else b.setImageDrawable(new CockpitSymbols(key,accent()));b.setEnabled(!editingBoard);b.setOnClickListener(v->action.run());return b;}
    private View separator(boolean vertical){return new View(this){private final android.graphics.Paint p=new android.graphics.Paint(3);@Override protected void onDraw(android.graphics.Canvas c){float w=getWidth(),h=getHeight();p.setShader(new android.graphics.LinearGradient(0,0,vertical?0:w,vertical?h:0,new int[]{accent(),Color.argb(35,Color.red(accent()),Color.green(accent()),Color.blue(accent())),Color.TRANSPARENT},new float[]{0,.4f,1},android.graphics.Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);}};}
    private void personalHeading(LinearLayout box,JSONObject cell,int text){
        if(!CockpitSymbols.icon(cell)&&!CockpitSymbols.title(cell))return;
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);row.setPadding(0,dp(2),0,dp(4));
        if(CockpitSymbols.icon(cell)){ImageView icon=new ImageView(this);icon.setImageDrawable(new CockpitSymbols(cell.optString("key"),iconColor(cell)));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));}
        if(CockpitSymbols.title(cell)){TextView label=label(CockpitSymbols.caption(cell),12,text);label.setPadding(dp(5),0,0,0);row.addView(label,new LinearLayout.LayoutParams(-2,-2));}box.addView(row);
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private ScrollView scrollMenu(View content){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setVerticalScrollBarEnabled(true);s.setTag("submenu_scroll");s.addView(content);return s;}
    private void limitDialogHeight(AlertDialog dialog,float fraction){if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,Math.round(getResources().getDisplayMetrics().heightPixels*fraction));}
    private void rebuild() {
        setTheme(CockpitTheme.light(prefs)?R.style.AppThemeLight:R.style.AppTheme);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(CockpitTheme.light(prefs));
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(CockpitTheme.light(prefs));
        getWindow().setNavigationBarColor(android.os.Build.VERSION.SDK_INT<27?0xff0b1015:color(prefs.getString("app_background","#0B1015"),0xff0b1015));
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root=column();root.setPadding(dp(14),dp(landscape?0:6),dp(14),dp(landscape?0:20));
        root.setBackgroundColor(prefs.getBoolean("background_gradient_enabled",false)?Color.TRANSPARENT:color(prefs.getString("app_background","#0B1015"),0xff0b1015));
        LinearLayout header=new LinearLayout(this);header.setTag("cockpit_header");header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(0,dp(landscape?0:3),0,dp(landscape?0:7));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_logo);logo.setContentDescription("ScootPit BMS Logo");header.addView(logo,new LinearLayout.LayoutParams(dp(landscape?36:42),dp(landscape?38:48)));
        LinearLayout identity=column();identity.setPadding(dp(landscape?6:8),0,dp(4),0);
        TextView title=label("ScootPit BMS",landscape?20:22,color(prefs.getString("header_color",prefs.getString("accent_color","#FF9800")),accent()));title.setTypeface(null,Typeface.BOLD);title.setPadding(0,0,0,0);title.setSingleLine(true);title.setAutoSizeTextTypeUniformWithConfiguration(14,landscape?20:22,1,android.util.TypedValue.COMPLEX_UNIT_SP);identity.addView(title,new LinearLayout.LayoutParams(-1,dp(landscape?24:28)));
        LinearLayout profileRow=new LinearLayout(this);profileRow.setGravity(Gravity.CENTER_VERTICAL);profileRow.setMinimumHeight(dp(landscape?30:40));profileRow.setContentDescription("Scooter-Profil auswählen: "+ScooterProfiles.name(prefs));profileRow.setEnabled(!editingBoard);profileRow.setOnClickListener(v->profileMenu());
        TextView profile=label(ScooterProfiles.name(prefs),landscape?13:14,foreground());profile.setSingleLine(true);profile.setEllipsize(android.text.TextUtils.TruncateAt.END);profileRow.addView(profile,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=label("⌄",landscape?16:18,accent());arrow.setPadding(dp(5),0,0,0);profileRow.addView(arrow);identity.addView(profileRow);
        header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout stateRow=new LinearLayout(this);stateRow.setTag("cockpit_readiness_status");stateRow.setGravity(Gravity.CENTER_VERTICAL);
        if(landscape){
            stateRow.setPadding(dp(4),0,dp(4),0);
            stateDot=new View(this);LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(dp(12),dp(12));dotParams.setMargins(0,0,dp(6),0);stateRow.addView(stateDot,dotParams);
            stateView=label(BmsMonitorService.running?"Bereitschaft aktiv":"Bereitschaft aus",13,foreground());stateRow.addView(stateView);header.addView(stateRow,new LinearLayout.LayoutParams(-2,-2));
        }
        LinearLayout.LayoutParams divider=new LinearLayout.LayoutParams(dp(1),dp(landscape?32:38));divider.setMargins(dp(4),0,dp(4),0);header.addView(separator(true),divider);
        header.addView(headerAction("journal","Fahrtenbuch",()->{journalAfterImport=new JournalUi(this,prefs);journalAfterImport.show();}),new LinearLayout.LayoutParams(dp(landscape?40:48),dp(landscape?40:48)));
        header.addView(headerAction("settings","Einstellungen öffnen",()->showSettings()),new LinearLayout.LayoutParams(dp(landscape?40:48),dp(landscape?40:56)));root.addView(header);
        LinearLayout.LayoutParams rule=new LinearLayout.LayoutParams(-1,dp(1));rule.setMargins(0,0,0,dp(landscape?0:8));root.addView(separator(false),rule);
        if(!landscape){
            stateRow.setGravity(Gravity.CENTER_VERTICAL);stateRow.setPadding(0,dp(7),0,dp(10));
            stateDot=new View(this);LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(dp(16),dp(16));dotParams.setMargins(0,0,dp(10),0);stateRow.addView(stateDot,dotParams);
            stateView=label(BmsMonitorService.running?"Bereitschaft aktiv":"Bereitschaft aus",15,foreground());stateRow.addView(stateView);root.addView(stateRow);
        }
        updateStateDot();
        if(!hasSelectedBms()&&!editingBoard){
            root.addView(label("Bitte zuerst das BMS deines Rollers auswählen.",14,muted()));
            Button select=button("BMS auswählen");select.setOnClickListener(v->openBmsPicker());root.addView(select);
        }
        if(editingBoard){
            root.addView(label("Layout: "+(boardKey().equals("cockpit_board")?"Hochformat":"Querformat")+" · Kachel lange drücken und ziehen · unten rechts Größe ziehen · antippen für Inhalt/Farbe.",12,muted()));
            LinearLayout tools=new LinearLayout(this);
            Button save=button("Speichern"),cancel=button("Zurück"),add=button("+ Kachel");
            tools.addView(save,new LinearLayout.LayoutParams(0,-2,1));tools.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));tools.addView(add,new LinearLayout.LayoutParams(0,-2,1));root.addView(tools);
            Button compact=button("Lücken schließen");root.addView(compact);compact.setOnClickListener(v->{board.compact();rebuild();});
            Button reset=button("Standardlayout");root.addView(reset);reset.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Layoutentwurf auf Standard zurücksetzen?").setPositiveButton("Zurücksetzen",(d,w)->{boardTiles=CockpitBoard.standardDefaults(this,boardKey());rebuild();}).setNegativeButton("Zurück",null).show());
            save.setOnClickListener(v->{try{CockpitBoard.validate(boardTiles);orientationDrafts.put(boardKey(),boardTiles.toString());SharedPreferences.Editor layouts=prefs.edit();for(java.util.Map.Entry<String,String> draft:orientationDrafts.entrySet()){CockpitBoard.validate(new JSONArray(draft.getValue()));layouts.putString(draft.getKey(),draft.getValue());}layouts.apply();editingBoard=false;orientationDrafts.clear();orientationSaved.clear();rebuild();returnAfterEditor();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});
            cancel.setOnClickListener(v->{boardTiles=savedBoard;editingBoard=false;orientationDrafts.clear();orientationSaved.clear();rebuild();returnAfterEditor();});
            add.setOnClickListener(v->addTileMenu());
        }
        dashboard=column();root.addView(dashboard);buildTiles();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(true);
        if(prefs.getBoolean("background_gradient_enabled",false))try{scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.parseColor(prefs.getString("background_gradient_start","#0B1015")),Color.parseColor(prefs.getString("background_gradient_end","#26384A"))}));}catch(Exception e){scroll.setBackgroundColor(color(prefs.getString("app_background","#0B1015"),0xff0c1014));}
        else scroll.setBackgroundColor(color(prefs.getString("app_background","#0B1015"),0xff0c1014));scroll.addView(root);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        setContentView(scroll);ViewCompat.requestApplyInsets(scroll);
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
            GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(Math.max(0,Math.min(48,cell.optInt("corner_radius",12)))));bg.setStroke(dp(cell.optBoolean("border_enabled",true)?1:0),color(cell.optString("border_color",prefs.getString("outline_color",CockpitTheme.light(prefs)?"#D5DFEB":"#2C3843")),muted()));bg.setColor(color(cell.optBoolean("custom_colors",false)?cell.optString("background"):prefs.getString("tile_background",CockpitTheme.light(prefs)?"#F3F7FC":"#141B22"),0xff1c2228));box.setBackground(bg);
            int text=color(cell.optBoolean("custom_colors",false)?cell.optString("text"):prefs.getString("tile_text",CockpitTheme.light(prefs)?"#172B40":"#FFFFFF"),foreground());
            if(key.equals("ready_start")||key.equals("ready_end")||key.equals("trip_end")){
                Button action=button(cell.optString("caption",CockpitLayout.title(key)));
                if(!CockpitSymbols.title(cell))action.setText("");if(CockpitSymbols.icon(cell)){CockpitSymbols icon=new CockpitSymbols(key,iconColor(cell));icon.setBounds(0,0,dp(26),dp(26));action.setCompoundDrawables(null,icon,null,null);action.setCompoundDrawablePadding(dp(4));}action.setContentDescription(CockpitLayout.title(key));
                action.setTextSize(Math.max(12,Math.min(30,cell.optInt("font",18))));action.setMaxLines(Math.max(1,Math.min(3,cell.optInt("lines",2))));action.setAutoSizeTextTypeUniformWithConfiguration(10,Math.max(12,Math.min(30,cell.optInt("font",18))),1,android.util.TypedValue.COMPLEX_UNIT_SP);
                if(!key.equals("trip_end")){action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(key.equals("ready_start")?GREEN:RED));action.setTextColor(Color.BLACK);}
                action.setOnClickListener(v->{if(key.equals("ready_start"))startMonitoring();else if(key.equals("ready_end"))confirmEndReadiness();else if(BmsMonitorService.running && lastStatus!=null && lastStatus.getBooleanExtra("trip_active",false))startService(new Intent(this,BmsMonitorService.class).setAction(BmsMonitorService.ACTION_END_TRIP));else Toast.makeText(this,"Keine laufende Fahrt",Toast.LENGTH_SHORT).show();});
                box.addView(action,new LinearLayout.LayoutParams(-1,-1));
            }else if(key.equals("bms_output")){
                dischargeSwitch=new Switch(this);personalHeading(box,cell,text);dischargeSwitch.setText("");dischargeSwitch.setContentDescription("BMS-Lastausgang");dischargeSwitch.setTextColor(muted());dischargeSwitch.setShowText(false);dischargeSwitch.setEnabled(false);
                dischargeSwitch.setOnCheckedChangeListener((v,on)->{if(updatingDischarge)return;updatingDischarge=true;dischargeSwitch.setChecked(!on);updatingDischarge=false;requestDischarge(on);});
                box.addView(dischargeSwitch);dischargeNote=label("Auf frische BMS-Daten warten",10,muted());box.addView(dischargeNote);
            }else if(key.equals("image")){
                String data=cell.optString("image_data");android.graphics.Bitmap image=data.isEmpty()?null:TileImage.decode(data);
                personalHeading(box,cell,text);
                if(image!=null){ImageView photo=new ImageView(this);photo.setImageBitmap(image);photo.setScaleType(cell.optInt("image_mode",0)==1?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.FIT_CENTER);photo.setContentDescription(cell.optString("caption","Rollerfoto"));box.addView(photo,new LinearLayout.LayoutParams(-1,0,1));}
                else box.addView(label("Bild auswählen im Kacheleditor",12,text));
            }else if(key.equals("free_text")){
                personalHeading(box,cell,text);
                TextView custom=label(cell.optString("free_text","Dein Text"),Math.max(12,Math.min(80,cell.optInt("font",20))),text);custom.setGravity(Gravity.CENTER);custom.setPadding(dp(4),dp(4),dp(4),dp(4));custom.setContentDescription(custom.getText());ScrollView textScroll=new ScrollView(this);textScroll.setFillViewport(true);textScroll.addView(custom,new ScrollView.LayoutParams(-1,-2));textScroll.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN||event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE)view.getParent().requestDisallowInterceptTouchEvent(true);else view.getParent().requestDisallowInterceptTouchEvent(false);return false;});box.addView(textScroll,new LinearLayout.LayoutParams(-1,0,1));
            }else if(key.equals("log")){
                TextView toggle=label((CockpitSymbols.title(cell)?cell.optString("caption","Statusdetails / Log"):"")+" "+(logExpanded?"▾":"▸"),12,muted());if(CockpitSymbols.icon(cell)){CockpitSymbols icon=new CockpitSymbols(key,iconColor(cell));icon.setBounds(0,0,dp(22),dp(22));toggle.setCompoundDrawables(icon,null,null,null);toggle.setCompoundDrawablePadding(dp(6));}toggle.setContentDescription("Statusdetails / Log ein- oder ausblenden");box.addView(toggle);
                logView=label(String.join("\n",log),11,muted());androidx.core.widget.NestedScrollView details=new androidx.core.widget.NestedScrollView(this);details.setNestedScrollingEnabled(false);details.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN||event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE)view.getParent().requestDisallowInterceptTouchEvent(true);else view.getParent().requestDisallowInterceptTouchEvent(false);return false;});details.addView(logView);details.setVisibility(logExpanded?View.VISIBLE:View.GONE);box.addView(details,new LinearLayout.LayoutParams(-1,0,1));
                toggle.setOnClickListener(v->{logExpanded=!logExpanded;details.setVisibility(logExpanded?View.VISIBLE:View.GONE);toggle.setText((CockpitSymbols.title(cell)?cell.optString("caption","Statusdetails / Log"):"")+" "+(logExpanded?"▾":"▸"));});
            }else{
                JSONObject renderCell=cell;
                if(key.equals("consumption_500m"))try{String current=cell.optString("caption","");if(current.isEmpty()||current.matches("Verbrauch · letzte \\d+ m")||current.equals(CockpitLayout.title(key)))cell.put("caption","Verbrauch · letzte "+prefs.getInt("consumption_window_m",500)+" m");}catch(Exception ignored){}
                if((key.equals("temp1")||key.equals("temp2"))&&!cell.has("caption"))try{renderCell=new JSONObject(cell.toString()).put("caption",prefs.getString(key+"_label",CockpitLayout.title(key)));}catch(Exception ignored){}
                MetricTile value=new MetricTile(this,renderCell,text,color(cell.optString("instrument_color"),accent()),color(cell.optString("scale_color",CockpitTheme.scale(prefs)),0xff35434d),bg.getColor().getDefaultColor());box.setPadding(0,0,0,0);box.addView(value,new LinearLayout.LayoutParams(-1,-1));tileKeys.add(key);tileValues.add(value);tileNotes.add(new TextView(this));

            }
            if(key.equals("tour")&&!editingBoard){box.setOnClickListener(v->resetTour());box.setContentDescription("Tourenzähler zurücksetzen");}
            else if(!editingBoard&&Arrays.asList("temp1","temp2","outside").contains(key)){box.setOnClickListener(v->refreshTemperature());box.setContentDescription(CockpitLayout.title(key)+" aktualisieren");}
            final int index=i;board.addTile(box,cell,()->editBoardTile(index));
        }
        if(lastStatus!=null)renderStatus(lastStatus);
    }
    private void editBoardTile(int index){
        JSONObject cell=boardTiles.optJSONObject(index);LinearLayout l=column();
        String tileKey=cell.optString("key");boolean personal=tileKey.equals("image")||tileKey.equals("free_text");
        EditText caption=textField(l,"Beschriftung",cell.optString("caption",CockpitLayout.title(cell.optString("key"))));
        caption.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);caption.setMaxLines(3);caption.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        TextView windowLabel=label("Verbrauchsfenster: "+prefs.getInt("consumption_window_m",500)+" m",13,muted());
        android.widget.SeekBar windowSlider=new android.widget.SeekBar(this);windowSlider.setMax(90);windowSlider.setProgress(Math.max(0,Math.min(90,(prefs.getInt("consumption_window_m",500)-100)/10)));
        windowSlider.setContentDescription("Verbrauchsfenster 100 bis 1000 Meter");windowSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){windowLabel.setText("Verbrauchsfenster: "+(100+n*10)+" m");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        if(tileKey.equals("consumption_500m")){l.addView(windowLabel);l.addView(windowSlider);l.addView(label("Messbereich der Verbrauchsanzeige · 100–1000 m",11,muted()));}else{windowLabel.setVisibility(View.GONE);windowSlider.setVisibility(View.GONE);}
        EditText freeText=null;Button chooseImage=null;Spinner imageMode=null;
        if(tileKey.equals("free_text")){freeText=textField(l,"Freitext (max. 4000 Zeichen)",cell.optString("free_text",""));freeText.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);freeText.setMinLines(3);freeText.setMaxLines(12);freeText.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});}
        if(tileKey.equals("image")){
            chooseImage=button(cell.optString("image_data").isEmpty()?"Bild auswählen":"Bild ersetzen");l.addView(chooseImage);
            imageMode=new Spinner(this);imageMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Ganzes Bild einpassen","Kachel füllen (Zuschneiden)"}));imageMode.setSelection(cell.optInt("image_mode",0));l.addView(imageMode);
        }
        final EditText customText=freeText;final Spinner photoMode=imageMode;
        Spinner arrangement=new Spinner(this);arrangement.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Automatisch","Einzeilig","Untereinander"}));arrangement.setSelection(Math.max(0,Math.min(2,cell.optInt("arrangement",0))));l.addView(arrangement);
        Spinner display=new Spinner(this);display.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Zahl","Balken","Rundinstrument"}));display.setSelection(Math.max(0,Math.min(2,cell.optInt("display",cell.optString("key").equals("speed")?2:cell.optString("key").equals("soc")?1:0))));l.addView(display);
        EditText sweepDegrees=field(l,"Kreisausschnitt in Grad (90–270)",""+Math.max(90,Math.min(270,cell.optInt("gauge_sweep",180))));
        sweepDegrees.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        sweepDegrees.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
        android.widget.SeekBar sweep=new android.widget.SeekBar(this);sweep.setMax(180);sweep.setProgress(Math.max(0,Math.min(180,cell.optInt("gauge_sweep",180)-90)));l.addView(sweep);
        sweep.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){if(user)sweepDegrees.setText(""+(90+n));}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        sweepDegrees.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){try{int degrees=Integer.parseInt(s.toString());if(degrees>=90&&degrees<=270)sweep.setProgress(degrees-90);}catch(NumberFormatException ignored){}}public void afterTextChanged(android.text.Editable e){}});
        TextView rotationLabel=label("Instrument drehen: "+cell.optInt("gauge_rotation",0)+"°",13,muted());l.addView(rotationLabel);
        android.widget.SeekBar rotation=new android.widget.SeekBar(this);rotation.setMax(359);rotation.setProgress(Math.floorMod(cell.optInt("gauge_rotation",0),360));l.addView(rotation);
        rotation.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){rotationLabel.setText("Instrument drehen: "+n+"°");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        boolean isGauge=cell.optInt("display",cell.optString("key").equals("speed")?2:0)==2;
        CheckBox showGaugeValue=new CheckBox(this);showGaugeValue.setText("Messwert im Instrument anzeigen");showGaugeValue.setTextColor(foreground());showGaugeValue.setChecked(cell.optBoolean("gauge_value_visible",true));l.addView(showGaugeValue);
        Spinner gaugeValuePosition=new Spinner(this);gaugeValuePosition.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Zentriert","Weiter oben","Weiter unten"}));gaugeValuePosition.setSelection(Math.max(0,Math.min(2,cell.optInt("gauge_value_position",0))));l.addView(label("Messwertposition",12,muted()));l.addView(gaugeValuePosition);
        sweepDegrees.setVisibility(isGauge?View.VISIBLE:View.GONE);sweep.setVisibility(isGauge?View.VISIBLE:View.GONE);rotationLabel.setVisibility(isGauge?View.VISIBLE:View.GONE);rotation.setVisibility(isGauge?View.VISIBLE:View.GONE);
        showGaugeValue.setVisibility(isGauge?View.VISIBLE:View.GONE);gaugeValuePosition.setVisibility(isGauge?View.VISIBLE:View.GONE);
        display.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){sweepDegrees.setVisibility(pos==2?View.VISIBLE:View.GONE);sweep.setVisibility(pos==2?View.VISIBLE:View.GONE);rotationLabel.setVisibility(pos==2?View.VISIBLE:View.GONE);rotation.setVisibility(pos==2?View.VISIBLE:View.GONE);showGaugeValue.setVisibility(pos==2?View.VISIBLE:View.GONE);gaugeValuePosition.setVisibility(pos==2?View.VISIBLE:View.GONE);}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        if(personal){arrangement.setVisibility(View.GONE);display.setVisibility(View.GONE);}
        l.addView(label("Kachelkopf: Symbol / Beschriftung",13,muted()));
        Spinner heading=new Spinner(this);heading.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Nur Symbol","Nur Beschriftung","Symbol und Beschriftung","Beides ausblenden"}));heading.setSelection(CockpitSymbols.mode(cell));l.addView(heading);
        CheckBox allowOverlap=new CheckBox(this);allowOverlap.setText("Überlappung mit anderen Kacheln zulassen");allowOverlap.setTextColor(foreground());allowOverlap.setChecked(cell.optBoolean("allow_overlap",false));l.addView(allowOverlap);
        l.addView(label("Ebene (unten → oben)",13,muted()));Spinner layer=new Spinner(this);String[] layers=new String[boardTiles.length()];for(int n=0;n<layers.length;n++)layers[n]="Ebene "+(n+1)+(n==0?" · unten":n==layers.length-1?" · oben":"");layer.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,layers));layer.setSelection(index);l.addView(layer);
        CheckBox showNote=new CheckBox(this);showNote.setText("Zusatztext anzeigen");showNote.setTextColor(foreground());showNote.setChecked(cell.optBoolean("show_note",true));l.addView(showNote);
        EditText lines=field(l,"Überschrift / Button: maximal 1–3 Zeilen",""+cell.optInt("lines",2));
        EditText scale=field(l,"Skalenmaximum (Balken / Rundinstrument)",""+cell.optDouble("scale_max",cell.optString("key").equals("speed")?CockpitLayout.DEFAULT_SPEED_SCALE_MAX:cell.optString("key").contains("power")?1400:100));
        EditText font=field(l,"Schriftgröße (12–80)",""+cell.optInt("font",28));
        boolean hasUnit=!personal&&!tileKey.startsWith("ready_")&&!tileKey.equals("trip_end")&&!tileKey.equals("log")&&!tileKey.equals("bms_output");
        EditText unitFont=hasUnit?field(l,"Einheit: Schriftgröße (8–80, leer = wie Wert)",cell.has("unit_font")?""+cell.optInt("unit_font"):""):new EditText(this);
        Spinner unitPosition=new Spinner(this);unitPosition.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Neben dem Wert","Hochgestellt","Über dem Wert","Unter dem Wert / im Tacho"}));unitPosition.setSelection(Math.max(0,Math.min(3,cell.optInt("unit_position",0))));
        if(hasUnit){l.addView(label("Einheit: Position",12,muted()));l.addView(unitPosition);}
        EditText x=field(l,"Spalte (0–23)",""+cell.optInt("x")),y=field(l,"Zeile im Raster (0–600)",""+cell.optInt("y"));
        EditText w=field(l,"Breite (1–24 Rasterspalten)",""+cell.optInt("w")),h=field(l,"Höhe (2–24, je 20 dp)",""+cell.optInt("h"));
        EditText bg=textField(l,"Hintergrund (#RRGGBB)",cell.optBoolean("custom_colors",false)?cell.optString("background"):prefs.getString("tile_background",CockpitTheme.light(prefs)?"#F3F7FC":"#141B22"));
        EditText instrument=personal?new EditText(this):textField(l,"Instrument / Balken: Farbe (#RRGGBB)",cell.optString("instrument_color",prefs.getString("accent_color","#FF9800")));
        EditText track=personal?new EditText(this):textField(l,"Skala / Hintergrundbogen: Farbe (#RRGGBB)",cell.optString("scale_color",CockpitTheme.scale(prefs)));
        CheckBox reverseScale=new CheckBox(this);reverseScale.setText("Skalenrichtung umkehren");reverseScale.setTextColor(foreground());reverseScale.setChecked(cell.optBoolean("scale_reverse",false));reverseScale.setVisibility(!personal&&display.getSelectedItemPosition()!=0?View.VISIBLE:View.GONE);l.addView(reverseScale);TextView reverseScaleHint=label("Hohe Werte liegen dann am Skalenanfang; Messwert und Zahl bleiben unverändert.",11,muted());reverseScaleHint.setVisibility(reverseScale.getVisibility());l.addView(reverseScaleHint);
        LinearLayout gradientPanel=column();l.addView(gradientPanel);
        gradientPanel.addView(label("Farbverlauf für Balken und Rundinstrument",13,muted()));
        Spinner gradientMode=new Spinner(this);gradientMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Einfarbig","Zweifarbig","Dreifarbig"}));gradientMode.setSelection(Math.max(0,Math.min(2,cell.optInt("scale_gradient_mode",0))));gradientPanel.addView(gradientMode);
        EditText gradientEnd=textField(gradientPanel,"Endfarbe am Skalenende / hoher Wert (#RRGGBB)",cell.optString("scale_gradient_high","#34C759"));
        EditText gradientMiddle=textField(gradientPanel,"Mittelfarbe (#RRGGBB)",cell.optString("scale_gradient_mid","#FF9800"));
        EditText gradientStart=textField(gradientPanel,"Startfarbe am Skalenanfang / niedriger Wert (#RRGGBB)",cell.optString("scale_gradient_low","#FF3B30"));
        TextView highFullLabel=label("Endfarbe gilt ab: "+cell.optInt("scale_gradient_high_full",80)+" %",12,muted());gradientPanel.addView(highFullLabel);
        android.widget.SeekBar highFull=new android.widget.SeekBar(this);highFull.setMax(100);highFull.setProgress(Math.max(0,Math.min(100,cell.optInt("scale_gradient_high_full",80))));gradientPanel.addView(highFull);
        highFull.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){highFullLabel.setText("Endfarbe gilt ab: "+n+" %");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        TextView lowFullLabel=label("Startfarbe gilt bis: "+cell.optInt("scale_gradient_low_full",20)+" %",12,muted());gradientPanel.addView(lowFullLabel);
        android.widget.SeekBar lowFull=new android.widget.SeekBar(this);lowFull.setMax(100);lowFull.setProgress(Math.max(0,Math.min(100,cell.optInt("scale_gradient_low_full",20))));gradientPanel.addView(lowFull);
        lowFull.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){lowFullLabel.setText("Startfarbe gilt bis: "+n+" %");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        gradientPanel.addView(label("Zwischen den Schwellen ändert sich die Skalenfarbe stufenlos.",11,muted()));
        Runnable updateGradientVisibility=()->{boolean visible=!personal&&display.getSelectedItemPosition()!=0;gradientPanel.setVisibility(visible?View.VISIBLE:View.GONE);int mode=gradientMode.getSelectedItemPosition();gradientMiddle.setVisibility(visible&&mode==2?View.VISIBLE:View.GONE);};
        gradientMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateGradientVisibility.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        display.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){sweepDegrees.setVisibility(pos==2?View.VISIBLE:View.GONE);sweep.setVisibility(pos==2?View.VISIBLE:View.GONE);rotationLabel.setVisibility(pos==2?View.VISIBLE:View.GONE);rotation.setVisibility(pos==2?View.VISIBLE:View.GONE);showGaugeValue.setVisibility(pos==2?View.VISIBLE:View.GONE);gaugeValuePosition.setVisibility(pos==2?View.VISIBLE:View.GONE);reverseScale.setVisibility(!personal&&pos!=0?View.VISIBLE:View.GONE);reverseScaleHint.setVisibility(reverseScale.getVisibility());updateGradientVisibility.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        updateGradientVisibility.run();
        if(personal){instrument.setText(cell.optString("instrument_color",prefs.getString("accent_color","#FF9800")));track.setText(cell.optString("scale_color",CockpitTheme.scale(prefs)));}
        EditText icon=textField(l,"Symbolfarbe (#RRGGBB)",cell.optString("icon_color",String.format(Locale.ROOT,"#%06X",iconColor(cell)&0xffffff)));
        EditText fg=textField(l,"Textfarbe (#RRGGBB)",cell.optBoolean("custom_colors",false)?cell.optString("text"):prefs.getString("tile_text",CockpitTheme.light(prefs)?"#172B40":"#FFFFFF"));
        EditText borderColor=textField(l,"Rahmenfarbe (#RRGGBB)",cell.optString("border_color",prefs.getString("outline_color",CockpitTheme.light(prefs)?"#D5DFEB":"#2C3843")));
        CheckBox borderEnabled=new CheckBox(this);borderEnabled.setText("Kachelrahmen anzeigen");borderEnabled.setTextColor(foreground());borderEnabled.setChecked(cell.optBoolean("border_enabled",true));l.addView(borderEnabled);
        TextView radiusLabel=label("Eckenradius: "+cell.optInt("corner_radius",12)+" dp",13,muted());l.addView(radiusLabel);android.widget.SeekBar radius=new android.widget.SeekBar(this);radius.setMax(48);radius.setProgress(cell.optInt("corner_radius",12));l.addView(radius);radius.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){radiusLabel.setText("Eckenradius: "+n+" dp");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        TextView insetLabel=label("Inhalt-Abstand zum Rahmen: "+cell.optInt("content_inset",6)+" dp",13,muted());l.addView(insetLabel);android.widget.SeekBar inset=new android.widget.SeekBar(this);inset.setMax(32);inset.setProgress(cell.optInt("content_inset",6));l.addView(inset);inset.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int n,boolean user){insetLabel.setText("Inhalt-Abstand zum Rahmen: "+n+" dp");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        android.widget.SeekBar bgAlpha=opacity(l,"Hintergrund",bg.getText().toString());
        android.widget.SeekBar fgAlpha=opacity(l,"Text",fg.getText().toString());
        ScrollView sv=new ScrollView(this);sv.addView(l);
        LinearLayout content=column();content.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);Button apply=button("Übernehmen"),back=button("Zurück"),remove=button("Entfernen");
        for(Button action:new Button[]{apply,back,remove}){action.setSingleLine(true);action.setMinWidth(0);action.setPadding(dp(2),0,dp(2),0);action.setAutoSizeTextTypeUniformWithConfiguration(9,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);actions.addView(action,new LinearLayout.LayoutParams(0,dp(52),1));}content.addView(actions);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(CockpitLayout.title(cell.optString("key"))).setView(content).create();
        tileDialog=dialog;
        back.setOnClickListener(v->dialog.dismiss());remove.setOnClickListener(v->{if(boardTiles.length()>1){boardTiles.remove(index);dialog.dismiss();rebuild();}});
        apply.setOnClickListener(v->{boolean customUnit=hasUnit&&!unitFont.getText().toString().trim().isEmpty();Double uu=customUnit?valid(unitFont,8,80):null;Double ff=valid(font,12,80),xx=valid(x,0,23),yy=valid(y,0,600),ww=valid(w,1,24),hh=valid(h,2,24),angle=valid(sweepDegrees,90,270);Double ll=valid(lines,1,3),ss=valid(scale,.1,10000000);int gradientChoice=gradientMode.getSelectedItemPosition(),lowPct=lowFull.getProgress(),highPct=highFull.getProgress(),windowMeters=100+windowSlider.getProgress()*10;if(angle!=null&&angle!=Math.rint(angle)){sweepDegrees.setError("Bitte eine ganze Gradzahl eingeben");angle=null;}if((customUnit&&uu==null)||ff==null||xx==null||yy==null||ww==null||hh==null||angle==null||ll==null||ss==null||!validColor(bg)||!validColor(fg)||!validColor(instrument)||!validColor(track)||!validColor(icon)||!validColor(borderColor)||(gradientChoice>0&&(!validColor(gradientEnd)||!validColor(gradientMiddle)||!validColor(gradientStart))))return;if(gradientChoice>0&&lowPct>=highPct){lowFullLabel.setError("Muss unter der hohen Schwelle liegen");highFullLabel.setError("Muss über der niedrigen Schwelle liegen");return;}
            JSONArray before;try{before=new JSONArray(boardTiles.toString());boolean oldOverlap=cell.optBoolean("allow_overlap",false);cell.put("allow_overlap",allowOverlap.isChecked()).put("x",xx.intValue()).put("y",yy.intValue()).put("w",ww.intValue()).put("h",hh.intValue());try{board.push(cell);CockpitBoard.validate(boardTiles);}catch(Exception e){for(int i=0;i<boardTiles.length();i++){JSONObject target=boardTiles.getJSONObject(i),original=before.getJSONObject(i);target.put("x",original.getInt("x")).put("y",original.getInt("y")).put("w",original.getInt("w")).put("h",original.getInt("h"));}cell.put("allow_overlap",oldOverlap);Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return;}
                String nextCaption=caption.getText().toString();if(tileKey.equals("consumption_500m")&&(nextCaption.isEmpty()||nextCaption.matches("Verbrauch · letzte \\d+ m")||nextCaption.equals(CockpitLayout.title(tileKey))))nextCaption="Verbrauch · letzte "+windowMeters+" m";
                cell.put("arrangement",arrangement.getSelectedItemPosition()).put("display",display.getSelectedItemPosition()).put("gauge_sweep",angle.intValue()).put("heading_mode",heading.getSelectedItemPosition()).put("show_title",heading.getSelectedItemPosition()==1||heading.getSelectedItemPosition()==2).put("show_note",showNote.isChecked()).put("lines",ll.intValue()).put("scale_max",ss).put("caption",nextCaption).put("font",ff.intValue()).put("background",alphaColor(bg.getText().toString(),bgAlpha.getProgress())).put("text",alphaColor(fg.getText().toString(),fgAlpha.getProgress())).put("custom_colors",true).put("instrument_color",instrument.getText().toString()).put("scale_color",track.getText().toString()).put("icon_color",icon.getText().toString()).put("border_color",borderColor.getText().toString()).put("border_enabled",borderEnabled.isChecked()).put("corner_radius",radius.getProgress()).put("content_inset",inset.getProgress()).put("scale_gradient_mode",gradientChoice).put("scale_reverse",reverseScale.isChecked());if(gradientChoice>0)cell.put("scale_gradient_high",gradientEnd.getText().toString()).put("scale_gradient_mid",gradientMiddle.getText().toString()).put("scale_gradient_low",gradientStart.getText().toString()).put("scale_gradient_low_full",lowPct).put("scale_gradient_high_full",highPct);if(tileKey.equals("consumption_500m"))prefs.edit().putInt("consumption_window_m",windowMeters).apply();if(display.getSelectedItemPosition()==2){cell.put("gauge_sweep",angle.intValue()).put("gauge_rotation",rotation.getProgress()).put("gauge_value_visible",showGaugeValue.isChecked()).put("gauge_value_position",gaugeValuePosition.getSelectedItemPosition());}if(hasUnit){cell.put("unit_position",unitPosition.getSelectedItemPosition());if(customUnit)cell.put("unit_font",uu.intValue());else cell.remove("unit_font");}if(customText!=null)cell.put("free_text",customText.getText().toString());if(photoMode!=null)cell.put("image_mode",photoMode.getSelectedItemPosition());moveTileToLayer(index,layer.getSelectedItemPosition());dialog.dismiss();rebuild();
            }catch(Exception e){Toast.makeText(this,"Kachel konnte nicht geändert werden",Toast.LENGTH_SHORT).show();}});dialog.show();
        dialog.getWindow().setLayout(-1,Math.round(getResources().getDisplayMetrics().heightPixels*.9f));
        if(chooseImage!=null)chooseImage.setOnClickListener(v->{apply.performClick();if(dialog.isShowing())return;try{
            if(!cell.has("tile_id"))cell.put("tile_id",UUID.randomUUID().toString());pendingImageTile=cell.getString("tile_id");pendingImageLayout=boardKey();
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),93);
        }catch(Exception e){Toast.makeText(this,"Bildauswahl nicht verfügbar",Toast.LENGTH_LONG).show();}});
    }
    private void moveTileToLayer(int from,int to){
        if(from==to||from<0||from>=boardTiles.length())return;
        try{java.util.ArrayList<JSONObject> ordered=new java.util.ArrayList<>();for(int i=0;i<boardTiles.length();i++)ordered.add(boardTiles.getJSONObject(i));JSONObject tile=ordered.remove(from);ordered.add(Math.max(0,Math.min(ordered.size(),to)),tile);JSONArray result=new JSONArray();for(JSONObject item:ordered)result.put(item);boardTiles=result;}catch(Exception ignored){}
    }
    private android.widget.SeekBar opacity(LinearLayout parent,String name,String color){
        TextView value=label(name+" – Transparenz",12,muted());parent.addView(value);
        android.widget.SeekBar slider=new android.widget.SeekBar(this);slider.setMax(100);slider.setProgress(Math.round((255-Color.alpha(Color.parseColor(color)))*100f/255));parent.addView(slider);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar s,int n,boolean user){value.setText(name+" – Transparenz: "+n+" %");}public void onStartTrackingTouch(android.widget.SeekBar s){}public void onStopTrackingTouch(android.widget.SeekBar s){}});
        value.setText(name+" – Transparenz: "+slider.getProgress()+" %");return slider;
    }
    private String alphaColor(String color,int transparent){return String.format(Locale.ROOT,"#%08X",(Color.parseColor(color)&0xffffff)|(Math.round((100-transparent)*255f/100)<<24));}
    private String f(double n,String format){return Double.isFinite(n)?String.format(Locale.GERMANY,format,n):"–";}
    private void renderStatus(Intent intent){
        String activeProfile=prefs.getString(ScooterProfiles.ACTIVE,"");
        if(!activeProfile.equals(displayedProfile)&&!editingBoard){displayedProfile=activeProfile;boardTiles=loadBoard();lastStatus=new Intent(intent);rebuild();return;}
        String statusProfile=intent.getStringExtra("scooter_id");if(statusProfile!=null&&!statusProfile.equals(activeProfile))return;
        lastStatus=new Intent(intent);
        boolean trip=intent.getBooleanExtra("trip_active",false),paused=intent.getBooleanExtra("trip_paused",false);
        stateView.setTextColor(foreground());updateStateDot();
        renderDischarge(intent);
        stateView.setText(!BmsMonitorService.running?"Bereitschaft aus":trip?(paused?"Bereitschaft aktiv · Fahrt pausiert":"Bereitschaft aktiv · Fahrt läuft"):"Bereitschaft aktiv");
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
        int consumptionWindow=intent.getIntExtra("consumption_window_m",prefs.getInt("consumption_window_m",500));
        boolean bmsFresh=BmsMonitorService.running && packet>0 && intent.getBooleanExtra("bms_connected",false) && now-packet<=8000;
        boolean gpsFresh=BmsMonitorService.running && trip && gps>0 && now-gps<=8000;
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
                case "speed":value=gpsFresh?f(intent.getDoubleExtra("speed_kmh",0),"%.1f"):"–";note="km/h";break;
                case "soc":int soc=intent.getIntExtra("soc",-1);value=soc<0?"–":soc+" %";break;
                case "range":value=f(intent.getDoubleExtra("range_km",Double.NaN),"%.1f km");note=(intent.getBooleanExtra("range_recent",false)?"jüngster Fahrtverbrauch":"Startschätzung")+" · "+prefs.getString("reserve_percent","10")+" % Reserve";break;
                case "distance":value=f(distance,"%.2f km");break;
                case "total":value=f(numberPref("total_km"),"%.2f km");break;
                case "daily":value=f(DistanceCounters.daily(prefs,now),"%.2f km");note="Heute";break;
                case "tour":value=f(DistanceCounters.number(prefs,"tour_km"),"%.2f km");note="Manuell zurücksetzbar";break;
                case "power":value=packet==0?"–":f(intent.getDoubleExtra("discharge_watts",0),"%.0f W");break;
                case "voltage":value=packet==0?"–":f(intent.getDoubleExtra("voltage",0),"%.2f V");break;
                case "current":value=packet==0?"–":f(intent.getDoubleExtra("current",0),"%.2f A");break;
                case "energy":value=f(intent.getDoubleExtra("energy_wh",0),"%.1f Wh");break;
                case "consumption":value=f(intent.getDoubleExtra("wh_km",Double.NaN),"%.1f Wh/km");break;
                case "consumption_500m":value=f(intent.getDoubleExtra("wh_500m",Double.NaN),"%.1f Wh/km");note="letzte "+consumptionWindow+" m";break;
                case "trip_time":value=formatDuration(intent.getLongExtra("trip_time_ms",intent.getLongExtra("moving_ms",0)+intent.getLongExtra("standing_ms",0)));note="Fahrzeit + Standzeit";break;
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
            else if(!Arrays.asList("total","daily","tour").contains(key))inactive|=!BmsMonitorService.running||!trip;
            ((MetricTile)tileValues.get(i)).reading(value,note,inactive);tileNotes.get(i).setText(note);
        }
    }
    private void renderDischarge(Intent intent){
        if(dischargeSwitch==null)return;
        long at=intent.getLongExtra("bms_at",0);
        boolean fresh=BmsMonitorService.running && intent.getBooleanExtra("bms_connected",false) && at>0 && System.currentTimeMillis()-at<4000;
        boolean pending=intent.getBooleanExtra("control_pending",false),on=intent.getBooleanExtra("discharge_enabled",false);
        int tint=!fresh || pending?muted():on?GREEN:RED;
        updatingDischarge=true;dischargeSwitch.setChecked(fresh && on);updatingDischarge=false;
        dischargeSwitch.setTextColor(tint);dischargeSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(tint));
        dischargeSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(tint));
        dischargeSwitch.setEnabled(fresh && !pending);
        String message=intent.getStringExtra("control_message");
        dischargeNote.setText(pending?"Schaltbefehl gesendet · Bestätigung wird geprüft":!fresh?"Bereitschaft starten / auf frische BMS-Daten warten":message==null||message.startsWith("Bereitschaft starten")?"Wegfahrsperre · nur im Stand schalten":message);
    }
    private void refreshTemperature(){
        if(!BmsMonitorService.running){Toast.makeText(this,"Bereitschaft starten, um aktuelle Temperaturwerte abzurufen",Toast.LENGTH_SHORT).show();return;}
        startService(new Intent(this,BmsMonitorService.class).setAction(BmsMonitorService.ACTION_REFRESH_TEMPERATURE));
        Toast.makeText(this,"Temperaturwerte werden aktualisiert",Toast.LENGTH_SHORT).show();
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
        String[] items={"Cockpit bearbeiten","App-Farben","Gesamtkilometer korrigieren","BMS und Fahrt-Erkennung","Akku und Restreichweite","Benachrichtigungen","Außentemperatur","Letzte Fahrt / Export","Akkuoptimierung","Speicherorte","Scooter-Profile","Temperatur-Beschriftungen","Fahrtenbuch","Tourenzähler zurücksetzen","Routine-Nachrichten bearbeiten"};
        if(settingsDialog!=null&&settingsDialog.isShowing())return;
        settingsDialog=new AlertDialog.Builder(this).setTitle("Einstellungen").setItems(items,null).setNegativeButton("Schließen",null).create();
        settingsDialog.show();
        settingsDialog.getListView().setVerticalScrollBarEnabled(true);
        settingsDialog.getListView().setOnItemClickListener((parent,view,w,id)->{
            switch(w){case 0:editorFromSettings=true;settingsDialog.dismiss();editLayout();break;case 1:editColors();break;case 2:editOdometer();break;
                case 3:editBms();break;case 4:editBattery();break;case 5:notificationSettings();break;
                case 6:weatherSettings();break;case 7:tripDialog();break;case 8:requestBatteryExemption();break;case 9:storageSettings();break;case 10:profileMenu();break;case 11:editTemperatures();break;case 12:journalAfterImport=new JournalUi(this,prefs);journalAfterImport.show();break;case 13:resetTour();break;case 14:editRoutineMessages();break;}
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
        if(copy)l.addView(label("Übernimmt Design und Akku-Einstellungen. BMS, Kilometer, Fahrten und Verbrauchshistorie beginnen leer.",13,muted()));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(rename?"Profil umbenennen":"Neuer Scooter").setView(scrollMenu(l)).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{if(rename)ScooterProfiles.rename(prefs,name.getText().toString());else ScooterProfiles.add(prefs,name.getText().toString(),copy);dialog.dismiss();changedProfile();}catch(Exception e){name.setError(e.getMessage());}}));dialog.show();limitDialogHeight(dialog,.86f);
    }
    private void editTemperatures(){
        LinearLayout l=column();l.addView(label("Sensorreihenfolge unverändert. Temp1 = Platine und Temp2 = Akkupack ist bei deinem BMS bisher eine Vermutung. Eigene Kachelbeschriftungen haben Vorrang.",13,muted()));
        EditText first=textField(l,"Temp1-Beschriftung",prefs.getString("temp1_label","Temp1")),second=textField(l,"Temp2-Beschriftung",prefs.getString("temp2_label","Temp2"));first.setSingleLine(true);second.setSingleLine(true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Temperatursensoren").setView(scrollMenu(l)).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(first.getText().toString().trim().isEmpty()||second.getText().toString().trim().isEmpty()||first.length()>40||second.length()>40){first.setError("Jeweils 1–40 Zeichen");return;}prefs.edit().putString("temp1_label",first.getText().toString().trim()).putString("temp2_label",second.getText().toString().trim()).apply();dialog.dismiss();rebuild();}));dialog.show();limitDialogHeight(dialog,.86f);
    }
    private void addTileMenu(){
        String[] titles=Arrays.copyOf(CockpitLayout.TITLES,CockpitLayout.TITLES.length+1);titles[titles.length-1]="BMS-Daten (optional)";
        new AlertDialog.Builder(this).setTitle("Kachel hinzufügen").setItems(titles,(d,i)->{if(i<CockpitLayout.KEYS.length)addTile(CockpitLayout.KEYS[i]);else optionalTiles();}).setNegativeButton("Zurück",null).show();
    }
    private void addTile(String key){
        if(boardTiles.length()>=100){Toast.makeText(this,"Maximal 100 Kacheln",Toast.LENGTH_LONG).show();return;}
        if(Arrays.asList("bms_output","log").contains(key))for(int i=0;i<boardTiles.length();i++)if(boardTiles.optJSONObject(i).optString("key").equals(key)){Toast.makeText(this,"Diese Kachel ist bereits vorhanden",Toast.LENGTH_SHORT).show();return;}
        try{boardTiles.put(CockpitBoard.position(CockpitLayout.tile(key).put("heading_mode",2).put("show_title",true),0,board.bottom(),6,3));rebuild();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void optionalTiles(){
        ArrayList<String> keys=new ArrayList<>();try{JSONArray available=new JSONArray(prefs.getString("bms_available","[]"));for(int i=0;i<available.length();i++){String key=available.getString(i);if(BmsExtras.known(key))keys.add(key);}}catch(Exception ignored){}
        keys.sort(Comparator.comparing(CockpitLayout::title));
        if(keys.isEmpty()){new AlertDialog.Builder(this).setTitle("Optionale BMS-Daten").setMessage("Für dieses Profil wurden noch keine Zusatzdaten erkannt. BMS auswählen und Bereitschaft aktivieren; nach einer Verbindung erneut öffnen.").setPositiveButton("OK",null).show();return;}
        String[] names=new String[keys.size()];for(int i=0;i<names.length;i++)names[i]=CockpitLayout.title(keys.get(i));
        new AlertDialog.Builder(this).setTitle("Erkannte BMS-Daten dieses Scooters").setItems(names,(d,i)->addTile(keys.get(i))).setNegativeButton("Zurück",null).show();
    }

    private void resetTour(){String profile=prefs.getString(ScooterProfiles.ACTIVE,"");tourDialog=new AlertDialog.Builder(this).setTitle("Tourenzähler zurücksetzen?").setMessage("Nur der Tourenzähler für "+ScooterProfiles.name(prefs)+" wird auf 0 gesetzt. Tages- und Gesamtkilometer bleiben erhalten.")
        .setPositiveButton("Zurücksetzen",(d,w)->{if(!profile.equals(prefs.getString(ScooterProfiles.ACTIVE,""))){Toast.makeText(this,"Scooter wurde gewechselt. Tourenzähler erneut öffnen.",Toast.LENGTH_LONG).show();return;}prefs.edit().putString("tour_km","0").apply();if(lastStatus!=null)renderStatus(lastStatus);else buildTiles();}).setNegativeButton("Zurück",null).show();}
    private void editRoutineMessages(){
        LinearLayout l=column();String[] keys={"routine_start_title","routine_start_text","routine_end_title","routine_end_text"};
        String[] names={"Fahrtstart: Titel","Fahrtstart: Text (leer = Standard)","Fahrtende: Titel","Fahrtende: Text (leer = Fahrtwerte)"};String[] defaults={"T6E Fahrt gestartet","","T6E Fahrt beendet",""};
        java.util.List<EditText> fields=new ArrayList<>();for(int i=0;i<keys.length;i++){EditText e=textField(l,names[i],prefs.getString(keys[i],defaults[i]));e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(i%2==0?100:1000)});fields.add(e);}
        l.addView(label("Platzhalter im Text: {scooter}, {km}, {wh}. Wenn du den von einer Samsung-Routine gesuchten Titel oder Text änderst, passe auch die Routine an. Die Nachrichten gelten für dieses Scooter-Profil.",13,muted()));
        ScrollView scroll=new ScrollView(this);scroll.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Routine-Nachrichten").setView(scroll).setPositiveButton("Speichern",null).setNeutralButton("Standard",(a,w)->{SharedPreferences.Editor e=prefs.edit();for(String k:keys)e.remove(k);e.apply();}).setNegativeButton("Zurück",null).create();
        d.setOnShowListener(a->d.getButton(-1).setOnClickListener(v->{if(fields.get(0).getText().toString().trim().isEmpty()||fields.get(2).getText().toString().trim().isEmpty()){fields.get(0).setError("Titel dürfen nicht leer sein");return;}SharedPreferences.Editor e=prefs.edit();for(int i=0;i<keys.length;i++)e.putString(keys[i],fields.get(i).getText().toString());e.apply();d.dismiss();}));d.show();
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
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Gesamtkilometer korrigieren").setView(scrollMenu(l)).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            Double n=valid(km,0,10000000);if(n==null)return;prefs.edit().putString("total_km",n.toString()).apply();if(lastStatus!=null)renderStatus(lastStatus);else buildTiles();d.dismiss();}));d.show();limitDialogHeight(d,.86f);}
    private void editColors(){
        LinearLayout l=column();Button dark=button("Dunkel · Schwarz / Orange"),light=button("Hell · Weiß / Blau");l.addView(dark);l.addView(light);dark.setOnClickListener(v->chooseTheme(false));light.setOnClickListener(v->chooseTheme(true));
        EditText bg=textField(l,"App-Hintergrund (#RRGGBB)",prefs.getString("app_background","#0B1015"));
        EditText ac=textField(l,"Akzentfarbe (#RRGGBB)",prefs.getString("accent_color","#FF9800"));
        EditText hc=textField(l,"Appkopf-Farbe (#RRGGBB)",prefs.getString("header_color",prefs.getString("accent_color","#FF9800")));
        EditText tileBg=textField(l,"Kachelhintergrund (#RRGGBB)",prefs.getString("tile_background",CockpitTheme.light(prefs)?"#F3F7FC":"#141B22"));
        EditText sc=textField(l,"Skalenfarbe (#RRGGBB)",CockpitTheme.scale(prefs));EditText outline=textField(l,"Trennlinien / Kachelrahmen (#RRGGBB)",prefs.getString("outline_color",CockpitTheme.light(prefs)?"#D5DFEB":"#2C3843"));
        EditText tileText=textField(l,"Kacheltext (#RRGGBB)",prefs.getString("tile_text",CockpitTheme.light(prefs)?"#172B40":"#FFFFFF"));
        CheckBox gradient=new CheckBox(this);gradient.setText("Farbverlauf für App-Hintergrund verwenden");gradient.setTextColor(foreground());gradient.setChecked(prefs.getBoolean("background_gradient_enabled",false));l.addView(gradient);
        EditText gradientStart=textField(l,"Verlauf Anfang (#RRGGBB)",prefs.getString("background_gradient_start",prefs.getString("app_background","#0B1015")));
        EditText gradientEnd=textField(l,"Verlauf Ende (#RRGGBB)",prefs.getString("background_gradient_end",CockpitTheme.light(prefs)?"#DCEBFA":"#26384A"));
        l.addView(label("Einzeln eingefärbte Kacheln behalten ihre eigenen Farben.",12,muted()));
        ScrollView sv=new ScrollView(this);sv.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("App-Farben").setView(sv).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();
        colorDialog=d;d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            if(!validColor(bg)||!validColor(ac)||!validColor(hc)||!validColor(tileBg)||!validColor(tileText)||!validColor(sc)||!validColor(outline)||!validColor(gradientStart)||!validColor(gradientEnd))return;
            prefs.edit().putString("app_background",bg.getText().toString()).putString("accent_color",ac.getText().toString())
                .putString("header_color",hc.getText().toString()).putString("tile_background",tileBg.getText().toString()).putString("tile_text",tileText.getText().toString()).putString("scale_color",sc.getText().toString()).putString("outline_color",outline.getText().toString()).putBoolean("background_gradient_enabled",gradient.isChecked()).putString("background_gradient_start",gradientStart.getText().toString()).putString("background_gradient_end",gradientEnd.getText().toString()).apply();
            d.dismiss();rebuild();
        }));d.show();
    }
    private void chooseTheme(boolean light){new AlertDialog.Builder(this).setTitle(light?"Helles Farbschema anwenden?":"Dunkles Farbschema anwenden?").setMessage("Nur App-Farben ändert die Grundfarben und erhält eigene Kachelfarben. Alle Kacheln ersetzt auch deren Farben, in Hoch- und Querformat. Transparente Kacheln bleiben transparent; Layout und Inhalte bleiben erhalten.").setPositiveButton("Alle Kacheln",(d,w)->applyTheme(light,true)).setNeutralButton("Nur App-Farben",(d,w)->applyTheme(light,false)).setNegativeButton("Zurück",null).show();}
    private void applyTheme(boolean light,boolean replaceTiles){try{CockpitTheme.apply(prefs,light,replaceTiles);if(settingsDialog!=null)settingsDialog.dismiss();if(colorDialog!=null)colorDialog.dismiss();boardTiles=loadBoard();rebuild();}catch(Exception e){Toast.makeText(this,"Farbschema konnte nicht angewendet werden",Toast.LENGTH_LONG).show();}}
    private boolean validColor(EditText e){try{Color.parseColor(e.getText().toString());return true;}catch(Exception ex){e.setError("Farbe z. B. #FFB300");return false;}}
    private Double valid(EditText e,double min,double max){try{double n=Double.parseDouble(e.getText().toString().replace(',','.'));if(!Double.isFinite(n)||n<min||n>max)throw new Exception();return n;}catch(Exception ex){e.setError("Wert zwischen "+min+" und "+max);return null;}}
    private EditText textField(LinearLayout l,String title,String value){l.addView(label(title,13,muted()));EditText e=new EditText(this);e.setText(value);e.setTextColor(foreground());l.addView(e);
        if(title.contains("#RRGGBB")){
            Button choose=button("●  Farbe auswählen / mischen");l.addView(choose);updateColorButton(choose,value);
            choose.setOnClickListener(v->showColorPicker(e,choose));
        }
        return e;
    }
    private void updateColorButton(Button button,String hex){try{int c=Color.parseColor(hex),swatch=0xff000000|(c&0xffffff);double lum=Color.red(swatch)*.2126+Color.green(swatch)*.7152+Color.blue(swatch)*.0722;button.setText("●  "+String.format(Locale.ROOT,"#%06X",c&0xffffff));button.setTextColor(lum>145?0xff102030:Color.WHITE);button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(swatch));}catch(Exception ignored){}}
    private void showColorPicker(EditText target,Button preview){
        LinearLayout panel=column();TextView sample=panel("Eigene Farbe mischen");panel.addView(sample);
        String[] labels={"Farbton","Sättigung","Helligkeit"};android.widget.SeekBar[] sliders=new android.widget.SeekBar[3];float[] hsv=new float[]{28,100,100};
        try{Color.colorToHSV(Color.parseColor(target.getText().toString()),hsv);}catch(Exception ignored){}
        android.widget.EditText hex=new android.widget.EditText(this);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setText(target.getText());hex.setTextColor(foreground());panel.addView(hex);
        for(int i=0;i<3;i++){final int index=i;int initial=index==0?Math.round(hsv[index]):Math.round(hsv[index]*100);TextView label=label(labels[i]+": "+initial,12,muted());panel.addView(label);android.widget.SeekBar s=new android.widget.SeekBar(this);s.setMax(i==0?360:100);s.setProgress(initial);sliders[i]=s;panel.addView(s);s.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int value,boolean fromUser){hsv[index]=index==0?value:value/100f;label.setText(labels[index]+": "+value);int color=Color.HSVToColor(hsv);sample.setBackgroundColor(color);hex.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff));}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});}
        Runnable sync=()->{try{int color=Color.parseColor(hex.getText().toString());Color.colorToHSV(color,hsv);for(int i=0;i<3;i++)sliders[i].setProgress(i==0?Math.round(hsv[i]):Math.round(hsv[i]*100));sample.setBackgroundColor(color);}catch(Exception ignored){}};hex.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){sync.run();}public void afterTextChanged(android.text.Editable e){}});
        AlertDialog picker=new AlertDialog.Builder(this).setTitle("Farbe auswählen").setView(scrollMenu(panel)).setPositiveButton("Übernehmen",null).setNegativeButton("Zurück",null).create();picker.setOnShowListener(v->picker.getButton(-1).setOnClickListener(b->{try{int c=Color.parseColor(hex.getText().toString());target.setText(String.format(Locale.ROOT,"#%06X",c&0xffffff));updateColorButton(preview,target.getText().toString());picker.dismiss();}catch(Exception ex){hex.setError("Bitte eine gültige Farbe eingeben");}}));picker.show();limitDialogHeight(picker,.72f);
    }
    private void editBattery(){LinearLayout l=column();String[] keys={"capacity_ah","nominal_voltage","reserve_percent","reference_wh_km"};String[] titles={"Kapazität (Ah, Ersatzwert ohne BMS-Kapazität)","Nennspannung (V)","Restreserve (%)","Startwert Verbrauch (Wh/km)"};String[] values={"26","48","10","20"};List<EditText> es=new ArrayList<>();for(int i=0;i<keys.length;i++)es.add(field(l,titles[i],prefs.getString(keys[i],values[i])));
        l.addView(label("Ab 250 m wird der aktuelle Fahrtverbrauch verwendet; die jüngsten ungefähr 800 m werden stärker gewichtet. Reichweite ist eine Schätzung.",12,muted()));
        ScrollView scroll=new ScrollView(this);scroll.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Akku und Restreichweite").setView(scroll).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();batteryConfigDialog=d;d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{double[] mins={0.1,1,0,1},maxs={1000,100,99,200};SharedPreferences.Editor edit=prefs.edit();for(int i=0;i<keys.length;i++){Double n=valid(es.get(i),mins[i],maxs[i]);if(n==null)return;edit.putString(keys[i],n.toString());}edit.apply();d.dismiss();}));d.show();}
    private boolean hasSelectedBms(){return BluetoothAdapter.checkBluetoothAddress(prefs.getString("device_address",""));}
    private void editBms(){LinearLayout l=column();TextView selected=panel(hasSelectedBms()?"Ausgewählt: "+prefs.getString("device_name","BMS")+"\n"+prefs.getString("device_address",""):"Noch kein BMS ausgewählt");l.addView(selected);
        Button scan=button(hasSelectedBms()?"Anderes BMS auswählen":"BMS auswählen");scan.setOnClickListener(v->openBmsPicker());l.addView(scan);
        CheckBox automatic=new CheckBox(this);automatic.setText("Gespeicherte Scooter automatisch erkennen und auswählen");automatic.setTextColor(foreground());automatic.setChecked(prefs.getBoolean("auto_scooter",true));l.addView(automatic);
        String[] keys={"active_current","active_seconds","idle_seconds","monitor_timeout","connect_rssi","departure_rssi","scan_absent","scan_weak","scan_good","scan_pause","departure_seconds","gps_max_kmh","connect_confirm_seconds","stop_seconds","slow_unpowered_stop_seconds","pause_end_seconds","pause_disconnect_seconds"};
        String[] titles={"Fahrt ab Entladestrom (A)","Startverzögerung (s)","Pause ohne Entnahme nach (s)","Ohne Fahrt zurück zur Suche nach (s)","Verbinden ab Empfang (dBm)","Entfernung unter Empfang (dBm)","Suchpause: BMS fehlt (s)","Suchpause: schwacher Empfang (s)","Suchpause: guter Empfang (s)","Wiederverbindung während Fahrtpause (s)","Entfernung bestätigen ohne BMS-Daten (s)","Maximal plausible GPS-Geschwindigkeit (km/h)","Starken Empfang vor Erstverbindung bestätigen (s)","Fahrt pausieren nach Stillstand (s)","Bei langsamer Bewegung ohne Motorlast pausieren (s)","Fahrtende nach langer Pause (s)","Wiederverbindung erlauben bis nach Pausenbeginn (s)"};
        String[] defs={"0.30","1","5","90","-75","-95","5","15","2","2","30","45","3","45","15","600","120"};
        List<EditText> es=new ArrayList<>();for(int i=0;i<keys.length;i++){EditText input=field(l,titles[i],prefs.getString(keys[i],defs[i]));if(i==4||i==5)input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);es.add(input);}
        l.addView(label("Erstverbindung nach mindestens drei stabilen Empfangsmessungen. Automatik prüft nur gespeicherte, eindeutig zugeordnete BMS und hält das Profil während einer Fahrt fest. Ausrollen wird weiter aufgezeichnet. Nach 45 s Stillstand oder 15 s langsamer Bewegung ohne Motorlast pausiert die Fahrtaufzeichnung; Fußwege werden nicht als Fahrstrecke aufgezeichnet. Die BMS-Verbindung bleibt bestehen. Fortsetzen erfordert GPS-Fahrgeschwindigkeit und anhaltende Motorlast. Nach 10 Minuten endet sie rückwirkend ab Pausenbeginn. Bei bestätigter Entfernung wird die Pause ebenfalls als Fahrtende gewertet. GPS-Ausfall zählt nicht als Stillstand. Änderungen der Erkennung gelten nach Neustart der Bereitschaft.",12,muted()));ScrollView sv=new ScrollView(this);sv.addView(l);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("BMS und Fahrt-Erkennung").setView(sv).setPositiveButton("Speichern",null).setNegativeButton("Zurück",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            double[] mins={.01,1,1,1,-110,-120,5,5,1,1,10,10,1,15,5,60,0},maxs={100,3600,3600,3600,-30,-30,3600,3600,3600,60,600,150,15,3600,120,7200,7200};
            Double[] values=new Double[keys.length];for(int i=0;i<keys.length;i++){values[i]=valid(es.get(i),mins[i],maxs[i]);if(values[i]==null)return;}
            if(values[5]>values[4]){es.get(5).setError("Entfernungsschwelle muss gleich oder schwächer als Verbindungsschwelle sein");return;}
            SharedPreferences.Editor edit=prefs.edit();for(int i=0;i<keys.length;i++)edit.putString(keys[i],values[i].toString());edit.putBoolean("auto_scooter",automatic.isChecked()).putString("connection_policy_version","3");edit.apply();d.dismiss();
        }));d.show();}

    private void tripDialog(){LinearLayout l=column();lastTripView=panel("");l.addView(lastTripView);showLastTrip();Button share=button("Letzte GPX- und CSV-Datei teilen");share.setOnClickListener(v->shareLastTrip());l.addView(share);AlertDialog d=new AlertDialog.Builder(this).setTitle("Letzte Fahrt").setView(scrollMenu(l)).setNegativeButton("Schließen",null).create();d.show();limitDialogHeight(d,.86f);}
    private void storageSettings(){
        String settings=prefs.getString(StorageFolders.SETTINGS,""),trips=prefs.getString(StorageFolders.TRIPS,"");
        String status="Einstellungen: "+(settings.isEmpty()?"App-Speicher":prefs.getString(StorageFolders.SETTINGS+"_label",settings))+"\n\nFahrtenbuch: "+(trips.isEmpty()?"App-Speicher":prefs.getString(StorageFolders.TRIPS+"_label",trips));
        String error=prefs.getString("settings_storage_error","")+"\n"+prefs.getString("trip_storage_error","");
        LinearLayout l=column();l.addView(label(status,12,muted()));if(!error.trim().isEmpty())l.addView(label(error.trim(),12,RED));
        Button settingsButton=button("Ordner für Einstellungen wählen"),tripButton=button("Ordner für Fahrtenbuch wählen"),load=button("Einstellungen aus gewähltem Ordner laden"),retry=button("Speichern / ausstehende Kopien erneut versuchen");
        for(Button b:new Button[]{settingsButton,tripButton,load,retry})l.addView(b);
        settingsButton.setOnClickListener(v->chooseFolder(91));tripButton.setOnClickListener(v->chooseFolder(92));
        load.setOnClickListener(v->{if(settings.isEmpty())return;if(BmsMonitorService.running){Toast.makeText(this,"Vor dem Laden Bereitschaft beenden",Toast.LENGTH_LONG).show();return;}
            new AlertDialog.Builder(this).setMessage("Aktuelle Einstellungen durch die Datei im gewählten Ordner ersetzen?").setPositiveButton("Laden",(d,w)->StorageFolders.install(this).run(()->{try{StorageFolders.install(this).importSettings(settings);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},errorMessage->{if(errorMessage==null){boardTiles=loadBoard();rebuild();}Toast.makeText(this,errorMessage==null?"Einstellungen geladen":errorMessage,Toast.LENGTH_LONG).show();})).setNegativeButton("Zurück",null).show();});
        retry.setOnClickListener(v->{StorageFolders.install(this).retryAsync();Toast.makeText(this,"Speicherung wird erneut versucht. Ergebnis hier nach erneutem Öffnen.",Toast.LENGTH_LONG).show();});
        l.addView(label("Fahrten werden während der Aufzeichnung lokal gepuffert und nach Abschluss in den gewählten Ordner kopiert. Ordnerwechsel gilt ab der nächsten Fahrt. Die lokale Kopie bleibt zum Teilen erhalten.",12,muted()));
        storageDialog=new AlertDialog.Builder(this).setTitle("Separate Speicherorte").setView(scrollMenu(l)).setNegativeButton("Schließen",null).create();storageDialog.show();limitDialogHeight(storageDialog,.82f);
    }
    void chooseTripImport(JournalUi journal){
        journalAfterImport=journal;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/comma-separated-values","application/gpx+xml","application/xml","text/xml"}).addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent,94);
    }
    void chooseTripFolderImport(JournalUi journal){
        journalAfterImport=journal;
        new AlertDialog.Builder(this).setTitle("Fahrtenbuch aus Ordner importieren")
            .setMessage("Wähle den ScootPit-Fahrtenordner mit den JSON- und CSV-Dateien deiner Fahrten. Unterordner werden mit durchsucht. Bestehende Fahrten bleiben erhalten; Duplikate werden übersprungen.")
            .setPositiveButton("Ordner wählen",(d,w)->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,95);})
            .setNegativeButton("Zurück",null).show();
    }
    private void importTrip(Uri uri){
        String name="Fahrtdatei";try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}
        final String displayName=name;StorageFolders.install(this).run(()->{try{TripImporter.importFile(this,prefs,uri,displayName);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{if(error==null&&journalAfterImport!=null)journalAfterImport.refreshAfterImport();Toast.makeText(this,error==null?"Fahrt ins Fahrtenbuch importiert":error,Toast.LENGTH_LONG).show();});
    }
    private void importTripFolder(Uri tree){
        TripFolderImporter.Summary[] summary=new TripFolderImporter.Summary[1];StorageFolders.install(this).run(()->{try{summary[0]=TripFolderImporter.importFolder(this,prefs,tree);}catch(Exception e){throw new IllegalStateException(e.getMessage());}},error->{if(error==null&&journalAfterImport!=null)journalAfterImport.refreshAfterImport();Toast.makeText(this,error==null?summary[0].message():error,Toast.LENGTH_LONG).show();});
    }
    private void chooseFolder(int request){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,request);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==93){receiveTileImage(result,data);return;}if(request==94){if(result==RESULT_OK&&data!=null&&data.getData()!=null)importTrip(data.getData());return;}if(request==95){if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri tree=data.getData();try{if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)throw new SecurityException();getContentResolver().takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){Toast.makeText(this,"Lesender Ordnerzugriff fehlgeschlagen",Toast.LENGTH_LONG).show();return;}importTripFolder(tree);}return;}if((request!=91&&request!=92)||result!=RESULT_OK||data==null||data.getData()==null)return;
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
        TextView view = label(value, 15, accent());
        view.setPadding(0, dp(24), 0, dp(7));
        return view;
    }

    private TextView panel(String value) {
        TextView view = label(value, 16, foreground());
        view.setBackgroundColor(CockpitTheme.color(prefs,"tile_background",CockpitTheme.light(prefs)?"#F3F7FC":"#141B22"));
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
        button.setTextColor(foreground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(5), 0, dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private EditText field(LinearLayout root, String title, String value) {
        root.addView(label(title, 13, muted()));
        EditText edit = new EditText(this);
        edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit.setText(value);
        edit.setTextColor(foreground());
        edit.setHintTextColor(muted());
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
        if(!hasSelectedBms()&&(!prefs.getBoolean("auto_scooter",true)||ScooterProfiles.knownBms(prefs).isEmpty())){Toast.makeText(this,"Bitte zuerst ein BMS auswählen",Toast.LENGTH_LONG).show();openBmsPicker();return;}
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


