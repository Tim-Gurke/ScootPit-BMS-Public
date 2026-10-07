package de.wortmonster.jbdtrigger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Small metadata-free photo embedded in the layout, so backups are portable. */
final class TileImage {
    static String importPhoto(Context context, Uri uri)throws Exception {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}
        if(options.outWidth<=0||options.outHeight<=0)throw new Exception("Kein lesbares Bild");
        int sample=1;while(Math.max(options.outWidth,options.outHeight)/sample>960)sample*=2;
        int orientation=ExifInterface.ORIENTATION_NORMAL;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
        }catch(Exception ignored){}
        options.inJustDecodeBounds=false;options.inSampleSize=sample;
        Bitmap bitmap;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}
        if(bitmap==null)throw new Exception("Bild konnte nicht geladen werden");
        try {
            Matrix transform=new Matrix();
            switch(orientation){
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:transform.setScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_180:transform.setRotate(180);break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:transform.setScale(1,-1);break;
                case ExifInterface.ORIENTATION_TRANSPOSE:transform.setRotate(90);transform.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_90:transform.setRotate(90);break;
                case ExifInterface.ORIENTATION_TRANSVERSE:transform.setRotate(270);transform.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_270:transform.setRotate(270);break;
            }
            if(!transform.isIdentity()){
                Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),transform,true);
                if(rotated!=bitmap){bitmap.recycle();bitmap=rotated;}
            }
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            int quality=82;
            while(true){
                bytes.reset();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,bytes);
                if(bytes.size()<=80000)break;
                if(quality>45){quality-=10;continue;}
                Bitmap smaller=Bitmap.createScaledBitmap(bitmap,Math.max(1,bitmap.getWidth()*3/4),Math.max(1,bitmap.getHeight()*3/4),true);
                if(smaller==bitmap)throw new Exception("Bild zu groß");
                bitmap.recycle();bitmap=smaller;
            }
            return Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);
        }finally{bitmap.recycle();}
    }
    static void validate(String data)throws Exception{
        if(data.length()>110000)throw new Exception("Kachelbild zu groß");
        byte[] bytes=Base64.decode(data,Base64.DEFAULT);
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>1024||bounds.outHeight>1024)throw new Exception("Ungültiges Kachelbild");
    }
    static Bitmap decode(String data){
        try{validate(data);byte[] bytes=Base64.decode(data,Base64.DEFAULT);BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);}
        catch(Exception e){return null;}
    }
}
