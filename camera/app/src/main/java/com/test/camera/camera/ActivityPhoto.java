package com.test.camera.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

/**
 * 主要实现拍照获取图片，从相册中获取，图片的裁剪
 */
public class ActivityPhoto extends AppCompatActivity implements View.OnClickListener{

    /** 拍照成功请求码*/
    public static int CAMERA_SUCCESS_CODE = 1;
    /** 选择图片的请求码*/
    public static int PICTURE_SUCCESS_CODE = 2;
    /** 进行裁剪的请求码*/
    public static int CROP_SUCCESS_CODE = 3;

    /** 是否需要进行裁剪*/
    private boolean isAllowCrop = false;

    /** 当调用系统相机的时候，设置拍照后图片保存的Uri*/
    private Uri uri;

    /** 选择相机 和 选择图片*/
    private TextView tvCamera , tvPickPhoto;

    /** 选择图片之后的展示控件*/
    private ImageView imgShowPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        initView();
        addClickListener();
    }

    /**
     * 控件的关联和初始化
     */
    private void initView() {
        tvCamera = (TextView) findViewById(R.id.tv_camera);
        tvPickPhoto = (TextView) findViewById(R.id.tv_pickphoto);
        imgShowPhoto = (ImageView) findViewById(R.id.img_showphoto);
    }

    /**
     * 添加点击监听
     */
    private void addClickListener() {
        tvCamera.setOnClickListener(this);
        tvPickPhoto.setOnClickListener(this);
        imgShowPhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //去检查是否获得权限，主要是由于6.0需要运行时权限，仅在manifest中设置不可以
        if(boolOpenCarmer()){
            //点击照相的时候
            if(v == tvCamera){
                toOpenSystemCamera();
            }
            //点击相册的时候
            else if(v == tvPickPhoto) {
                toOpenSystemPicture();
            }
        }
    }

    /**
     * 去检查是否打开相机、sd卡的权限，没有权限时获取权限，针对6.0以后
     */
    private boolean boolOpenCarmer(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this
                    , Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
            //去申请权限
            ActivityCompat.requestPermissions(this
                    , new String[]{Manifest.permission.CAMERA
                            ,Manifest.permission.READ_EXTERNAL_STORAGE
                            ,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }else{
            return true;
        }
    }

    /**
     * 去调用系统相机
     */
    private void toOpenSystemCamera() {
        //调用拍照活动
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        //设置图片的输出地址
        intent.putExtra(MediaStore.EXTRA_OUTPUT, toCreateImageUri());
        startActivityForResult(intent, CAMERA_SUCCESS_CODE);
    }

    /**
     * 去创建一个拍照后文件输出的位置
     */
    private Uri toCreateImageUri() {
        File file = new File(Environment.getExternalStorageDirectory()
                , System.currentTimeMillis()+"cameratest.jpg");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uri = Uri.fromFile(file);
        return uri;
    }

    /**
     * 打开系统相册
     */
    private void toOpenSystemPicture() {
        //打开系统资源库
        Intent intentSel =
                new Intent(Intent.ACTION_PICK
                        , MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentSel, PICTURE_SUCCESS_CODE);
    }

    /**
     * 打开系统的剪切界面
     */
    private void toCropPicture(Uri iamgeUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(iamgeUri, "image/*");
        //是否可以缩放
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, iamgeUri);
        startActivityForResult(intent, CROP_SUCCESS_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拍照成功后回传
        if(requestCode == CAMERA_SUCCESS_CODE) {
            /**
             * 1.注意拍照后data == null（手里的测试机如此，不清楚是否所有机型都如此）
             * 2.理论上data中应该包含拍照后图片的bitmap信息，但是规定intent最大携带数据1M，所以会出现这种情况（个人观点）
             * 3.刚刚拍照后的图片的uri就是刚才我们调用系统相机设置文件的输出uri，这也是为什么我们调用系统相机 最好配置一下输出位置的好处
             */
            if(data == null){
                if(isAllowCrop){
                    toCropPicture(uri);
                }
                //不需要裁剪，拍照后立刻显示图片
                else{
                    showPhoto(uri);
                }
            }
        }
        //选择图片成功后的回传
        else if(requestCode == PICTURE_SUCCESS_CODE) {
            /**
             * 1.在图片选择之后data 中携带着已选择图片得URI信息（还没有发现data == null的情况）
             */
            if(data != null){
                uri = data.getData();
                if(isAllowCrop){
                    toCropPicture(uri);
                }
                //不需要裁剪，选择图片之后直接显示图片到界面
                else{
                    showPhoto(uri);
                }
            }
        }
        //裁剪完图片之后的返回
        else if(requestCode == CROP_SUCCESS_CODE){
            showPhoto(uri);
        }
    }

    /**
     *
     * 将照相或者相册的图片显示到界面控件上
     *
     * @param photoUri 图片的Uri:
     *
     */
    private void showPhoto(Uri photoUri) {
        Glide.with(this).load(photoUri).into(imgShowPhoto);
    }

}
