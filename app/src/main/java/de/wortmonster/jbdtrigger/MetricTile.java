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
        setContentDescription(CockpitSymbols.caption(config)+": "+value+(note.isEmpty()?"":" · "+note)+(inactive?" · nicht aktuell":""));
        if(getParent() instanceof android.view.View){android.graphics.drawable.Drawable bg=((android.view.View)getParent()).getBackground();if(bg instanceof android.graphics.drawable.GradientDrawable){
            float luminance=Color.red(background)*.2126f+Color.green(background)*.7152f+Color.blue(background)*.0722f;int shade=luminance>150?Math.round(luminance*.96f):Math.min(48,Math.round(luminance*.55f));
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
    private void readingText(Canvas canvas,String value,String prefix,float centerX,float centerY,float maxWidth,float size,float maxUnitSize,int color){
        String[] parts=value.trim().split("\\s+",2);
        int position=Math.max(0,Math.min(3,config.optInt("unit_position",0)));
        if((!config.has("unit_font")&&position==0)||parts.length!=2||!parts[0].matches("[-+]?\\d[\\d.,]*")){
            text(canvas,prefix+value,centerX,centerY+size*.34f,maxWidth,size,true,color);return;
        }
        float unitSize=config.has("unit_font")?Math.min(maxUnitSize,sp(Math.max(8,Math.min(80,config.optInt("unit_font",28))))):size;
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(Typeface.DEFAULT_BOLD);
        String number=prefix+parts[0];paint.setTextSize(size);float numberWidth=paint.measureText(number);
        paint.setTextSize(unitSize);float unitWidth=paint.measureText(parts[1]),gap=dp(3);
        float total=position>=2?Math.max(numberWidth,unitWidth):numberWidth+gap+unitWidth;
        float textHeight=position>=2?size+gap+unitSize:Math.max(size,unitSize+(position==1?size*.4f:0));
        float fit=Math.min(1,Math.min(maxWidth/Math.max(1,total),maxUnitSize/Math.max(1,textHeight)));
        size*=fit;unitSize*=fit;numberWidth*=fit;unitWidth*=fit;gap*=fit;total*=fit;
        if(position>=2){
            paint.setTextSize(unitSize);canvas.drawText(parts[1],centerX-unitWidth/2,centerY+(position==3?1:-1)*(size+gap)/2+unitSize*.34f,paint);
            paint.setTextSize(size);canvas.drawText(number,centerX-numberWidth/2,centerY+(position==3?-1:1)*(unitSize+gap)/2+size*.34f,paint);return;
        }
        float left=centerX-total/2,baseline=centerY+Math.max(size,unitSize)*.34f;
        paint.setTextSize(size);canvas.drawText(number,left,baseline,paint);
        paint.setTextSize(unitSize);canvas.drawText(parts[1],left+numberWidth+gap,baseline-(position==1?size*.4f:0),paint);
    }
    @Override protected void onDraw(Canvas c){
        int foreground=inactive?Color.argb(Color.alpha(this.foreground),120,120,120):this.foreground,accent=inactive?0xff606060:this.accent;
        float width=getWidth()-dp(12),height=getHeight(),cx=getWidth()/2f;
        if(width<=0||height<=0)return;
        String value=getText().toString(),caption=CockpitSymbols.title(config)?CockpitSymbols.caption(config):"";
        String displayNote=note;
        if(key.equals("speed")&&note.equals("km/h")&&(config.has("unit_font")||config.optInt("unit_position",0)!=0)){
            value+=" km/h";displayNote="";
        }
        int layout=config.optInt("arrangement",0),style=config.optInt("display",key.equals("speed")?2:key.equals("soc")?1:0);
        if(key.equals("speed")&&style==2&&height>=dp(110)&&width>=dp(100)){
            gauge(c,value.replace(" km/h",""),cx,width,height,foreground,accent);return;
        }
        boolean single=layout==1||(layout==0&&height<dp(95));
        float valueSize=sp(Math.max(12,Math.min(80,config.optInt("font",28))));
        boolean showIcon=CockpitSymbols.icon(config);int symbol=parseColor(config.optString("icon_color"),CockpitSymbols.defaultColor(key,Color.red(this.foreground)<180));
        if(single){
            float iconSpace=showIcon?dp(30):0;if(showIcon)CockpitSymbols.draw(c,key,dp(8),height/2-dp(12),dp(24),symbol);
            String prefix=caption.isEmpty()?"":caption.replace('\n',' ')+"  ";
            readingText(c,value,prefix,cx+iconSpace/2,height/2,width-iconSpace,Math.min(valueSize,height*.45f),height*.7f,foreground);
            if(style!=0)bar(c,dp(8),height-dp(10),getWidth()-dp(16),progress(value));return;
        }
        List<String> headings=caption.isEmpty()?Collections.emptyList():lines(caption,Math.max(1,Math.min(3,config.optInt("lines",2))),width-(showIcon?dp(30):0));
        float lineHeight=sp(14),top=dp(6);
        // Titles and notes give way before the reading does.
        if(height<dp(65))headings=Collections.emptyList();
        if(showIcon&&height>=dp(65)){
            float labelWidth=0;paint.setTextSize(sp(12));for(String heading:headings)labelWidth=Math.max(labelWidth,Math.min(width-dp(30),paint.measureText(heading)));
            CockpitSymbols.draw(c,key,headings.isEmpty()?cx-dp(11):cx-(labelWidth+dp(28))/2,top,dp(22),symbol);
            float headingCenter=headings.isEmpty()?cx:cx+dp(14);
            for(String heading:headings){text(c,heading,headingCenter,top+sp(13),width-dp(30),sp(12),false,foreground);top+=lineHeight;}top=Math.max(top,dp(30));
        }else for(String heading:headings){text(c,heading,cx,top+sp(11),width,sp(12),false,foreground);top+=lineHeight;}
        boolean showNote=config.optBoolean("show_note",true)&&!displayNote.isEmpty()&&height-top>dp(65);
        float bottom=height-(showNote?sp(23):dp(8)),middle=(top+bottom)/2;
        float valueWidth=width;
        if(style==2&&bottom-top>=dp(85)&&width>=dp(100)){
            float radius=Math.min(width/2-dp(10),(bottom-top)/2-dp(6));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(6));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(inactive?Color.argb(Color.alpha(scale),41,41,41):scale);
            RectF arc=new RectF(cx-radius,middle-radius,cx+radius,middle+radius);c.drawArc(arc,140,260,false,paint);paint.setColor(accent);c.drawArc(arc,140,(float)(260*progress(value)),false,paint);
            valueWidth=radius*1.55f;valueSize=Math.min(valueSize,radius*.60f);
        }else if(style!=0){bar(c,dp(8),bottom-dp(5),getWidth()-dp(16),progress(value));bottom-=dp(12);middle=(top+bottom)/2;}
        float available=Math.max(dp(12),bottom-top);valueSize=Math.min(valueSize,available*.65f);
        readingText(c,value,"",cx,middle,valueWidth,valueSize,available*.65f,foreground);
        if(showNote)text(c,displayNote.replace('\n',' '),cx,height-dp(8),width,sp(10),false,foreground);
    }
    private void gauge(Canvas c,String value,float cx,float width,float height,int foreground,int accent){
        float radius=Math.min(width/2-dp(8),height/2-dp(10)),cy=height/2;
        double max=config.optDouble("scale_max",CockpitLayout.DEFAULT_SPEED_SCALE_MAX);
        if(!Double.isFinite(max)||max<=0)max=CockpitLayout.DEFAULT_SPEED_SCALE_MAX;
        double progress=progress(value);int track=inactive?Color.argb(Color.alpha(scale),55,55,55):scale;
        RectF bounds=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeWidth(dp(3));paint.setColor(track);c.drawArc(bounds,140,260,false,paint);
        paint.setColor(accent);paint.setStrokeWidth(dp(4));if(progress>0)c.drawArc(bounds,140,(float)(260*progress),false,paint);
        for(int i=0;i<=40;i++){
            double angle=Math.toRadians(140+260*i/40d);boolean major=i%10==0;
            float outer=radius-dp(7),inner=outer-dp(major?10:5);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(major?1.3f:.8f));paint.setColor(i/40d<=progress&&progress>0?accent:track);
            c.drawLine(cx+(float)Math.cos(angle)*inner,cy+(float)Math.sin(angle)*inner,cx+(float)Math.cos(angle)*outer,cy+(float)Math.sin(angle)*outer,paint);
            if(major){double number=max*i/40d;String label=Math.abs(number-Math.rint(number))<.001?String.format(Locale.GERMANY,"%.0f",number):String.format(Locale.GERMANY,"%.1f",number);
                float labelRadius=radius-dp(30);text(c,label,cx+(float)Math.cos(angle)*labelRadius,cy+(float)Math.sin(angle)*labelRadius+sp(4),dp(36),sp(11),false,foreground);
            }
        }
        if(progress>0){double angle=Math.toRadians(140+260*progress);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(accent);c.drawLine(cx+(float)Math.cos(angle)*(radius+dp(2)),cy+(float)Math.sin(angle)*(radius+dp(2)),cx+(float)Math.cos(angle)*(radius-dp(17)),cy+(float)Math.sin(angle)*(radius-dp(17)),paint);}
        float numberSize=Math.min(sp(Math.max(12,Math.min(80,config.optInt("font",72)))),radius*.76f);
        int unitPosition=config.optInt("unit_position",3);float unitSize=sp(config.optInt("unit_font",18));
        if(unitPosition==3){text(c,value,cx,cy+numberSize*.26f,radius*1.45f,numberSize,true,foreground);text(c,"km/h",cx,cy+numberSize*.26f+Math.min(radius*.39f,unitSize+dp(10)),radius,Math.min(unitSize,radius*.22f),false,foreground);}
        else readingText(c,value+" km/h","",cx,cy,radius*1.45f,numberSize,radius*.88f,foreground);
    }
    private int parseColor(String value,int fallback){try{return Color.parseColor(value);}catch(Exception e){return fallback;}}
    private double progress(String value){
        try{double n=Double.parseDouble(value.replace(',','.').split(" ")[0]);double defaultMax=key.equals("speed")?CockpitLayout.DEFAULT_SPEED_SCALE_MAX:key.equals("soc")?100:key.contains("power")?1200:key.equals("voltage")?60:key.equals("current")?30:key.startsWith("temp")?80:100;
            double max=config.optDouble("scale_max",defaultMax);return Double.isFinite(n)&&Double.isFinite(max)?Math.max(0,Math.min(1,n/Math.max(.1,max))):0;}catch(Exception e){return 0;}
    }
    private void bar(Canvas c,float x,float y,float w,double progress){paint.setStyle(Paint.Style.FILL);paint.setColor(inactive?Color.argb(Color.alpha(scale),41,41,41):scale);c.drawRoundRect(x,y,x+w,y+dp(4),dp(2),dp(2),paint);paint.setColor(inactive?Color.argb(Color.alpha(accent),96,96,96):accent);c.drawRoundRect(x,y,x+(float)(w*progress),y+dp(4),dp(2),dp(2),paint);}
}
