package vipo.haozi.orclib.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import vipo.haozi.orclib.R;
import vipo.haozi.orclib.task.RecogTaskListener;
import vipo.haozi.orclib.task.RecogThread;
import vipo.haozi.orclib.task.ThreadMangaer;
import vipo.haozi.orclib.utils.CameraParametersUtils;
import vipo.haozi.orclib.utils.CameraSetting;
import vipo.haozi.orclib.utils.OpenCvHelper;
import vipo.haozi.orclib.utils.SharedPreferencesHelper;
import vipo.haozi.orclib.utils.TessHelper;

import static android.Manifest.permission.CAMERA;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 10:51
 */

public class PhoneScanBaseActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener{

    private static final String TAG = "PhoneScan";
    /**恢复扫描*/
    private static final int HANDLER_RECOVERSCAN = 8008;
    private static final int HANDLER_SCANRST_UPDATE = 8001;

    /**屏幕宽高*/
    private int srcWidth, srcHeight;
    /**预览画面管理器*/
    private SurfaceHolder surfaceHolder;
    /**摄像头引用*/
    private Camera camera;
    /**尺寸缓存*/
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    /**操作标记：操作中不进行识别*/
    private boolean isTouch = false;
    /**扫描视频数据缓存*/
    private byte[] previewImgData;
    /**识别结果返回值*/
    private String recogStr;
    private Bitmap recogBitmap;

    private SurfaceView surfaceView;
    private View iv_camera_scanarea;
    private ImageView iv_camera_back, iv_camera_flash,img_rst;
    private ImageButton imbtn_takepic;
    private TextView txv_rst;
    private Vibrator mVibrator;

    /**是否截图标记*/
    private boolean isTakePic = false;
    /**是否识别成功标记*/
    protected boolean isRecogSuccess;
    /**设置是否刷新扫描区域参数*/
    private boolean isRefreshScanarea = true;
    /**UI线程处理器*/
    private Handler mHandler;
    /**识别进程*/
    private RecogThread recogThread;
    /**自动对焦计时器*/
    private TimerTask autoFoucusTimer;
    /**对焦时间计时器*/
    private Timer focustimeAuto;

    /**
     * (non-Javadoc)
     * @see Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //加载资源文件到指定目录
        TessHelper.getInstance().init(this);
        RecogThread.stopAllScan();

        if(isCameraGranted() == false){
            //提示权限不足
            Intent intent = new Intent(this, ErrorAuthorityDailogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        initContent();
        initView();
        initData();
        Log.i(TAG,"PhoneScanBaseOldActivity create success!");
    }

    public boolean isCameraGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED;
    }

    protected void initContent(){
        initContent(R.layout.activity_phonescan_base);
    }

    protected void initContent(int layoutId){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(layoutId > 0){
            setContentView(layoutId);
        }

        CameraSetting.getInstance(this).hiddenVirtualButtons(getWindow().getDecorView());
    }

    protected void initView(){
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        CameraParametersUtils cameraParametersUtils = new CameraParametersUtils(this);
        srcWidth = cameraParametersUtils.srcWidth;
        srcHeight = cameraParametersUtils.srcHeight;

        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview_camera);
        imbtn_takepic = (ImageButton) this.findViewById(R.id.imbtn_takepic);
        iv_camera_back = (ImageView) this.findViewById(R.id.iv_camera_back);
        iv_camera_flash = (ImageView) this.findViewById(R.id.iv_camera_flash);
        img_rst = (ImageView) this.findViewById(R.id.img_rst);
        txv_rst = (TextView) this.findViewById(R.id.txv_rst);
        iv_camera_scanarea = this.findViewById(R.id.iv_camera_scanarea);

        if (srcWidth == surfaceView.getWidth() || surfaceView.getWidth() == 0) {
            RelativeLayout.LayoutParams  layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, srcHeight);
            surfaceView.setLayoutParams(layoutParams);
        }else if (srcWidth > surfaceView.getWidth()) {
            // 如果将虚拟硬件弹出则执行如下布局代码，相机预览分辨率不变压缩屏幕的高度
            int surfaceViewHeight = (surfaceView.getWidth() * srcHeight) / srcWidth;
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, surfaceViewHeight);
            layoutParams.topMargin = (srcHeight - surfaceViewHeight) / 2;
            surfaceView.setLayoutParams(layoutParams);
        }

        if(imbtn_takepic != null){
            imbtn_takepic.setOnClickListener(this);
        }
        iv_camera_back.setOnClickListener(this);
        iv_camera_flash.setOnClickListener(this);
    }

    protected void initData(){
        //initRecogConn();
        //设置预览引用控制类
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public Handler getHandler(){
        if(mHandler == null){
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 1) {
                        if (isRecogSuccess) {
                            if(imbtn_takepic != null){
                                imbtn_takepic.setVisibility(View.GONE);
                            }
                        }
                    }else if (msg.what == 3) {
                        isRefreshScanarea = true;
                    }else if(msg.what == HANDLER_RECOVERSCAN){
                        //恢复自动识别功能
                        continueScan();
                    }else if(msg.what == HANDLER_SCANRST_UPDATE){
                        //震动提醒扫码成功
                        if (TessHelper.isMobilePhone(recogStr)){
                            //震动提醒扫码成功
                            if(mVibrator == null){
                                mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                            }
                            mVibrator.vibrate(200);
                            //UI反馈操作
                            txv_rst.setText(recogStr);
                            img_rst.setImageBitmap(recogBitmap);
                        }else{
                            String[] rcgArray = recogStr.split("[^0-9]");
                            String finalStr = recogStr;
                            for(String str : rcgArray){
                                if(TessHelper.isMobilePhone(str)){
                                    finalStr = str;
                                    //震动提醒扫码成功
                                    if(mVibrator == null){
                                        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                                    }
                                    mVibrator.vibrate(200);
                                    break;
                                }
                            }
                            //UI反馈操作
                            txv_rst.setText(finalStr);
                            img_rst.setImageBitmap(recogBitmap);
                        }
                    }
                }
            };
        }
        return mHandler;
    }

    /**
     * 刷新闪光灯开关
     * */
    protected void refreshFlashIcon(boolean isOn,int iconRes){
        if(iconRes <= 0){
            if(isOn == false){
                iv_camera_flash.setImageResource(R.drawable.flash_on);
            }else{
                iv_camera_flash.setImageResource(R.drawable.flash_off);
            }
        }else{
            iv_camera_flash.setImageResource(iconRes);
        }
    }

    protected void pauseScan(){
        if(recogThread != null){
            recogThread.stopScan();
        }
    }

    protected void continueScan(){
        if(recogThread != null){
            recogThread.continueScan();
        }
    }

    /**
     * 1秒以后恢复自动识别
     * */
    protected void continueScanDely(){
        continueScanDely(1000);
    }

    /**
     * delyMsec毫秒以后恢复识别功能（防止极短时间内重复识别）
     * */
    protected void continueScanDely(long delyMsec){
        getHandler().sendEmptyMessageDelayed(HANDLER_RECOVERSCAN,delyMsec);
    }

    public void OpenCameraAndSetParameters() {
        try {
            if (null == camera) {
                //打开摄像头
                camera = Camera.open();
                //设置参数
                CameraSetting.getInstance(this).setCameraParameters(this, surfaceHolder, this,
                        camera, (float) srcWidth / srcHeight, false);
                //自动对焦
                if (autoFoucusTimer == null) {
                    autoFoucusTimer = new TimerTask() {
                        public void run() {
                            if (camera != null) {
                                try {
                                    autoFocus();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                }
                //开始执行周期性对焦
                if (focustimeAuto == null) {
                    focustimeAuto = new Timer();
                }
                focustimeAuto.schedule(autoFoucusTimer, 200, 2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"打开摄像头失败",Toast.LENGTH_SHORT).show();
            if(camera != null){
                CloseCameraAndStopTimer();
            }
            //提示权限不足
            Intent intent = new Intent(this, ErrorAuthorityDailogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    public void CloseCameraAndStopTimer() {
        isTouch = true;
        if (autoFoucusTimer != null) {
            autoFoucusTimer.cancel();
            autoFoucusTimer = null;
        }
        if (focustimeAuto != null) {
            focustimeAuto.cancel();
            focustimeAuto = null;
        }
        TessHelper.getTessBaseAPI().end();
        if (camera != null) {
            camera = CameraSetting.getInstance(this).closeCamera(camera);
        }
    }

    public void autoFocus() {
        if (camera != null) {
            try {
                if (camera.getParameters().getSupportedFocusModes() != null &&
                        camera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                //对焦成功
                            }
                        }
                    });
                } else {
                    Toast.makeText(getBaseContext(),getString(R.string.unsupport_auto_focus),Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                camera.stopPreview();
                camera.startPreview();
                Toast.makeText(this, R.string.toast_autofocus_failure,Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecog(byte[] data, Camera camera){
        //初始化识别进程
        recogThread = new RecogThread(camera, iv_camera_scanarea, data, new RecogTaskListener() {
            @Override
            public void recogSuccess(String recogResultString, Bitmap localBitmap) {
                if ((recogResultString != null && !recogResultString.equals(""))) {
                    recogStr = recogResultString;
                    recogBitmap = localBitmap;
                    //拍照标记更新
                    isTakePic = false;
                    //刷新页面
                    getHandler().sendEmptyMessage(HANDLER_SCANRST_UPDATE);
                }
            }
            @Override
            public void recogError(Exception e) {}
        });
        ThreadMangaer.getInstance().excuteTaskInSingle(recogThread);
    }

    private Runnable logTask;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isTouch || camera == null) {
            return;
        }
        previewImgData = data;
        //进行判断
        if(RecogThread.isRecoging() || RecogThread.isRecogStoped()){
            if(logTask != null){
                getHandler().removeCallbacks(logTask);
            }
            logTask = new Runnable() {
                @Override
                public void run() {
                    if(RecogThread.isRecoging()){
                        Log.i(TAG, "onPreviewFrame: isRecoging");
                    }else{
                        Log.i(TAG, "onPreviewFrame: isRecogStoped");
                    }
                }
            };
            getHandler().postDelayed(logTask,1000);
            return;
        }
        startRecog(data,camera);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraSetting.getInstance(this).setCameraParameters(this, surfaceHolder, this,  camera, (float) srcWidth / srcHeight, false);
        if (SharedPreferencesHelper.getBoolean(this, "isOpenFlash", false)) {
            refreshFlashIcon(true,R.drawable.flash_off);
            CameraSetting.getInstance(this).openCameraFlash(camera);
        } else {
            refreshFlashIcon(false,R.drawable.flash_on);
            CameraSetting.getInstance(this).closedCameraFlash(camera);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Message msg = new Message();
        msg.what = 1;
        getHandler().sendMessage(msg);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_camera_flash) {
            if (SharedPreferencesHelper.getBoolean(this,"isOpenFlash", false)) {
                refreshFlashIcon(false,R.drawable.flash_on);
                SharedPreferencesHelper.putBoolean(this,"isOpenFlash", false);
                CameraSetting.getInstance(this).closedCameraFlash(camera);
            } else {
                SharedPreferencesHelper.putBoolean(this,"isOpenFlash", true);
                refreshFlashIcon(true,R.drawable.flash_off);
                CameraSetting.getInstance(this).openCameraFlash(camera);
            }
            // 返回按钮触发事件
        } else if (view.getId() == R.id.iv_camera_back) {
            CloseCameraAndStopTimer();
            overridePendingTransition(
                    getResources().getIdentifier("zoom_enter", "anim",getApplication().getPackageName()),
                    getResources().getIdentifier("push_down_out", "anim",getApplication().getPackageName()));
            this.finish();
        } else if (view.getId() == R.id.imbtn_takepic) {
            if(previewImgData != null){
                startRecog(previewImgData,camera);
            }
        }
    }

    /**监听返回键事件*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            CloseCameraAndStopTimer();
            overridePendingTransition(
                    getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()),
                    getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
            this.finish();
            return true;
        }
        return true;
    }

    /**
     * 小米PAD 解锁屏时执行surfaceChanged
     * surfaceCreated，容易出现超时卡死现象，
     * 故在此处打开相机和设置参数
     */
    @Override
    protected void onResume() {
        super.onResume();
        OpenCameraAndSetParameters();
        OpenCvHelper.getInstance().initOpencv(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        CloseCameraAndStopTimer();
    }
}
