package de.wortmonster.jbdtrigger;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/** Device smoke check: fresh launch, actual rendered measurements, persistence and editors. */
public class CockpitSmokeTest extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            SharedPreferences prefs=getTargetContext().getSharedPreferences("settings",0);
            prefs.edit().clear().putString("total_km","123.45").commit();
            Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            checked(()->systemInsetsCheck(activity));
            checked(()->{
                if(!contains(activity.getWindow().getDecorView(),"ScootPit BMS"))throw new AssertionError("App name");
                if(!contains(activity.getWindow().getDecorView(),"123,45 km"))throw new AssertionError("Saved odometer before readiness");
                if(prefs.contains("device_address")||!contains(activity.getWindow().getDecorView(),"BMS auswählen"))throw new AssertionError("Fresh installation must require BMS selection");
                try{CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());CockpitBoard.validate(board.tiles);
                    if(board.tiles.length()!=17||board.tiles.getJSONObject(1).getInt("x")!=8||!board.tiles.getJSONObject(0).getString("background").equals("#00000000"))throw new AssertionError("Screenshot default layout");
                    if(board.tiles.getJSONObject(0).getInt("w")!=8||board.tiles.getJSONObject(0).getInt("h")!=6||board.tiles.getJSONObject(3).getInt("x")!=8||board.tiles.getJSONObject(3).getInt("y")!=4||board.tiles.getJSONObject(4).getInt("y")!=6)throw new AssertionError("Large transparent gauge and three stacked readings");
                    if(board.tiles.getJSONObject(0).getDouble("scale_max")!=22)throw new AssertionError("22 km/h default gauge scale");
                    org.json.JSONObject legacyGauge=new org.json.JSONObject().put("key","speed");
                    MetricTile probe=new MetricTile(activity,legacyGauge,0xffffffff,0xffffb300,0xff222222,0);
                    Method progress=MetricTile.class.getDeclaredMethod("progress",String.class);progress.setAccessible(true);
                    if(Math.abs((Double)progress.invoke(probe,"11 km/h")-.5)>.001)throw new AssertionError("Legacy gauge fallback is not 22 km/h");
                    legacyGauge.put("scale_max",40);
                    if(Math.abs((Double)progress.invoke(probe,"11 km/h")-.275)>.001)throw new AssertionError("Custom gauge scale overwritten");
                    CockpitBoard.validate(board.tiles);
                    if(!contains(activity.getWindow().getDecorView(),"An")||!contains(activity.getWindow().getDecorView(),"Aus"))throw new AssertionError("Default readiness labels");
                }catch(Exception e){throw new RuntimeException(e);}
                invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{new Intent()
                    .putExtra("soc",75).putExtra("bms_at",System.currentTimeMillis()).putExtra("bms_connected",true)
                    .putExtra("discharge_enabled",true).putExtra("range_km",24.5).putExtra("speed_kmh",18.2).putExtra("temperatures",new double[]{25,30}).putExtra("max_power_w",980.0)});
                if(!contains(activity.getWindow().getDecorView(),"BMS-Lastausgang"))throw new AssertionError("No control without readiness");
                if(!contains(activity.getWindow().getDecorView(),"25,0 °C")||!contains(activity.getWindow().getDecorView(),"30,0 °C"))throw new AssertionError("Independent temperatures");
                if(!contains(activity.getWindow().getDecorView(),"980 W"))throw new AssertionError("Maximum power");
                if(!contains(activity.getWindow().getDecorView(),"75 %"))throw new AssertionError("SOC");
                if(!contains(activity.getWindow().getDecorView(),"24,5 km"))throw new AssertionError("Range");
            });
            checked(()->{invoke(activity,"startMonitoring",new Class[0],new Object[0]);if(BmsMonitorService.running)throw new AssertionError("Readiness started without BMS");});
            checked(()->{try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("picker");field.setAccessible(true);BmsPicker picker=(BmsPicker)field.get(activity);for(int i=0;i<20;i++)picker.add(String.format(java.util.Locale.US,"02:00:00:00:01:%02X",i),"JBD-Test "+i,-50-i,true);picker.list.setSelection(19);}catch(Exception e){throw new RuntimeException(e);}});
            checked(()->{try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("picker");field.setAccessible(true);BmsPicker picker=(BmsPicker)field.get(activity);if(picker.list.getAdapter().getCount()!=20||picker.list.getLastVisiblePosition()<19)throw new AssertionError("BMS picker cannot scroll past three candidates");}catch(Exception e){throw new RuntimeException(e);}});
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
            checked(()->{
                java.util.concurrent.atomic.AtomicInteger selected=new java.util.concurrent.atomic.AtomicInteger();
                BmsPicker probe=new BmsPicker(activity,(address,name)->{if(!address.equals("02:00:00:00:00:01"))throw new AssertionError("Probe identity");selected.incrementAndGet();});
                try{java.lang.reflect.Field busy=BmsPicker.class.getDeclaredField("busy"),candidate=BmsPicker.class.getDeclaredField("checking");busy.setAccessible(true);candidate.setAccessible(true);busy.set(probe,true);candidate.set(probe,new BmsPicker.Candidate("02:00:00:00:00:01","Test-BMS",-50,true));
                    byte[] frame=new byte[30];frame[0]=(byte)0xdd;frame[1]=3;frame[3]=23;frame[4]=0x13;frame[5]=(byte)0xbf;frame[23]=75;frame[25]=13;frame[29]=0x77;int sum=0;for(int i=2;i<27;i++)sum+=frame[i]&255;int checksum=(-sum)&65535;frame[27]=(byte)(checksum>>8);frame[28]=(byte)checksum;
                    byte[] corrupt=frame.clone();corrupt[5]++;invoke(probe,"receive",new Class[]{byte[].class},new Object[]{corrupt});
                    invoke(probe,"receive",new Class[]{byte[].class},new Object[]{java.util.Arrays.copyOfRange(frame,0,20)});if(selected.get()!=0)throw new AssertionError("Partial/corrupt BMS selected");
                    invoke(probe,"receive",new Class[]{byte[].class},new Object[]{java.util.Arrays.copyOfRange(frame,20,30)});if(selected.get()!=1)throw new AssertionError("Fragmented BMS response not selected");
                }catch(Exception e){throw new RuntimeException(e);}finally{probe.close();}
            });
            checked(()->{
                invoke(activity,"selectBms",new Class[]{String.class,String.class},new Object[]{"02:00:00:00:00:01","Test-BMS"});
                if(!prefs.getString("device_address","").equals("02:00:00:00:00:01")||contains(activity.getWindow().getDecorView(),"BMS auswählen"))throw new AssertionError("Selected BMS not applied");
            });
            customizationCheck(activity,prefs);
            batterySettingsCheck(activity);
            checked(()->unitFontRenderCheck(activity));
            checked(()->{
                try{CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());for(int i=0;i<board.tiles.length();i++){org.json.JSONObject t=board.tiles.getJSONObject(i);if(t.getString("key").equals("soc"))t.put("h",2);if(t.getString("key").equals("ready_start"))t.put("caption","Los geht’s");}prefs.edit().putString("header_name","Mein Joyor").apply();invoke(activity,"rebuild",new Class[0],new Object[0]);if(!contains(activity.getWindow().getDecorView(),"ScootPit BMS")||!contains(activity.getWindow().getDecorView(),"Los geht’s"))throw new AssertionError("Custom header/button labels");
                org.json.JSONArray rides=new org.json.JSONArray();for(int i=0;i<3;i++)rides.put(new org.json.JSONObject().put("temperature",10+i).put("km",5).put("wh",100));TemperatureHistory.validate(rides);if(Math.abs(TemperatureHistory.estimate(rides,11,15)-20)>.01)throw new AssertionError("Temperature start estimate");if(TemperatureHistory.estimate(rides,30,15)!=15)throw new AssertionError("No unsupported extrapolation");
                CockpitBoard b=(CockpitBoard)findBoard(activity.getWindow().getDecorView());org.json.JSONObject top=b.tiles.getJSONObject(0);top.put("h",8);b.push(top);CockpitBoard.validate(b.tiles);if(b.tiles.getJSONObject(4).getInt("y")<8)throw new AssertionError("Automatic displacement");top.put("h",6);b.compact();
                }catch(Exception e){throw new RuntimeException(e);}
            });
            profileCheck(prefs);
            officialVersionCheck(activity,prefs);
            designCheck(activity,prefs);
            checked(()->{
                boolean old=BmsMonitorService.running;
                try{invoke(activity,"addTile",new Class[]{String.class},new Object[]{"bms_cell_delta"});
                    TextView heading=findText(activity.getWindow().getDecorView(),"ScootPit BMS");if(heading==null||heading.getCurrentTextColor()!=android.graphics.Color.parseColor("#42A5F5"))throw new AssertionError("Independent header colour");
                    BmsMonitorService.running=true;long now=System.currentTimeMillis();Intent status=new Intent().putExtra("bms_connected",true).putExtra("bms_at",now).putExtra("bms_values","{\"bms_cell_delta\":\"5 mV\"}").putExtra("bms_times","{\"bms_cell_delta\":"+now+"}");
                    invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{status});MetricTile tile=findMetric(activity.getWindow().getDecorView(),"bms_cell_delta");
                    if(!tile.getText().toString().equals("5 mV")||tile.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Optional BMS tile value/freshness");
                    status.putExtra("bms_times","{\"bms_cell_delta\":"+(now-16000)+"}");invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{status});if(!tile.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Stale optional tile");
                }finally{BmsMonitorService.running=old;}
            });
            storageCheck(prefs);
            checked(()->{
                boolean running=BmsMonitorService.running;
                try{
                    prefs.edit().putString("tile_background","#64B966").apply();invoke(activity,"rebuild",new Class[0],new Object[0]);
                    BmsMonitorService.running=true;
                    Intent fresh=new Intent().putExtra("soc",75).putExtra("range_km",24.5).putExtra("bms_at",System.currentTimeMillis()).putExtra("bms_connected",true).putExtra("trip_active",true).putExtra("gps_at",System.currentTimeMillis()).putExtra("outside_temperature",15.0).putExtra("weather_at",System.currentTimeMillis());
                    invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{fresh});
                    MetricTile soc=findMetric(activity.getWindow().getDecorView(),"soc"),power=findMetric(activity.getWindow().getDecorView(),"power");
                    if(soc.getContentDescription().toString().contains("nicht aktuell")||power.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Fresh zero is active");
                    if(tileBackground(soc)!=android.graphics.Color.parseColor("#64B966"))throw new AssertionError("Fresh custom background");
                    Intent stale=new Intent(fresh).putExtra("bms_at",System.currentTimeMillis()-9000).putExtra("gps_at",System.currentTimeMillis()-9000).putExtra("weather_at",System.currentTimeMillis()-1801000);
                    invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{stale});
                    if(!soc.getText().toString().equals("75 %")||!soc.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Stale reading retained and marked");
                    int bg=tileBackground(soc);if(android.graphics.Color.red(bg)!=android.graphics.Color.green(bg)||android.graphics.Color.green(bg)!=android.graphics.Color.blue(bg))throw new AssertionError("Inactive custom background remains coloured");
                    MetricTile staleGauge=findMetric(activity.getWindow().getDecorView(),"speed");staleGauge.layout(0,0,320,160);Bitmap pixels=Bitmap.createBitmap(320,160,Bitmap.Config.ARGB_8888);staleGauge.draw(new android.graphics.Canvas(pixels));
                    for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){int p=pixels.getPixel(x,y);if(android.graphics.Color.alpha(p)>0&&(android.graphics.Color.red(p)!=android.graphics.Color.green(p)||android.graphics.Color.green(p)!=android.graphics.Color.blue(p)))throw new AssertionError("Inactive text/bar remains coloured");}pixels.recycle();
                    if(!findMetric(activity.getWindow().getDecorView(),"speed").getContentDescription().toString().contains("nicht aktuell")||!findMetric(activity.getWindow().getDecorView(),"outside").getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("GPS/weather freshness");
                    invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{fresh});if(tileBackground(soc)!=android.graphics.Color.parseColor("#64B966")||soc.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Fresh colour restoration");
                    BmsMonitorService.running=false;invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{fresh});
                    if(!soc.getContentDescription().toString().contains("nicht aktuell")||!findMetric(activity.getWindow().getDecorView(),"distance").getContentDescription().toString().contains("nicht aktuell")||findMetric(activity.getWindow().getDecorView(),"total").getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Stopped tracking and odometer");
                    invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{new Intent()});if(!soc.getContentDescription().toString().contains("nicht aktuell"))throw new AssertionError("Missing value");
                }finally{BmsMonitorService.running=running;prefs.edit().remove("tile_background").apply();invoke(activity,"rebuild",new Class[0],new Object[0]);}
            });
            checked(()->invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{new Intent().putExtra("soc",75).putExtra("range_km",24.5).putExtra("speed_kmh",18.2).putExtra("bms_at",System.currentTimeMillis()).putExtra("bms_connected",true).putExtra("temperatures",new double[]{25,30}).putExtra("max_power_w",980.0)}));
            screenshot("cockpit.png");
            checked(()->invoke(activity,"editLayout",new Class[0],new Object[0]));
            screenshot("layout-editor.png");
            checked(()->{
                try{java.lang.reflect.Field tiles=MainActivity.class.getDeclaredField("boardTiles");tiles.setAccessible(true);org.json.JSONArray layout=(org.json.JSONArray)tiles.get(activity);CockpitBoard.validate(layout);CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());layout.getJSONObject(1).put("x",0).put("y",board.bottom()).put("w",6).put("h",2);board.requestLayout();}catch(Exception e){throw new RuntimeException(e);}
            });
            checked(()->{
                try{
                    CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());View overlay=board.getChildAt(3);
                    float x=overlay.getWidth()-4,y=overlay.getHeight()-4;long time=android.os.SystemClock.uptimeMillis();
                    android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,0,x,y,0);overlay.dispatchTouchEvent(down);down.recycle();
                    android.view.MotionEvent move=android.view.MotionEvent.obtain(time,time+20,2,x+board.getWidth()/12f,y+40*getTargetContext().getResources().getDisplayMetrics().density,0);overlay.dispatchTouchEvent(move);move.recycle();
                    android.view.MotionEvent up=android.view.MotionEvent.obtain(time,time+40,1,x+board.getWidth()/12f,y+40*getTargetContext().getResources().getDisplayMetrics().density,0);overlay.dispatchTouchEvent(up);up.recycle();
                    if(board.tiles.getJSONObject(1).getInt("w")!=7 || board.tiles.getJSONObject(1).getInt("h")!=3)throw new AssertionError("Visual tile resize");
                    if(!clickText(activity.getWindow().getDecorView(),"Speichern"))throw new AssertionError("Editor save");CockpitBoard.validate(new org.json.JSONArray(prefs.getString("cockpit_board","")));
                    org.json.JSONObject snapshot=StorageFolders.install(getTargetContext()).snapshot();if(!snapshot.getJSONObject("settings").getString("total_km").equals("123.45"))throw new AssertionError("Settings snapshot");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            checked(()->{
                try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("log");field.setAccessible(true);java.util.ArrayDeque<String> log=(java.util.ArrayDeque<String>)field.get(activity);for(int i=0;i<60;i++)log.add("Logeintrag "+i+" – Test der Scrollbarkeit");invoke(activity,"rebuild",new Class[0],new Object[0]);if(!clickText(activity.getWindow().getDecorView(),"Statusdetails / Log ▸"))throw new AssertionError("Log toggle");}catch(Exception e){throw new RuntimeException(e);}
            });
            Thread.sleep(200);
            checked(()->{androidx.core.widget.NestedScrollView log=findLog(activity.getWindow().getDecorView());if(log==null)throw new AssertionError("Log scroll container");log.scrollTo(0,500);if(log.getScrollY()<=0)throw new AssertionError("Log scrolling");});
            checked(()->{
                try{
                    String portrait=prefs.getString("cockpit_board","");
                    if(!CockpitBoard.load(prefs,"cockpit_board_landscape").toString().equals(portrait))throw new AssertionError("Landscape fallback");
                    org.json.JSONArray landscape=new org.json.JSONArray(portrait);landscape.getJSONObject(0).put("caption","Querformat-Test");
                    prefs.edit().putString("cockpit_board_landscape",landscape.toString()).putString("connect_rssi","-85").putString("departure_rssi","-95").commit();
                    if(!CockpitBoard.load(prefs,"cockpit_board_landscape").getJSONObject(0).getString("caption").equals("Querformat-Test"))throw new AssertionError("Independent landscape");
                    if(!CockpitBoard.load(prefs).toString().equals(portrait))throw new AssertionError("Landscape overwrote portrait");
                    StorageFolders.validateValues(StorageFolders.install(getTargetContext()).snapshot().getJSONObject("settings"),false);
                    invoke(activity,"editLayout",new Class[0],new Object[0]);invoke(activity,"editBoardTile",new Class[]{int.class},new Object[]{1});
                    java.lang.reflect.Field dialogField=MainActivity.class.getDeclaredField("tileDialog");dialogField.setAccessible(true);android.app.AlertDialog dialog=(android.app.AlertDialog)dialogField.get(activity);
                    View apply=findText(dialog.getWindow().getDecorView(),"Übernehmen"),back=findText(dialog.getWindow().getDecorView(),"Zurück"),remove=findText(dialog.getWindow().getDecorView(),"Entfernen");
                    if(apply==null||back==null||remove==null||apply.getParent()!=back.getParent()||apply.getParent()!=remove.getParent())throw new AssertionError("Dialog actions not in one row");
                    org.json.JSONObject tile=((CockpitBoard)findBoard(activity.getWindow().getDecorView())).tiles.getJSONObject(1);
                    java.util.ArrayList<android.widget.SeekBar> sliders=new java.util.ArrayList<>();findSliders(dialog.getWindow().getDecorView(),sliders);
                    if(sliders.size()!=2)throw new AssertionError("Opacity controls");sliders.get(0).setProgress(50);sliders.get(1).setProgress(25);apply.performClick();
                    if(android.graphics.Color.alpha(android.graphics.Color.parseColor(tile.getString("background")))!=128)throw new AssertionError("Tile background alpha");
                    if(android.graphics.Color.alpha(android.graphics.Color.parseColor(tile.getString("text")))!=191)throw new AssertionError("Tile text alpha");
                    clickText(activity.getWindow().getDecorView(),"Zurück");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            String saved=prefs.getString("cockpit_board","");
            checked(()->invoke(activity,"editLayout",new Class[0],new Object[0]));
            final float[] origin=new float[2];
            checked(()->{CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());View overlay=board.getChildAt(3);origin[0]=overlay.getWidth()/2f;origin[1]=overlay.getHeight()/2f;long t=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(t,t,0,origin[0],origin[1],0);overlay.dispatchTouchEvent(down);down.recycle();});
            Thread.sleep(450);
            checked(()->{try{CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());View overlay=board.getChildAt(3);int y=board.tiles.getJSONObject(1).getInt("y");long t=android.os.SystemClock.uptimeMillis();float x=origin[0]+board.getWidth()/12f,yy=origin[1]+40*getTargetContext().getResources().getDisplayMetrics().density;android.view.MotionEvent move=android.view.MotionEvent.obtain(t,t,2,x,yy,0);overlay.dispatchTouchEvent(move);move.recycle();android.view.MotionEvent up=android.view.MotionEvent.obtain(t,t+20,1,x,yy,0);overlay.dispatchTouchEvent(up);up.recycle();if(board.tiles.getJSONObject(1).getInt("x")!=1||board.tiles.getJSONObject(1).getInt("y")!=y+1)throw new AssertionError("Visual tile drag");if(!clickText(activity.getWindow().getDecorView(),"Zurück"))throw new AssertionError("Editor cancel");if(!prefs.getString("cockpit_board","").equals(saved))throw new AssertionError("Cancel changed saved layout");CockpitBoard restored=(CockpitBoard)findBoard(activity.getWindow().getDecorView());if(restored.tiles.getJSONObject(1).getInt("x")!=0)throw new AssertionError("Cancel did not restore layout");}catch(Exception e){throw new RuntimeException(e);}});
            checked(()->invoke(activity,"startMonitoring",new Class[0],new Object[0]));
            Thread.sleep(1200);
            if(!BmsMonitorService.running || BmsMonitorService.latestStatus==null)throw new AssertionError("Readiness service");
            coastingAndStopCheck(prefs);
            long stamp=BmsMonitorService.latestStatus.getLongExtra("timestamp",0);
            getUiAutomation().executeShellCommand("input keyevent 223").close();
            Thread.sleep(6200);
            android.os.PowerManager power=getTargetContext().getSystemService(android.os.PowerManager.class);
            if(power.isInteractive())throw new AssertionError("Screen should be off");
            if(BmsMonitorService.latestStatus.getLongExtra("timestamp",0)<=stamp)throw new AssertionError("Screen-off readiness heartbeat");
            getUiAutomation().executeShellCommand("input keyevent 224").close();
            getUiAutomation().executeShellCommand("wm dismiss-keyguard").close();
            Thread.sleep(500);
            checked(()->activity.startActivity(new Intent(activity,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)));
            if(!BmsMonitorService.running)throw new AssertionError("Cockpit reopening stops readiness");
            checked(()->invoke(activity,"renderStatus",new Class[]{Intent.class},new Object[]{new Intent()
                .putExtra("bms_connected",true).putExtra("bms_at",System.currentTimeMillis()).putExtra("discharge_enabled",true)}));
            screenshot("cockpit-controls.png");
            checked(()->activity.stopService(new Intent(activity,BmsMonitorService.class)));
            Thread.sleep(300);
            if(BmsMonitorService.running)throw new AssertionError("Readiness stop");
            checked(()->{invoke(activity,"editLayout",new Class[0],new Object[0]);try{((CockpitBoard)findBoard(activity.getWindow().getDecorView())).tiles.getJSONObject(0).put("caption","Portrait draft");}catch(Exception e){throw new RuntimeException(e);}});
            android.app.Instrumentation.ActivityMonitor rotation=addMonitor(MainActivity.class.getName(),null,false);
            checked(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            Activity landscape=rotation.waitForActivityWithTimeout(10000);if(landscape==null)throw new AssertionError("Landscape recreation");waitForIdleSync();checked(()->systemInsetsCheck(landscape));
            checked(()->{if(!contains(landscape.getWindow().getDecorView(),"Layout: Querformat · Kachel lange drücken und ziehen · unten rechts Größe ziehen · antippen für Inhalt/Farbe."))throw new AssertionError("Landscape editor lost");try{((CockpitBoard)findBoard(landscape.getWindow().getDecorView())).tiles.getJSONObject(0).put("caption","Landscape draft");}catch(Exception e){throw new RuntimeException(e);}});
            checked(()->landscape.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            Activity portrait=rotation.waitForActivityWithTimeout(10000);if(portrait==null)throw new AssertionError("Portrait recreation");waitForIdleSync();removeMonitor(rotation);checked(()->systemInsetsCheck(portrait));
            checked(()->{try{if(!((CockpitBoard)findBoard(portrait.getWindow().getDecorView())).tiles.getJSONObject(0).getString("caption").equals("Portrait draft"))throw new AssertionError("Portrait draft lost on rotation");if(!clickText(portrait.getWindow().getDecorView(),"Speichern"))throw new AssertionError("Save orientation drafts");if(!new org.json.JSONArray(prefs.getString("cockpit_board_landscape","")).getJSONObject(0).getString("caption").equals("Landscape draft"))throw new AssertionError("Landscape draft lost on save");}catch(Exception e){throw new RuntimeException(e);}});
            result.putString("stream","Cockpit launch, battery/range display, persisted odometer layout editor and screen-off readiness: OK\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e){try{screenshot("failure.png");}catch(Exception ignored){}result.putString("stream", "FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
    private void customizationCheck(Activity activity,SharedPreferences prefs)throws Throwable{
        getUiAutomation();
        checked(()->{
            TextView profile=findText(activity.getWindow().getDecorView(),"Mein Scooter");
            if(profile==null || (profile.getTransformationMethod()!=null&&profile.getTransformationMethod().getClass().getSimpleName().contains("AllCaps")))throw new AssertionError("Profile label forced to uppercase");
            invoke(activity,"showSettings",new Class[0],new Object[0]);
        });
        checked(()->{
            android.app.AlertDialog menu=(android.app.AlertDialog)member(activity,"settingsDialog");
            menu.getListView().performItemClick(menu.getListView().getChildAt(4),4,4);
            if(!menu.isShowing())throw new AssertionError("Settings parent was dismissed");
        });
        waitForIdleSync();
        checked(()->{android.app.AlertDialog battery=(android.app.AlertDialog)member(activity,"batteryConfigDialog");
            if(!battery.isShowing()||!inputAfterLabel(battery.getWindow().getDecorView(),"Startwert Verbrauch (Wh/km)").getText().toString().equals("20"))throw new AssertionError("20 Wh/km battery default");});
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
        android.view.accessibility.AccessibilityNodeInfo returned=null;
        for(int attempt=0;attempt<20;attempt++){returned=getUiAutomation().getRootInActiveWindow();if(returned!=null&&!returned.findAccessibilityNodeInfosByText("App-Farben").isEmpty())break;android.os.SystemClock.sleep(100);}
        if(returned==null||returned.findAccessibilityNodeInfosByText("App-Farben").isEmpty())throw new AssertionError("Settings menu not in foreground after back");
        checked(()->{
            android.app.AlertDialog menu=(android.app.AlertDialog)member(activity,"settingsDialog");
            if(!menu.isShowing())throw new AssertionError("Back did not return to settings");menu.dismiss();
            invoke(activity,"editLayout",new Class[0],new Object[0]);
            invoke(activity,"editBoardTile",new Class[]{int.class},new Object[]{0});
            android.app.AlertDialog dialog=(android.app.AlertDialog)member(activity,"tileDialog");
            if(Double.parseDouble(inputAfterLabel(dialog.getWindow().getDecorView(),"Skalenmaximum (Balken / Rundinstrument)").getText().toString())!=22)throw new AssertionError("Gauge editor default scale");
            android.widget.EditText unitSize=inputAfterLabel(dialog.getWindow().getDecorView(),"Einheit: Schriftgröße (8–80, leer = wie Wert)");
            if(!unitSize.getText().toString().isEmpty())throw new AssertionError("Legacy unit size should follow the value");
            unitSize.setText("7");clickText(dialog.getWindow().getDecorView(),"Übernehmen");
            if(!dialog.isShowing())throw new AssertionError("Invalid unit font accepted");unitSize.setText("16");
            spinnerAfterLabel(dialog.getWindow().getDecorView(),"Einheit: Position").setSelection(2);
            inputAfterLabel(dialog.getWindow().getDecorView(),"Instrument / Balken: Farbe (#RRGGBB)").setText("#26C6DA");
            inputAfterLabel(dialog.getWindow().getDecorView(),"Skala / Hintergrundbogen: Farbe (#RRGGBB)").setText("#AB47BC");
            clickText(dialog.getWindow().getDecorView(),"Übernehmen");
            MetricTile gauge=findMetric(activity.getWindow().getDecorView(),"speed");gauge.reading("20","km/h",false);gauge.layout(0,0,320,360);
            Bitmap rendered=Bitmap.createBitmap(320,360,Bitmap.Config.ARGB_8888);gauge.draw(new android.graphics.Canvas(rendered));
            int fill=0,track=0;for(int y=0;y<360;y++)for(int x=0;x<320;x++){int pixel=rendered.getPixel(x,y);if(pixel==android.graphics.Color.parseColor("#26C6DA"))fill++;if(pixel==android.graphics.Color.parseColor("#AB47BC"))track++;}rendered.recycle();
            if(fill<10||track<10)throw new AssertionError("Independent instrument/scale colours not drawn");
            invoke(activity,"addTile",new Class[]{String.class},new Object[]{"free_text"});
            CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());int index=board.tiles.length()-1;
            invoke(activity,"editBoardTile",new Class[]{int.class},new Object[]{index});dialog=(android.app.AlertDialog)member(activity,"tileDialog");
            inputAfterLabel(dialog.getWindow().getDecorView(),"Freitext (max. 4000 Zeichen)").setText("Mein Roller\nT6e – Los geht’s!");
            clickText(dialog.getWindow().getDecorView(),"Übernehmen");
            if(!contains(activity.getWindow().getDecorView(),"Mein Roller\nT6e – Los geht’s!"))throw new AssertionError("Free text lost casing or line breaks");
            invoke(activity,"addTile",new Class[]{String.class},new Object[]{"image"});
        });
        String data;
        File source=new File(getTargetContext().getFilesDir(),"test-photo.png");
        Bitmap photo=Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888);photo.eraseColor(android.graphics.Color.GREEN);
        try(FileOutputStream out=new FileOutputStream(source)){photo.compress(Bitmap.CompressFormat.PNG,100,out);}photo.recycle();
        data=TileImage.importPhoto(getTargetContext(),android.net.Uri.fromFile(source));TileImage.validate(data);
        final String portablePhoto=data;
        checked(()->{
            try{
                CockpitBoard board=(CockpitBoard)findBoard(activity.getWindow().getDecorView());org.json.JSONObject image=board.tiles.getJSONObject(board.tiles.length()-1);
                image.put("image_data",portablePhoto).put("image_mode",1).put("caption","Mein Rollerfoto");invoke(activity,"rebuild",new Class[0],new Object[0]);
                if(!hasPhoto(activity.getWindow().getDecorView(),"Mein Rollerfoto"))throw new AssertionError("Image tile did not render");
                clickText(activity.getWindow().getDecorView(),"Speichern");
                org.json.JSONObject exported=StorageFolders.install(getTargetContext()).snapshot();StorageFolders.validateValues(exported.getJSONObject("settings"),false);
                org.json.JSONArray saved=new org.json.JSONArray(exported.getJSONObject("settings").getString("cockpit_board"));
                if(saved.getJSONObject(0).getInt("unit_font")!=16||saved.getJSONObject(0).getInt("font")!=56||saved.getJSONObject(0).getInt("unit_position")!=2)throw new AssertionError("Unit size/position missing from backup");
                if(!saved.getJSONObject(saved.length()-1).getString("image_data").equals(portablePhoto)||!saved.getJSONObject(saved.length()-2).getString("free_text").contains("T6e"))throw new AssertionError("Personal tiles absent from backup");
                ScooterProfiles.add(prefs,"Foto-Kopie",true);
                if(!prefs.getString("cockpit_board","").equals(saved.toString()))throw new AssertionError("Photo/text design not copied across profiles");
                ScooterProfiles.deleteActive(prefs);prefs.edit().remove("cockpit_board").commit();
                java.lang.reflect.Field tiles=MainActivity.class.getDeclaredField("boardTiles");tiles.setAccessible(true);tiles.set(activity,CockpitBoard.defaults());invoke(activity,"rebuild",new Class[0],new Object[0]);
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static android.widget.Spinner spinnerAfterLabel(View view,String label){
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);if(child instanceof TextView&&((TextView)child).getText().toString().equals(label)&&i+1<group.getChildCount()&&group.getChildAt(i+1) instanceof android.widget.Spinner)return (android.widget.Spinner)group.getChildAt(i+1);
            android.widget.Spinner found=spinnerAfterLabel(child,label);if(found!=null)return found;
        }}return null;
    }
    private static boolean scrollAccessible(android.view.accessibility.AccessibilityNodeInfo node){
        if(node.isScrollable()&&node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;
        for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&scrollAccessible(child))return true;}return false;
    }
    private static Object member(Object object,String name){try{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new RuntimeException(e);}}
    private static android.widget.EditText inputAfterLabel(View view,String label){
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);if(child instanceof TextView&&((TextView)child).getText().toString().equals(label)&&i+1<group.getChildCount()&&group.getChildAt(i+1) instanceof android.widget.EditText)return (android.widget.EditText)group.getChildAt(i+1);
            android.widget.EditText found=inputAfterLabel(child,label);if(found!=null)return found;
        }}return null;
    }
    private static boolean hasPhoto(View view,String description){if(view instanceof android.widget.ImageView&&view.getContentDescription()!=null&&description.contentEquals(view.getContentDescription())&&((android.widget.ImageView)view).getDrawable()!=null)return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasPhoto(group.getChildAt(i),description))return true;}return false;}
    private void profileCheck(SharedPreferences prefs)throws Exception{
        String first=prefs.getString(ScooterProfiles.ACTIVE,"");String board=CockpitBoard.defaults().toString();
        prefs.edit().putString("total_km","123.45").putString("header_color","#42A5F5").putString("temp1_label","BMS (vermutet)").putString("cockpit_board",board).putString("temperature_history","[]").putLong("last_started_at",123456).putString("trips_tree","content://first").commit();
        ScooterProfiles.add(prefs,"Zweiter Scooter",true);String second=prefs.getString(ScooterProfiles.ACTIVE,"");
        if(prefs.contains("device_address")||prefs.contains("total_km")||prefs.contains("last_started_at")||prefs.contains("trips_tree"))throw new AssertionError("Copied profile retained scooter identity/history");
        if(!prefs.getString("header_color","").equals("#42A5F5")||!prefs.getString("cockpit_board","").equals(board))throw new AssertionError("Copied profile lost design");
        prefs.edit().putString("total_km","7").putString("device_address","02:00:00:00:00:02").commit();
        ScooterProfiles.switchTo(prefs,first);
        if(!prefs.getString("total_km","").equals("123.45")||!prefs.getString("device_address","").equals("02:00:00:00:00:01")||prefs.getLong("last_started_at",0)!=123456||!prefs.getString("trips_tree","").equals("content://first"))throw new AssertionError("Profile restore mixed scooter data");
        boolean old=BmsMonitorService.running;BmsMonitorService.running=true;boolean blocked=false;try{ScooterProfiles.switchTo(prefs,second);}catch(Exception expected){blocked=true;}finally{BmsMonitorService.running=old;}if(!blocked)throw new AssertionError("Profile switch during readiness");
        prefs.edit().remove("trips_tree").remove("last_started_at").remove("temp1_label").commit();
        StorageFolders.validateProfiles(ScooterProfiles.export(prefs),first);
        org.json.JSONArray invalid=ScooterProfiles.export(prefs);invalid.getJSONObject(1).put("id",first);boolean rejected=false;try{StorageFolders.validateProfiles(invalid,first);}catch(Exception expected){rejected=true;}if(!rejected)throw new AssertionError("Duplicate profile id accepted");
    }
    private void storageCheck(SharedPreferences prefs)throws Exception{
        StorageFolders storage=StorageFolders.install(getTargetContext());String tree="content://de.wortmonster.jbdtrigger.test.documents/tree/root";
        getTargetContext().startActivity(new Intent().setComponent(new android.content.ComponentName(getContext(),TestGrantActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(1000);
        getTargetContext().getContentResolver().takePersistableUriPermission(android.net.Uri.parse(tree),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        org.json.JSONArray portable=CockpitBoard.load(prefs);int bottom=0;for(int i=0;i<portable.length();i++)bottom=Math.max(bottom,portable.getJSONObject(i).getInt("y")+portable.getJSONObject(i).getInt("h"));
        String photo=TileImage.importPhoto(getTargetContext(),android.net.Uri.fromFile(new File(getTargetContext().getFilesDir(),"test-photo.png")));
        portable.put(CockpitBoard.position(CockpitLayout.tile("image").put("image_data",photo),0,bottom,6,3));
        portable.put(CockpitBoard.position(CockpitLayout.tile("free_text").put("free_text","Mein Roller – T6e"),6,bottom,6,3));
        CockpitBoard.validate(portable);prefs.edit().putString("cockpit_board",portable.toString()).commit();
        storage.choose(StorageFolders.SETTINGS,tree,false);
        android.net.Uri uri=StorageFolders.document(getTargetContext(),tree,StorageFolders.NAME,false,"application/json");
        if(uri==null)throw new AssertionError("SAF settings snapshot missing");
        org.json.JSONObject document=new org.json.JSONObject(StorageFolders.read(getTargetContext(),uri));
        if(!document.getJSONObject("settings").getString("header_name").equals("Mein Joyor"))throw new AssertionError("Header stored in settings");
        if(!document.getJSONObject("settings").getString("total_km").equals("123.45"))throw new AssertionError("SAF settings write");
        if(document.getInt("version")!=2||document.getJSONArray("profiles").length()!=2)throw new AssertionError("Multi-scooter backup");
        String savedLayout=document.getJSONObject("settings").getString("cockpit_board");CockpitBoard.validate(new org.json.JSONArray(savedLayout));
        prefs.edit().putString("total_km","987.65").putString("cockpit_board","[]").commit();storage.importSettings(tree);
        if(!prefs.getString("total_km","").equals("123.45"))throw new AssertionError("SAF import");
        if(!prefs.getString("cockpit_board","").equals(savedLayout))throw new AssertionError("SAF layout import");
        document.put("version",99);StorageFolders.write(getTargetContext(),uri,document.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean rejected=false;try{storage.importSettings(tree);}catch(Exception e){rejected=true;}
        if(!rejected||!prefs.getString("total_km","").equals("123.45"))throw new AssertionError("Invalid import should preserve settings");
        StorageFolders.write(getTargetContext(),uri,storage.snapshot().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File trip=new File(getTargetContext().getFilesDir(),"test-trip.csv");try(FileOutputStream output=new FileOutputStream(trip)){output.write("trip;980".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        String bad="content://de.wortmonster.jbdtrigger.test.documents/tree/readonly";
        storage.exportTrip(bad,trip);Thread.sleep(500);
        if(!trip.isFile()||new org.json.JSONArray(prefs.getString("pending_exports","[]")).length()!=1)throw new AssertionError("Failed export must retain local file and retry job");
        // Retarget this test job to a writable tree, then retry the actual persisted queue.
        org.json.JSONArray jobs=new org.json.JSONArray(prefs.getString("pending_exports","[]"));jobs.getJSONObject(0).put("tree",tree);prefs.edit().putString("pending_exports",jobs.toString()).commit();storage.retry();
        if(new org.json.JSONArray(prefs.getString("pending_exports","[]")).length()!=0)throw new AssertionError("Retry queue not cleared");
        android.net.Uri copied=StorageFolders.document(getTargetContext(),tree,"test-trip.csv",false,"text/csv");if(copied==null||!StorageFolders.read(getTargetContext(),copied).equals("trip;980"))throw new AssertionError("SAF trip copy");
        prefs.edit().remove(StorageFolders.SETTINGS).apply();
    }
    private static TextView findText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return (TextView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView found=findText(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private static MetricTile findMetric(View v,String key){if(v instanceof MetricTile && ((MetricTile)v).key.equals(key))return (MetricTile)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){MetricTile found=findMetric(g.getChildAt(i),key);if(found!=null)return found;}}return null;}
    private static int tileBackground(MetricTile tile){return ((android.graphics.drawable.GradientDrawable)((View)tile.getParent()).getBackground()).getColor().getDefaultColor();}
    private void checked(Runnable task) throws Throwable {
        AtomicReference<Throwable> error=new AtomicReference<>();
        runOnMainSync(()->{try{task.run();}catch(Throwable t){error.set(t);}});
        if(error.get()!=null)throw error.get();waitForIdleSync();
    }
    private void systemInsetsCheck(Activity activity){
        ViewGroup content=activity.findViewById(android.R.id.content);
        android.widget.ScrollView scroll=(android.widget.ScrollView)content.getChildAt(0);
        androidx.core.view.WindowInsetsCompat actual=androidx.core.view.ViewCompat.getRootWindowInsets(scroll);
        if(actual==null)throw new AssertionError("System insets missing");
        int types=androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout();
        androidx.core.graphics.Insets safe=actual.getInsets(types);
        if(scroll.getPaddingTop()!=safe.top||scroll.getPaddingBottom()!=safe.bottom||scroll.getPaddingLeft()!=safe.left||scroll.getPaddingRight()!=safe.right||!scroll.getClipToPadding())throw new AssertionError("System bars or cutout overlap scroll viewport");
        TextView heading=findText(content,"ScootPit BMS");int[] position=new int[2];heading.getLocationOnScreen(position);
        if(position[1]<safe.top)throw new AssertionError("App header behind status bar");
        androidx.core.view.WindowInsetsCompat simulated=new androidx.core.view.WindowInsetsCompat.Builder()
            .setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),androidx.core.graphics.Insets.of(0,30,0,48))
            .setInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout(),androidx.core.graphics.Insets.of(24,60,12,0)).build();
        try{
            for(int i=0;i<2;i++)androidx.core.view.ViewCompat.dispatchApplyWindowInsets(scroll,simulated);
            if(scroll.getPaddingLeft()!=24||scroll.getPaddingTop()!=60||scroll.getPaddingRight()!=12||scroll.getPaddingBottom()!=48)throw new AssertionError("Cutout/system bar padding accumulated or missing");
        }finally{androidx.core.view.ViewCompat.dispatchApplyWindowInsets(scroll,actual);}
    }
    private void unitFontRenderCheck(Activity activity) {
        try {
            for(int arrangement:new int[]{1,2})for(int display:new int[]{0,1,2})for(int position:new int[]{0,1,2}){
                org.json.JSONObject config=new org.json.JSONObject().put("key","speed").put("font",40).put("unit_font",10).put("unit_position",position).put("display",display).put("arrangement",arrangement).put("show_title",false).put("show_note",false);
                MetricTile metric=new MetricTile(activity,config,0xffffffff,0xffffb300,0xff35434d,0);metric.layout(0,0,640,320);metric.reading("18,2","km/h",false);
                Bitmap small=Bitmap.createBitmap(640,320,Bitmap.Config.ARGB_8888),large=Bitmap.createBitmap(640,320,Bitmap.Config.ARGB_8888);
                metric.draw(new android.graphics.Canvas(small));config.put("unit_font",32);metric.draw(new android.graphics.Canvas(large));
                int changed=0;for(int y=0;y<320;y++)for(int x=0;x<640;x++)if(small.getPixel(x,y)!=large.getPixel(x,y))changed++;
                small.recycle();large.recycle();if(changed<20||config.getInt("font")!=40||!metric.getContentDescription().toString().contains("18,2")||!metric.getContentDescription().toString().contains("km/h"))throw new AssertionError("Unit font not independent in layout "+arrangement+"/"+display+"/"+position);
            }
            org.json.JSONArray invalid=CockpitBoard.defaults();invalid.getJSONObject(0).put("unit_font",81);
            boolean rejected=false;try{CockpitBoard.validate(invalid);}catch(Exception expected){rejected=true;}
            if(!rejected)throw new AssertionError("Invalid imported unit font accepted");
            invalid.getJSONObject(0).put("unit_font",16).put("unit_position",3);rejected=false;
            try{CockpitBoard.validate(invalid);}catch(Exception expected){rejected=true;}
            if(!rejected)throw new AssertionError("Invalid imported unit position accepted");
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private void batterySettingsCheck(Activity activity)throws Throwable {
        checked(()->invoke(activity,"showSettings",new Class[0],new Object[0]));
        checked(()->{
            android.app.AlertDialog menu=(android.app.AlertDialog)member(activity,"settingsDialog");
            menu.getListView().performItemClick(menu.getListView().getChildAt(8),8,8);
            android.app.AlertDialog status=(android.app.AlertDialog)member(activity,"batteryDialog");
            boolean exempt=activity.getSystemService(android.os.PowerManager.class).isIgnoringBatteryOptimizations(activity.getPackageName());
            TextView message=status.findViewById(android.R.id.message);
            if(!status.isShowing()||message==null||!message.getText().toString().contains(exempt?"bereits von":"ist für ScootPit BMS aktiv"))throw new AssertionError("Battery menu did not show current status");
        });
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
        checked(()->{
            android.app.AlertDialog menu=(android.app.AlertDialog)member(activity,"settingsDialog");
            if(!menu.isShowing())throw new AssertionError("Battery back lost settings menu");menu.dismiss();
        });
        batteryActionCheck(activity,false,android.app.AlertDialog.BUTTON_POSITIVE,android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryActionCheck(activity,true,android.app.AlertDialog.BUTTON_POSITIVE,android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        batteryActionCheck(activity,true,android.app.AlertDialog.BUTTON_NEUTRAL,android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    }
    private void batteryActionCheck(Activity activity,boolean exempt,int button,String action)throws Throwable {
        android.content.IntentFilter filter=new android.content.IntentFilter(action);
        if(!action.equals(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))filter.addDataScheme("package");
        ActivityMonitor monitor=addMonitor(filter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
        try {
            checked(()->invoke(activity,"showBatterySettings",new Class[]{boolean.class},new Object[]{exempt}));
            checked(()->{
                android.app.AlertDialog dialog=(android.app.AlertDialog)member(activity,"batteryDialog");
                TextView message=dialog.findViewById(android.R.id.message);
                if(message==null||!message.getText().toString().contains(exempt?"bereits von":"ist für ScootPit BMS aktiv"))throw new AssertionError("Battery status branch");
                dialog.getButton(button).performClick();
            });
            if(monitor.getHits()!=1)throw new AssertionError("Battery settings action missing: "+action);
        } finally {removeMonitor(monitor);}
    }
    private void coastingAndStopCheck(SharedPreferences prefs)throws Throwable {
        checked(()->{
            BmsMonitorService service=BmsMonitorService.liveService;if(service==null)throw new AssertionError("Live service missing");
            boolean weather=prefs.getBoolean("weather_enabled",true);String total=prefs.getString("total_km","0");
            long start=System.currentTimeMillis();
            try{
                prefs.edit().putBoolean("weather_enabled",false).commit();
                TripRecorder recorder=new TripRecorder(getTargetContext(),start);
                setMember(service,"recorder",recorder);setMember(service,"tripStartedAt",start);setMember(service,"lastPacket",start);
                setMember(service,"active",false);setMember(service,"current",0.0);setMember(service,"lastLocation",null);
                android.location.Location one=new android.location.Location("gps");one.setLatitude(49);one.setLongitude(8);one.setAccuracy(3);one.setSpeed(3);one.setSpeedAccuracyMetersPerSecond(.3f);one.setTime(start);one.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
                service.onLocationChanged(one);
                android.location.Location two=new android.location.Location(one);two.setLatitude(49.000025);two.setSpeed(2);two.setTime(start+1000);two.setElapsedRealtimeNanos(one.getElapsedRealtimeNanos()+1000000000L);
                service.onLocationChanged(two);
                Intent status=BmsMonitorService.latestStatus;
                if(Math.abs(status.getDoubleExtra("speed_kmh",0)-7.2)>.01||status.getDoubleExtra("distance_m",0)<2)throw new AssertionError("Coasting lost speed/distance without current");
                RideMotion motion=(RideMotion)member(service,"motion");long now=android.os.SystemClock.elapsedRealtime();motion.reset();for(long at=now-45000;at<=now;at+=1000)motion.fix(at,0,true);
                ((Runnable)member(service,"clock")).run();
                if(BmsMonitorService.latestStatus.getBooleanExtra("trip_active",true)||!BmsMonitorService.running)throw new AssertionError("Stillstand must finish trip and retain readiness");
                java.util.List<TripJournal.Ride> rides=TripJournal.list(getTargetContext(),prefs,7);TripJournal.Ride found=null;for(TripJournal.Ride r:rides)if(r.start==start)found=r;
                if(found==null)throw new AssertionError("Auto-ended ride missing");
                found.csv.delete();found.gpx.delete();new File(found.csv.getParent(),found.csv.getName().replace(".csv",".json")).delete();
            }catch(Exception e){throw new RuntimeException(e);}finally{prefs.edit().putBoolean("weather_enabled",weather).putString("total_km",total).apply();}
        });
    }
    private static void setMember(Object target,String name,Object value)throws Exception{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private void officialVersionCheck(Activity activity,SharedPreferences prefs)throws Throwable {
        SharedPreferences test=getTargetContext().getSharedPreferences("v100-test",0);test.edit().clear().commit();
        checked(()->{
            try{
                ScooterProfiles.install(test);test.edit().putString("device_address","02:00:00:00:02:01").putString("routine_start_title","Tour beginnt").commit();
                String first=test.getString(ScooterProfiles.ACTIVE,"");DistanceCounters.add(test,1200,System.currentTimeMillis());
                if(Math.abs(DistanceCounters.daily(test,System.currentTimeMillis())-1.2)>.001||DistanceCounters.number(test,"tour_km")!=1.2)throw new AssertionError("Independent counters");
                if(DistanceCounters.daily(test,System.currentTimeMillis()+2*86400000L)!=0)throw new AssertionError("Day counter reset");
                ScooterProfiles.add(test,"Zweiter Scooter",true);test.edit().putString("device_address","02:00:00:00:02:02").commit();
                if(DistanceCounters.number(test,"tour_km")!=0||DistanceCounters.number(test,"daily_km")!=0)throw new AssertionError("Copied distances");
                boolean old=BmsMonitorService.running;BmsMonitorService.running=true;
                try{ScooterProfiles.switchForService(test,first);}finally{BmsMonitorService.running=old;}
                if(DistanceCounters.number(test,"tour_km")!=1.2||!test.getString("routine_start_title","").equals("Tour beginnt"))throw new AssertionError("Automatic profile restore");
                if(ScooterProfiles.knownBms(test).size()!=2)throw new AssertionError("Known BMS selection");
                ScooterProfiles.add(test,"Doppelte Adresse",false);test.edit().putString("device_address","02:00:00:00:02:01").commit();
                if(ScooterProfiles.knownBms(test).containsKey("02:00:00:00:02:01"))throw new AssertionError("Ambiguous identity allowed");
                test.edit().putString("daily_day",DistanceCounters.day(System.currentTimeMillis())).putString("journal_stats","[\"speed_chart\"]").commit();
                StorageFolders.validateProfiles(ScooterProfiles.export(test),test.getString(ScooterProfiles.ACTIVE,""));
                if(findDescription(activity.getWindow().getDecorView(),"Fahrtenbuch")==null)throw new AssertionError("Journal entry missing");
                long start=System.currentTimeMillis()-10000;TripRecorder recorder=new TripRecorder(getTargetContext(),start);
                android.location.Location one=new android.location.Location("gps");one.setLatitude(49);one.setLongitude(8);one.setAccuracy(3);one.setSpeed(3);one.setAltitude(100);one.setTime(start);
                recorder.add(one,80,48,-2,96,0,96,20,new double[]{25,30});
                android.location.Location two=new android.location.Location(one);two.setLatitude(49.001);two.setTime(start+1000);two.setSpeed(4);
                recorder.add(two,80,48,-2,96,.026,96,20,new double[]{25,30});
                TripRecorder.Summary summary=recorder.finish(start+2000,111,14.4,0,.026,96,20,25,30,2000,0);
                TripJournal.Ride found=null;for(TripJournal.Ride ride:TripJournal.list(getTargetContext(),prefs,7))if(ride.start==start)found=ride;
                if(found==null)throw new AssertionError("Saved ride not listed");TripJournal.loadPoints(found);
                if(found.points.size()!=2||found.moving!=2000||Math.abs(found.averagePower-96)>.01)throw new AssertionError("Journal statistics");
                summary.gpxFile.delete();summary.csvFile.delete();summary.metadataFile.delete();
                String version=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
                if(!"1.1.0".equals(version))throw new AssertionError("Official version");
            }catch(Exception e){throw new RuntimeException(e);}
        });test.edit().clear().commit();
    }
    private void designCheck(Activity activity,SharedPreferences prefs)throws Throwable {
        java.util.Map<String,?> saved=prefs.getAll();Object savedTiles=member(activity,"boardTiles");
        try{
            checked(()->{try{
                for(String key:CockpitLayout.KEYS){Bitmap b=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);CockpitSymbols.draw(new android.graphics.Canvas(b),key,0,0,48,0xffff9800);int count=0;for(int y=0;y<48;y++)for(int x=0;x<48;x++)if(android.graphics.Color.alpha(b.getPixel(x,y))>0)count++;b.recycle();if(count<40)throw new AssertionError("Missing icon: "+key);}
                for(int mode=0;mode<4;mode++){org.json.JSONObject t=new org.json.JSONObject().put("key","tour").put("heading_mode",mode);if(CockpitSymbols.icon(t)!=(mode==0||mode==2)||CockpitSymbols.title(t)!=(mode==1||mode==2))throw new AssertionError("Heading choice");}
                org.json.JSONArray tiles=new org.json.JSONArray(savedTiles.toString());tiles.put(CockpitBoard.position(CockpitLayout.tile("tour"),0,40,6,2));prefs.edit().putString("cockpit_board",tiles.toString()).putString("tour_km","38.2").putString("daily_km","12.6").commit();
                String before=tiles.getJSONObject(1).toString();CockpitTheme.apply(prefs,true,false);if(!new org.json.JSONArray(prefs.getString("cockpit_board","")).getJSONObject(1).toString().equals(before))throw new AssertionError("Preset changed custom tile without permission");
                CockpitTheme.apply(prefs,true,true);org.json.JSONArray light=new org.json.JSONArray(prefs.getString("cockpit_board",""));if(!light.getJSONObject(0).getString("background").equals("#00000000")||!light.getJSONObject(0).getString("text").equals("#172B40"))throw new AssertionError("Light transparent gauge");
                java.lang.reflect.Field f=MainActivity.class.getDeclaredField("boardTiles");f.setAccessible(true);f.set(activity,light);invoke(activity,"rebuild",new Class[0],new Object[0]);
                String total=prefs.getString("total_km","0"),daily=prefs.getString("daily_km","0");View tour=findDescription(activity.getWindow().getDecorView(),"Tourenzähler zurücksetzen");if(tour==null||!tour.performClick())throw new AssertionError("Tour tile not clickable");
            }catch(Exception e){throw new RuntimeException(e);}});
            waitForIdleSync();
            android.view.accessibility.AccessibilityNodeInfo active=getUiAutomation().getRootInActiveWindow();if(active==null||active.findAccessibilityNodeInfosByText("Tourenzähler zurücksetzen?").isEmpty())throw new AssertionError("Tour confirmation missing");
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
            String totalBeforeReset=prefs.getString("total_km","0"),dailyBeforeReset=prefs.getString("daily_km","0");
            checked(()->{if(!prefs.getString("tour_km","").equals("38.2"))throw new AssertionError("Cancel reset changed counter");findDescription(activity.getWindow().getDecorView(),"Tourenzähler zurücksetzen").performClick();((android.app.AlertDialog)member(activity,"tourDialog")).getButton(-1).performClick();});
            waitForIdleSync();
            checked(()->{if(DistanceCounters.number(prefs,"tour_km")!=0||!totalBeforeReset.equals(prefs.getString("total_km","0"))||!dailyBeforeReset.equals(prefs.getString("daily_km","0")))throw new AssertionError("Tour reset changed other counters");systemInsetsCheck(activity);});screenshot("design-light.png");
            checked(()->{try{CockpitTheme.apply(prefs,false,true);java.lang.reflect.Field f=MainActivity.class.getDeclaredField("boardTiles");f.setAccessible(true);f.set(activity,CockpitBoard.load(prefs));invoke(activity,"rebuild",new Class[0],new Object[0]);View journal=findDescription(activity.getWindow().getDecorView(),"Fahrtenbuch"),settings=findDescription(activity.getWindow().getDecorView(),"Einstellungen öffnen");if(journal==null||settings==null||journal.getParent()!=settings.getParent())throw new AssertionError("Header actions not in same row");StorageFolders.validateValues(StorageFolders.install(getTargetContext()).snapshot().getJSONObject("settings"),false);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();screenshot("design-dark.png");
        }finally{checked(()->{SharedPreferences.Editor e=prefs.edit().clear();for(java.util.Map.Entry<String,?> entry:saved.entrySet()){Object value=entry.getValue();String key=entry.getKey();if(value instanceof String)e.putString(key,(String)value);else if(value instanceof Boolean)e.putBoolean(key,(Boolean)value);else if(value instanceof Long)e.putLong(key,(Long)value);else if(value instanceof Integer)e.putInt(key,(Integer)value);}e.commit();try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("boardTiles");f.setAccessible(true);f.set(activity,savedTiles);invoke(activity,"rebuild",new Class[0],new Object[0]);}catch(Exception ex){throw new RuntimeException(ex);}});}
    }
    private static View findDescription(View v,String text){if(v.getContentDescription()!=null&&text.contentEquals(v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findDescription(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    private static void invoke(Object target,String name,Class[] types,Object[] args){try{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(target,args);}catch(Exception e){throw new RuntimeException(e);}}
    private static void findSliders(View view,java.util.List<android.widget.SeekBar> result){if(view instanceof android.widget.SeekBar)result.add((android.widget.SeekBar)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)findSliders(((ViewGroup)view).getChildAt(i),result);}
    private static androidx.core.widget.NestedScrollView findLog(View v){if(v instanceof androidx.core.widget.NestedScrollView)return (androidx.core.widget.NestedScrollView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){androidx.core.widget.NestedScrollView found=findLog(g.getChildAt(i));if(found!=null)return found;}}return null;}
    private static View findBoard(View view){if(view instanceof CockpitBoard)return view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){View found=findBoard(g.getChildAt(i));if(found!=null)return found;}}return null;}
    private static boolean clickText(View view,String text){if(view instanceof TextView && text.contentEquals(((TextView)view).getText()))return view.performClick();if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(clickText(g.getChildAt(i),text))return true;}return false;}
    private static boolean contains(View view,String text){if(view instanceof TextView && text.contentEquals(((TextView)view).getText()))return true;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),text))return true;}return false;}
    private void screenshot(String name)throws Exception {
        // Wait only for the compositor after the UI thread has become idle.
        Thread.sleep(300);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        File dir=new File(getTargetContext().getExternalFilesDir(null),"screenshots");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
}
