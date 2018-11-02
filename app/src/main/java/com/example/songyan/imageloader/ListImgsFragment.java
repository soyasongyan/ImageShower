package com.example.songyan.imageloader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListImgsFragment extends Fragment {

    private static final String TAG="ListImgsFragment";

    private GridView mGridView;
    private String[] mUrlStrs;
    private List<String> mImagePathList;
    private ImageLoader mImageLoader;
    private static final String[] PERMISSION_EXTERNAL_STORAGE=new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE=100;
    private static final String DIRECTORY_NAME="Pictures";

    //请求运行时权限
    private void verifyStoragePermission(Activity activity){
        int permissionWrite=ActivityCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionWrite!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,PERMISSION_EXTERNAL_STORAGE,REQUEST_EXTERNAL_STORAGE);
        }
    }

    private List<String> getImagePathFromSD(){
        List<String> imageList=new ArrayList<String>();
        String filePath=Environment.getExternalStorageDirectory().toString()+ File.separator+DIRECTORY_NAME;
        Log.e(TAG,"filePath is "+filePath);
        //路径/storage/emulated/0/Pictures和路径/sdcard/Pictures效果一致
        //获得该路径下所有的文件
        File fileAll=new File(filePath);
        File[] files=fileAll.listFiles();
        Log.e(TAG,"files.length is "+files.length);
        for(int i=0;i<files.length;i++){
            if(checkIsImageFile(files[i].getPath())){
                imageList.add(files[i].getPath());
            }
        }
        return imageList;
    }

    private boolean checkIsImageFile(String filePath){
        boolean isImageFile=false;
        //获取扩展名
        String endName=filePath.substring(filePath.lastIndexOf(".")+1,filePath.length()).toLowerCase();
        if(endName.equals("jpg")||endName.equals("png")|| endName.equals("gif")||endName.equals("jpeg")||endName.equals("bmp")){
            isImageFile=true;
        }else {
            isImageFile=false;
        }
        return isImageFile;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermission(getActivity());
        mImagePathList=getImagePathFromSD();
        if(mImagePathList!=null){
            mUrlStrs=new String[mImagePathList.size()];
            mImagePathList.toArray(mUrlStrs);
        }
        mImageLoader=ImageLoader.getInstance(3,ImageLoader.Type.LIFO);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_list_imgs,container,false);
        mGridView=view.findViewById(R.id.id_gridview);
        setUpAdapter();
        return view;
    }

    private void setUpAdapter(){
        if(getActivity()==null || mGridView==null){
            return;
        }
        if(mUrlStrs!=null){
            mGridView.setAdapter(new ListImgItemAdapter(getActivity(),0,mUrlStrs));
        }else{
            mGridView.setAdapter(null);
        }
    }

    private class ListImgItemAdapter extends ArrayAdapter<String>{
        public ListImgItemAdapter(Context context,int resource,String[] data){
            super(getActivity(),0,data);
            Log.e("songsong","ListImgItemAdapter");
        }


        @Override
        public View getView(int position,View convertView,ViewGroup parent) {
            if(convertView==null){
                convertView=getActivity().getLayoutInflater().inflate(R.layout.item_fragment_list_imags,parent,false);
            }
            ImageView imageView=convertView.findViewById(R.id.id_image);
            imageView.setImageResource(R.drawable.person1);
            Log.e(TAG,getItem(position));
            mImageLoader.loadImage(getItem(position),imageView,false);
            return convertView;
        }
    }
}
