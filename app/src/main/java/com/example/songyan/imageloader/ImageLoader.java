package com.example.songyan.imageloader;

import android.graphics.Bitmap;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.LinkedList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class ImageLoader {
    private static ImageLoader mInstance;
    public static ImageLoader getInstance(int threadCount,Type type){
        if(mInstance==null){
            synchronized (ImageLoader.class){
                if(mInstance==null){
                    mInstance=new ImageLoader(threadCount,type);
                    return mInstance;
                }
            }
        }
        return null;
    }

    //图片缓存的核心对象
    private LruCache<String,Bitmap> mLruCache;

    //线程池
    private ExecutorService mThreadPool;//这个类可以执行Runnable
    private static final int DEFAULT_THREAD_COUNT=1;

    //队列的调度方式
    private Type mType=Type.LIFO;

    //任务队列
    private LinkedList<Runnable> mTaskQueue;

    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;//后台轮询线程的Handler

    //UI线程的Handler
    private Handler mUIHandler;

    //保证轮询线程Handler建立的同步性的信号量
    private Semaphore mSemaphorePoolThreadHandler=new Semaphore(0);
    //保证task处理同步性的信号量
    private Semaphore mSemaphoreThreadPool;

    private boolean isDiskCacheEnable=true;

    private static final String TAG="ImageLoader";

    public enum Type{
        FIFO,LIFO
    }

    private ImageLoader(int threadCount,Type type){
        init(threadCount,type);
    }

    //初始化
    private void init(int threadCount,Type type){
        initBackThread();

        //获取我们应用的最大可用内存
        int maxMemory=(int)Runtime.getRuntime().maxMemory();
        int cacheMemory=maxMemory/8;
        mLruCache=new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };
        //创建线程池
        mThreadPool= Executors.newFixedThreadPool(threadCount);
        //初始化任务队列
        mTaskQueue=new LinkedList<Runnable>();
        mType=type;
        //初始化线程池信号量
        mSemaphoreThreadPool=new Semaphore(threadCount);
    }

    //初始化后台轮询线程
    private void initBackThread(){
        mPoolThread=new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler=new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //取出一个任务到线程池中执行
                        mThreadPool.execute(getTask());
                        try{
                            mSemaphoreThreadPool.acquire();//线程池信号量减一
                        }catch (InterruptedException e){

                        }

                    }
                };
                mSemaphorePoolThreadHandler.release();//mPoolThreadHandler初始化完成,释放信号量
                Looper.loop();
            }
        };
        mPoolThread.start();
    }

    //根据path给ImageView设置图片
    public void loadImage(String path,ImageView view,boolean isFromNet){
        view.setTag(path);//每个ImageView都关联一个路径

        if(mUIHandler==null){
            //更新主线程UI
            mUIHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    ImageBeanHolder holder=(ImageBeanHolder)msg.obj;
                    Bitmap bitmap=holder.bitmap;
                    ImageView imageView=holder.imageView;
                    String path=holder.path;
                    if(imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }

        Bitmap bitmap=getBitmapFromLruCache(path);

        if(bitmap!=null){
            //更新UI,Handler sendMessage
            refresh(path,view,bitmap);
        }else{
            //新建task并加入taskQueue
            addTask(buildTask(path,view,isFromNet));
        }

    }

    private Runnable buildTask(final String path, final ImageView imageView, final boolean isFromNet){
        return new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap=null;
                if(isFromNet){
                    //从网络加载图片的内容
                }else{
                    bitmap=ImageLoadUtils.loadBitmapFromLocal(path,imageView);
                }
                //把图片加入到缓存
                addBitmapToLruCache(path,bitmap);
                refresh(path,imageView,bitmap);
                mSemaphoreThreadPool.release();//线程池信号量加一
            }
        };
    }

    private void addBitmapToLruCache(String path,Bitmap bitmap){
        if(getBitmapFromLruCache(path)==null){
            if(bitmap!=null){
                mLruCache.put(path,bitmap);
            }
        }
    }


    private void addTask(Runnable runnable){
        mTaskQueue.add(runnable);
        if(mPoolThreadHandler==null){
            try {
                mSemaphorePoolThreadHandler.acquire();//如果mPoolHeadHandler为空,信号量阻塞直到它被初始化后释放信号量
            }catch (InterruptedException e){

            }
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);//会进入mPoolThreadHandler的handleMessage方法
    }

    private Runnable getTask(){
        if(mType==Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType==Type.LIFO){
            return mTaskQueue.removeLast();
        }
        return  null;
    }

    private void refresh(String path,ImageView view,Bitmap bitmap){
        Message message=Message.obtain();
        ImageBeanHolder holder=new ImageBeanHolder();
        holder.path=path;
        holder.bitmap=bitmap;
        holder.imageView=view;
        message.obj=holder;
        mUIHandler.sendMessage(message);
    }

    private Bitmap getBitmapFromLruCache(String path){
        return mLruCache.get(path);
    }

    class ImageBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

}
