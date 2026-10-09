package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fine-grid responsive dashboard. Editing uses the very same tile views. */
final class CockpitBoard extends FrameLayout {
    static final int GRID_COLUMNS=24, GRID_UNIT_DP=20;
    final JSONArray tiles;
    private final int unit;
    private final boolean editing;
    private final Runnable changed;
    private int downX,downY,oldX,oldY,oldW,oldH;
    private boolean resizing, moved, dragging;
    private Runnable hold;
    private JSONObject active;
    private int[][] initial;
    CockpitBoard(Context context, JSONArray tiles, boolean editing, Runnable changed) {
        super(context);this.tiles=tiles;this.editing=editing;this.changed=changed;
        unit=Math.round(GRID_UNIT_DP*getResources().getDisplayMetrics().density);
        setClipChildren(false);
    }
    void addTile(View view, JSONObject tile, Runnable configure) {
        view.setTag(tile);addView(view,new FrameLayout.LayoutParams(1,1));
        if(editing){
            // The overlay consumes touches so switches and readiness actions cannot run in the editor.
            View overlay=new View(getContext());overlay.setTag(tile);
            overlay.setContentDescription("Kachel bearbeiten: "+CockpitLayout.title(tile.optString("key")));
            overlay.setBackground(new android.graphics.drawable.GradientDrawable(){ {
                setColor(0x08ffffff);setStroke(2,CockpitTheme.color(getContext().getSharedPreferences("settings",0),"accent_color","#FF9800"));setCornerRadius(12);
            }});
            addView(overlay,new FrameLayout.LayoutParams(1,1));
            overlay.setOnClickListener(v->configure.run());
            overlay.setOnTouchListener((v,event)->{
                int column=Math.max(1,getWidth()/GRID_COLUMNS);
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                    initial=positions();active=tile;downX=(int)event.getRawX();downY=(int)event.getRawY();
                    oldX=tile.optInt("x");oldY=tile.optInt("y");oldW=tile.optInt("w",6);oldH=tile.optInt("h",3);
                    int grip=Math.round(32*getResources().getDisplayMetrics().density);
                    resizing=event.getX()>v.getWidth()-grip && event.getY()>v.getHeight()-grip;
                    moved=false;dragging=resizing;
                    if(resizing)getParent().requestDisallowInterceptTouchEvent(true);
                    hold=()->{dragging=true;getParent().requestDisallowInterceptTouchEvent(true);v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);};v.postDelayed(hold,350);return true;
                }
                if(event.getActionMasked()==MotionEvent.ACTION_MOVE){
                    if(!dragging)return true;
                    int dx=Math.round((event.getRawX()-downX)/column),dy=Math.round((event.getRawY()-downY)/unit);
                    if(dx!=0||dy!=0)moved=true;
                    restore(initial);
                    try {if(resizing){tile.put("w",Math.max(1,Math.min(GRID_COLUMNS-oldX,oldW+dx))).put("h",Math.max(2,Math.min(24,oldH+dy)));}
                    else tile.put("x",Math.max(0,Math.min(GRID_COLUMNS-oldW,oldX+dx))).put("y",Math.max(0,Math.min(600,oldY+dy)));}catch(Exception ignored){}
                    try{push(tile);}catch(Exception e){restore(initial);}
                    requestLayout();return true;
                }
                if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL){
                    boolean cancel=event.getActionMasked()==MotionEvent.ACTION_CANCEL;v.removeCallbacks(hold);
                    if(cancel)restore(initial);
                    active=null;getParent().requestDisallowInterceptTouchEvent(false);requestLayout();
                    if(!cancel){if(!moved)v.performClick();else changed.run();}return true;
                }return true;
            });

        }
    }
    private int[][] positions(){int[][] p=new int[tiles.length()][4];for(int i=0;i<tiles.length();i++){JSONObject t=tiles.optJSONObject(i);p[i]=new int[]{t.optInt("x"),t.optInt("y"),t.optInt("w"),t.optInt("h")};}return p;}
    private void restore(int[][] p){if(p==null)return;try{for(int i=0;i<p.length;i++)tiles.getJSONObject(i).put("x",p[i][0]).put("y",p[i][1]).put("w",p[i][2]).put("h",p[i][3]);}catch(Exception ignored){}}
    void push(JSONObject locked)throws Exception{if(locked.optBoolean("allow_overlap",false)||hasOverlaps())return;pack(locked,false);}
    private boolean hasOverlaps(){for(int i=0;i<tiles.length();i++)if(overlaps(tiles.optJSONObject(i)))return true;return false;}
    void compact(){int[][] before=positions();try{pack(null,true);}catch(Exception e){restore(before);}requestLayout();}
    private void pack(JSONObject locked,boolean compact)throws Exception{
        GridPacking.Cell[] cells=new GridPacking.Cell[tiles.length()];int index=-1;for(int i=0;i<cells.length;i++){JSONObject t=tiles.getJSONObject(i);if(t==locked)index=i;cells[i]=new GridPacking.Cell(i,t.getInt("x"),t.getInt("y"),t.getInt("w"),t.getInt("h"));}
        GridPacking.arrange(cells,index,compact);for(GridPacking.Cell cell:cells)tiles.getJSONObject(cell.id).put("y",cell.y);
    }
    boolean overlaps(JSONObject t){
        for(int i=0;i<tiles.length();i++){JSONObject o=tiles.optJSONObject(i);if(o==t)continue;
            if(t.optInt("x")<o.optInt("x")+o.optInt("w",6) && t.optInt("x")+t.optInt("w",6)>o.optInt("x") && t.optInt("y")<o.optInt("y")+o.optInt("h",3) && t.optInt("y")+t.optInt("h",3)>o.optInt("y"))return true;
        }return false;
    }
    int bottom(){int b=3;for(int i=0;i<tiles.length();i++){JSONObject t=tiles.optJSONObject(i);b=Math.max(b,t.optInt("y")+t.optInt("h",3));}return b;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),gap=Math.round(3*getResources().getDisplayMetrics().density);
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);JSONObject t=(JSONObject)v.getTag();v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,width*t.optInt("w",12)/GRID_COLUMNS-2*gap),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(unit*t.optInt("h",6)-2*gap,MeasureSpec.EXACTLY));}
        setMeasuredDimension(width,unit*(bottom()+(editing?4:0)));
    }
    @Override protected void onLayout(boolean c,int l,int t,int r,int b){
        int width=r-l,gap=Math.round(3*getResources().getDisplayMetrics().density);
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);JSONObject tile=(JSONObject)v.getTag();int x=width*tile.optInt("x")/GRID_COLUMNS+gap,y=unit*tile.optInt("y")+gap;v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());}
    }
    @Override protected void dispatchDraw(android.graphics.Canvas canvas){super.dispatchDraw(canvas);if(!editing)return;
        android.graphics.Paint p=new android.graphics.Paint();p.setColor(CockpitTheme.color(getContext().getSharedPreferences("settings",0),"accent_color","#FF9800"));p.setTextSize(24*getResources().getDisplayMetrics().density);
        for(int i=0;i<getChildCount();i+=2){View v=getChildAt(i);canvas.drawText("◢",v.getRight()-p.getTextSize(),v.getBottom()-4,p);}
    }
    static JSONArray load(android.content.SharedPreferences prefs){return load(prefs,"cockpit_board");}
    static JSONArray load(android.content.SharedPreferences prefs,String key){
        if(key.equals("cockpit_board_landscape"))try{
            String savedText=prefs.getString(key,"");if(savedText.isEmpty()){JSONArray portrait=load(prefs,"cockpit_board");prefs.edit().putString(key,portrait.toString()).putString("cockpit_board_landscape_version","1.2.1").apply();return portrait;}
            JSONArray saved=new JSONArray(savedText);
            if(!"1.2.1".equals(prefs.getString("cockpit_board_landscape_version",""))){
                JSONArray original=new JSONArray(saved.toString());for(int i=0;i<saved.length();i++){JSONObject t=saved.getJSONObject(i);for(String coordinate:new String[]{"x","y","w","h"})t.put(coordinate,t.getInt(coordinate)*2);}
                validate(saved);prefs.edit().putString("cockpit_board_landscape_before_121",original.toString()).putString(key,saved.toString()).putString("cockpit_board_landscape_version","1.2.1").apply();
            }validate(saved);return saved;
        }catch(Exception ignored){}

        try{JSONArray saved=new JSONArray(prefs.getString("cockpit_board",""));validate(saved);
            if(!prefs.getString("cockpit_design_version","").equals("1.2.1")){
                JSONArray updated=arrange121(saved);validate(updated);
                prefs.edit().putString("cockpit_board_before_121",saved.toString()).putString("cockpit_board",updated.toString()).putString("cockpit_design_version","1.2.1").apply();return updated;
            }return saved;}catch(Exception ignored){}
        JSONArray result=prefs.contains("cockpit_layout")?fromRows(CockpitLayout.load(prefs)):defaults();
        prefs.edit().putString("cockpit_board",result.toString()).putString("cockpit_design_version","1.2.1").apply();return result;
    }
    static JSONArray arrange121(JSONArray previous)throws Exception{
        JSONArray result=defaults();boolean[] used=new boolean[previous.length()];
        for(int i=0;i<result.length();i++){
            JSONObject target=result.getJSONObject(i);
            for(int j=0;j<previous.length();j++)if(!used[j]&&previous.getJSONObject(j).optString("key").equals(target.optString("key"))){
                JSONObject old=new JSONObject(previous.getJSONObject(j).toString());used[j]=true;
                for(String coordinate:new String[]{"x","y","w","h"})old.put(coordinate,target.getInt(coordinate));
                if(old.optString("key").equals("power")){old.put("display",2);if(!old.optBoolean("custom_colors",false))old.put("custom_colors",true).put("background","#00000000");if(!old.has("gauge_sweep"))old.put("gauge_sweep",180);}
                if(old.optString("key").equals("speed"))old.put("display",0);
                result.put(i,old);break;
            }
        }
        int bottom=48;for(int j=0;j<previous.length();j++)if(!used[j]){JSONObject extra=new JSONObject(previous.getJSONObject(j).toString());extra.put("x",extra.optInt("x")*2).put("y",bottom).put("w",extra.optInt("w",6)*2).put("h",extra.optInt("h",3)*2);bottom+=extra.optInt("h",6);result.put(extra);}
        return result.length()>100?defaults():result;
    }
    static JSONArray arrange111(JSONArray previous)throws Exception{
        JSONArray result=defaults();boolean[] used=new boolean[previous.length()];
        for(int i=0;i<result.length();i++){
            JSONObject target=result.getJSONObject(i);
            for(int j=0;j<previous.length();j++)if(!used[j]&&previous.getJSONObject(j).optString("key").equals(target.optString("key"))){
                JSONObject old=new JSONObject(previous.getJSONObject(j).toString());used[j]=true;
                if(!old.has("caption")&&target.has("caption"))old.put("caption",target.getString("caption"));
                for(String coordinate:new String[]{"x","y","w","h"})old.put(coordinate,target.getInt(coordinate));
                if(old.optString("key").equals("speed")){if(!old.has("unit_position"))old.put("unit_position",3);if(!old.has("unit_font"))old.put("unit_font",18);}
                if(old.optString("key").equals("ready_start")||old.optString("key").equals("ready_end"))old.put("caption","Bereit").put("font",14);
                result.put(i,old);break;
            }
        }
        int bottom=20;for(int j=0;j<previous.length();j++)if(!used[j]){
            if(bottom>300)return new JSONArray(previous.toString());
            JSONObject extra=new JSONObject(previous.getJSONObject(j).toString());extra.put("y",bottom);bottom+=extra.getInt("h");result.put(extra);
        }if(result.length()>100||bottom>312)return new JSONArray(previous.toString());return result;
    }
    static JSONArray defaults(){
        JSONArray tiles=new JSONArray();
        try{
            tiles.put(position(standardTile("consumption_500m").put("display",2).put("scale_max",50).put("caption","Verbrauch · letzte 500 m").put("gauge_sweep",180).put("custom_colors",true).put("background","#00000000"),0,0,6,4));
            tiles.put(position(standardTile("power").put("display",2).put("scale_max",1400).put("caption","Leistung").put("gauge_sweep",180).put("custom_colors",true).put("background","#00000000"),6,0,6,4));
            tiles.put(position(standardTile("speed").put("display",0).put("font",34).put("unit_font",18).put("caption","Geschwindigkeit"),0,4,6,2));
            tiles.put(position(standardTile("soc").put("display",1).put("show_note",false),6,4,6,2));
            tiles.put(position(standardTile("range").put("show_note",false),6,6,6,2));
            String[][] rows={{"moving","standing","trip_time"},{"distance","max_power","daily"},{"tour","bms_output","image"},{"total","outside","temp2"},{"temp1","ready_start","trip_end"},{"ready_end","voltage","current"}};
            for(int row=0;row<rows.length;row++)for(int col=0;col<3;col++){
                String key=rows[row][col];JSONObject tile=standardTile(key);
                if(key.equals("image"))tile.put("heading_mode",3);
                if(key.equals("temp2"))tile.put("caption","Akku-Temp");
                if(key.equals("temp1"))tile.put("caption","BMS-Temp");
                if(key.equals("bms_output"))tile.put("caption","BMS\nLastausgang");
                if(key.equals("ready_start")||key.equals("ready_end"))tile.put("caption","Bereit").put("font",14);
                tiles.put(position(tile,col*4,8+row*2,4,2));
            }
            tiles.put(position(standardTile("log"),0,20,12,4));
        }catch(Exception e){throw new IllegalStateException(e);}
        return tiles;
    }
    static JSONArray standardDefaults(android.content.Context context,String key){
        try(java.io.InputStream in=context.getAssets().open("default_cockpit_settings.json")){
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;while((count=in.read(chunk))!=-1)buffer.write(chunk,0,count);
            JSONObject root=new JSONObject(new String(buffer.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));JSONArray layout=root.getJSONArray(key);validate(layout);return layout;
        }catch(Exception e){android.util.Log.e("ScootPit","Standardlayout konnte nicht geladen werden: "+key,e);return defaults();}
    }
    private static JSONObject standardTile(String key)throws Exception{return CockpitLayout.tile(key).put("arrangement",2);}
    private static JSONArray fromRows(JSONArray rows){
        JSONArray result=new JSONArray();int y=3;
        try{result.put(position(CockpitLayout.tile("bms_output"),0,0,12,3));for(int r=0;r<rows.length();r++){JSONObject row=rows.getJSONObject(r);JSONArray cells=row.getJSONArray("tiles");double sum=0;for(int c=0;c<cells.length();c++)sum+=cells.getJSONObject(c).optDouble("weight",1);
            int x=0,h=Math.max(2,Math.min(10,Math.round(row.optInt("height",120)/40f)));
            for(int c=0;c<cells.length();c++){JSONObject tile=new JSONObject(cells.getJSONObject(c).toString());int w=c==cells.length()-1?12-x:Math.max(1,(int)Math.round(12*tile.optDouble("weight",1)/sum));w=Math.min(w,12-x-(cells.length()-c-1));
                if(tile.optString("key").equals("temperatures")){tile.put("key","temp1");result.put(position(tile,x,y,w,h));JSONObject second=new JSONObject(tile.toString()).put("key","temp2");result.put(position(second,x,y+h,w,h));}
                else result.put(position(tile,x,y,w,h));x+=w;
            }boolean temps=false;for(int c=0;c<cells.length();c++)if(cells.getJSONObject(c).optString("key").equals("temperatures"))temps=true;y+=temps?h*2:h;
        }
        result.put(position(CockpitLayout.tile("max_power"),0,y,12,3));y+=3;
        result.put(position(CockpitLayout.tile("ready_start"),0,y,6,2));result.put(position(CockpitLayout.tile("ready_end"),6,y,6,2));y+=2;
        result.put(position(CockpitLayout.tile("trip_end"),0,y,12,2));y+=2;
        result.put(position(CockpitLayout.tile("log"),0,y,12,4));
        }catch(Exception ignored){}return result;
    }
    static JSONObject position(JSONObject t,int x,int y,int w,int h)throws Exception{return t.put("x",x*2).put("y",y*2).put("w",w*2).put("h",h*2);}
    private static void validateAppearance(JSONObject tile)throws Exception{
        if(tile.has("gauge_sweep")&&(tile.getInt("gauge_sweep")<90||tile.getInt("gauge_sweep")>270))throw new Exception("Kreisausschnitt muss zwischen 90° und 270° liegen");
        if(tile.has("gauge_rotation")&&(tile.getInt("gauge_rotation")<0||tile.getInt("gauge_rotation")>359))throw new Exception("Drehwinkel muss zwischen 0° und 359° liegen");
        if(tile.has("heading_mode")&&(tile.getInt("heading_mode")<0||tile.getInt("heading_mode")>3))throw new Exception("Ungültige Symbol-/Beschriftungsauswahl");
        if(tile.has("unit_font")){double size=tile.getDouble("unit_font");if(!Double.isFinite(size)||size<8||size>80)throw new Exception("Einheit-Schriftgröße von 8 bis 80 erforderlich");}
        if(tile.has("unit_position")&&(tile.getInt("unit_position")<0||tile.getInt("unit_position")>3))throw new Exception("Ungültige Einheit-Position");
        if(tile.has("gauge_value_position")&&(tile.getInt("gauge_value_position")<0||tile.getInt("gauge_value_position")>2))throw new Exception("Ungültige Messwertposition");
        if(tile.has("scale_gradient_mode")&&(tile.getInt("scale_gradient_mode")<0||tile.getInt("scale_gradient_mode")>2))throw new Exception("Ungültiger Skalenfarbmodus");
        if(tile.optInt("scale_gradient_mode",0)>0&&(tile.optInt("scale_gradient_low_full",20)<0||tile.optInt("scale_gradient_low_full",20)>=tile.optInt("scale_gradient_high_full",80)||tile.optInt("scale_gradient_high_full",80)>100))throw new Exception("Die rote Schwelle muss unter der grünen Schwelle liegen");
        for(String field:new String[]{"background","text","instrument_color","scale_color","icon_color","border_color","scale_gradient_low","scale_gradient_mid","scale_gradient_high"})if(tile.has(field))android.graphics.Color.parseColor(tile.getString(field));
        if(tile.has("corner_radius")&&(tile.getInt("corner_radius")<0||tile.getInt("corner_radius")>48))throw new Exception("Eckenradius muss zwischen 0 und 48 dp liegen");
        if(tile.optString("caption").length()>300)throw new Exception("Beschriftung zu lang");
        if(tile.optString("free_text").length()>4000)throw new Exception("Freitext mit maximal 4000 Zeichen");
        if(tile.has("image_data")&&!tile.getString("image_data").isEmpty())TileImage.validate(tile.getString("image_data"));
        if(tile.has("image_mode")&&(tile.getInt("image_mode")<0||tile.getInt("image_mode")>1))throw new Exception("Ungültige Bilddarstellung");
    }
    static void validate(JSONArray tiles)throws Exception{
        if(tiles.length()<1||tiles.length()>100)throw new Exception("1–100 Kacheln erforderlich");
        java.util.HashSet<String> single=new java.util.HashSet<>();
        for(int i=0;i<tiles.length();i++){JSONObject t=tiles.getJSONObject(i);String key=t.getString("key");if((key.equals("bms_output")||key.equals("log"))&&!single.add(key))throw new Exception("Doppelte Steuerkachel");validateAppearance(t);int x=t.getInt("x"),y=t.getInt("y"),w=t.getInt("w"),h=t.getInt("h");if(x<0||y<0||y>600||w<1||w>GRID_COLUMNS||x+w>GRID_COLUMNS||h<2||h>24||(!java.util.Arrays.asList(CockpitLayout.KEYS).contains(t.getString("key"))&&!BmsExtras.known(key)))throw new Exception("Ungültige Kachel");}
        for(int i=0;i<tiles.length();i++)for(int j=i+1;j<tiles.length();j++){JSONObject a=tiles.getJSONObject(i),b=tiles.getJSONObject(j);if(a.getInt("x")<b.getInt("x")+b.getInt("w")&&a.getInt("x")+a.getInt("w")>b.getInt("x")&&a.getInt("y")<b.getInt("y")+b.getInt("h")&&a.getInt("y")+a.getInt("h")>b.getInt("y")&&!a.optBoolean("allow_overlap",false)&&!b.optBoolean("allow_overlap",false))throw new Exception("Kacheln überlappen – bei einer Kachel Überlappung aktivieren");}
    }
}

