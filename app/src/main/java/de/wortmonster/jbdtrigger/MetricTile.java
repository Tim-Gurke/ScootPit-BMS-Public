package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.graphics.*;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.*;

/** Text remains accessible; drawing prioritises the value at every tile height. */
final class MetricTile extends TextView {
    final JSONObject config;
    final String key;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String note="";
    private final int foreground,accent,scale,background;
    private boolean inactive=true;
    MetricTile(Context context,JSONObject config,int foreground,int accent,int scale,int background){super(context);this.config=config;this.key=config.optString("key");this.foreground=foreground;this.accent=accent;this.scale=scale;this.background=background;setText("–");setPadding(0,0,0,0);}
    void reading(String value,String note,boolean inactive){
        setText(value);this.note=note;this.inactive=inactive;
        setContentDescription(config.optString("caption",CockpitLayout.title(key))+": "+value+(note.isEmpty()?"":" · "+note)+(inactive?" · nicht aktuell":""));
        if(getParent() instanceof android.view.View){android.graphics.drawable.Drawable bg=((android.view.View)getParent()).getBackground();if(bg instanceof android.graphics.drawable.GradientDrawable){
            int shade=Math.min(48,Math.round((Color.red(background)*.2126f+Color.green(background)*.7152f+Color.blue(background)*.0722f)*.55f));
            ((android.graphics.drawable.GradientDrawable)bg).setColor(inactive?Color.argb(Color.alpha(background),shade,shade,shade):background);
        }}invalidate();
    }
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float sp(float n){return n*getResources().getDisplayMetrics().scaledDensity;}
    private void text(Canvas canvas,String text,float centerX,float baseline,float maxWidth,float size,boolean bold,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);paint.setTextSize(size);
        float width=paint.measureText(text);if(width>maxWidth)paint.setTextSize(size*maxWidth/Math.max(1,width));
        canvas.drawText(text,centerX-paint.measureText(text)/2,baseline,paint);
    }
    private List<String> lines(String text,int limit,float width){
        List<String> result=new ArrayList<>();paint.setTextSize(sp(12));paint.setTypeface(Typeface.DEFAULT);
        for(String paragraph:text.split("\n",-1)){
            String current="";for(String word:paragraph.split(" ")){String candidate=current.isEmpty()?word:current+" "+word;if(!current.isEmpty()&&paint.measureText(candidate)>width){result.add(current);current=word;}else current=candidate;}result.add(current);
        }
        if(result.size()>limit){StringBuilder tail=new StringBuilder(result.get(limit-1));for(int i=limit;i<result.size();i++)tail.append(" ").append(result.get(i));result=new ArrayList<>(result.subList(0,limit));result.set(limit-1,tail.toString());}return result;
    }
    @Override protected void onDraw(Canvas c){
        int foreground=inactive?Color.argb(Color.alpha(this.foreground),120,120,120):this.foreground,accent=inactive?0xff606060:this.accent;
        float width=getWidth()-dp(12),height=getHeight(),cx=getWidth()/2f;
        if(width<=0||height<=0)return;
        String value=getText().toString(),caption=config.optBoolean("show_title",true)?config.optString("caption",CockpitLayout.title(key)):"";
        int layout=config.optInt("arrangement",0),style=config.optInt("display",key.equals("speed")?2:key.equals("soc")?1:0);
        boolean single=layout==1||(layout==0&&height<dp(95));
        float valueSize=sp(Math.max(12,Math.min(80,config.optInt("font",28))));
        if(single){
            String combined=(caption.isEmpty()?"":caption.replace('\n',' ')+"  ")+value;
            text(c,combined,cx,height/2+Math.min(valueSize,height*.45f)*.34f,width,Math.min(valueSize,height*.45f),true,foreground);
            if(style!=0)bar(c,dp(8),height-dp(10),getWidth()-dp(16),progress(value));return;
        }
        List<String> headings=caption.isEmpty()?Collections.emptyList():lines(caption,Math.max(1,Math.min(3,config.optInt("lines",2))),width);
        float lineHeight=sp(14),top=dp(6);
        // Titles and notes give way before the reading does.
        if(height<dp(65))headings=Collections.emptyList();
        for(String heading:headings){text(c,heading,cx,top+sp(11),width,sp(12),false,foreground);top+=lineHeight;}
        boolean showNote=config.optBoolean("show_note",true)&&!note.isEmpty()&&height-top>dp(65);
        float bottom=height-(showNote?sp(23):dp(8)),middle=(top+bottom)/2;
        float valueWidth=width;
        if(style==2&&bottom-top>=dp(85)&&width>=dp(100)){
            float radius=Math.min(width/2-dp(10),(bottom-top)/2-dp(6));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(6));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(inactive?Color.argb(Color.alpha(scale),41,41,41):scale);
            RectF arc=new RectF(cx-radius,middle-radius,cx+radius,middle+radius);c.drawArc(arc,140,260,false,paint);paint.setColor(accent);c.drawArc(arc,140,(float)(260*progress(value)),false,paint);
            valueWidth=radius*1.55f;valueSize=Math.min(valueSize,radius*.60f);
        }else if(style!=0){bar(c,dp(8),bottom-dp(5),getWidth()-dp(16),progress(value));bottom-=dp(12);middle=(top+bottom)/2;}
        float available=Math.max(dp(12),bottom-top);valueSize=Math.min(valueSize,available*.65f);
        text(c,value,cx,middle+valueSize*.34f,valueWidth,valueSize,true,foreground);
        if(showNote)text(c,note.replace('\n',' '),cx,height-dp(8),width,sp(10),false,foreground);
    }
    private double progress(String value){
        try{double n=Double.parseDouble(value.replace(',','.').split(" ")[0]);double defaultMax=key.equals("speed")?CockpitLayout.DEFAULT_SPEED_SCALE_MAX:key.equals("soc")?100:key.contains("power")?1200:key.equals("voltage")?60:key.equals("current")?30:key.startsWith("temp")?80:100;
            double max=config.optDouble("scale_max",defaultMax);return Math.max(0,Math.min(1,n/Math.max(.1,max)));}catch(Exception e){return 0;}
    }
    private void bar(Canvas c,float x,float y,float w,double progress){paint.setStyle(Paint.Style.FILL);paint.setColor(inactive?Color.argb(Color.alpha(scale),41,41,41):scale);c.drawRoundRect(x,y,x+w,y+dp(4),dp(2),dp(2),paint);paint.setColor(inactive?Color.argb(Color.alpha(accent),96,96,96):accent);c.drawRoundRect(x,y,x+(float)(w*progress),y+dp(4),dp(2),dp(2),paint);}
}
