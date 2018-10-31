package com.example.songyan.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoadUtils {

    //得到ImageView的宽和高,也就是要求的宽和高
    public static ImageSize getImageSize(ImageView imageView){

        ImageSize imageSize=new ImageSize();
        DisplayMetrics displayMetrics=imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams  lp=imageView.getLayoutParams();

        int width=imageView.getWidth();//得到ImageView的实际宽度
        if(width<=0){
            width=lp.width;
        }
        if(width<=0){
            width=displayMetrics.widthPixels;
        }

        int height=imageView.getHeight();//得到ImageView的实际高度
        if(height<=0){
            height=lp.height;
        }
        if(height<=0){
            height=displayMetrics.heightPixels;
        }
        imageSize.width=width;
        imageSize.height=height;
        return imageSize;
    }


    //根据需求的宽和高还有图片实际的宽和高计算sampleSize
    public static int getSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        int width=options.outWidth;
        int height=options.outHeight;

        int sampleSize=1;

        if(width>reqWidth || height>reqHeight){
            int widthRadio=Math.round(width*1.0f/reqWidth);
            int heightRadio=Math.round(height*1.0f/reqHeight);

            sampleSize=Math.max(widthRadio,heightRadio);
        }

        return sampleSize;
    }

    //得到图片的宽和高,并不把图片加载到内存
    public static Bitmap loadBitmapFromLocal(String path,ImageView imageView){
        ImageSize imageSize=getImageSize(imageView);
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize=getSampleSize(options,imageSize.width,imageSize.height);
        options.inJustDecodeBounds=false;
        Bitmap bitmap=BitmapFactory.decodeFile(path,options);//如果bitmap返回为null,原因应该是读写文件的运行时权限
        return bitmap;
    }

    public static Bitmap downloadImageByUrl(String uriString,ImageView imageView){
        FileOutputStream fos=null;
        InputStream is=null;
        try{
            URL url=new URL(uriString);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            is=new BufferedInputStream(conn.getInputStream());
            is.mark(is.available());

            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            BitmapFactory.decodeStream(is,null,options);

            ImageSize imageSize=getImageSize(imageView);
            options.inSampleSize=getSampleSize(options,imageSize.width,imageSize.height);
            options.inJustDecodeBounds=false;
            is.reset();
            conn.disconnect();
            return BitmapFactory.decodeStream(is,null,options);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(is!=null){
                    is.close();
                }
            }catch (IOException e){

            }
            try{
                if(fos!=null){
                    fos.close();
                }
            }catch (IOException e){

            }
            return null;
        }
    }

    public static class ImageSize{
        int width;
        int height;
    }
}
