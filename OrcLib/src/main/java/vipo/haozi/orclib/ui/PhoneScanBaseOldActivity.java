package vipo.haozi.orclib.ui;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
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

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import vipo.haozi.orclib.R;
import vipo.haozi.orclib.task.ThreadMangaer;
import vipo.haozi.orclib.utils.CameraParametersUtils;
import vipo.haozi.orclib.utils.CameraSetting;
import vipo.haozi.orclib.utils.CameraUtils;
import vipo.haozi.orclib.utils.CameraViewRotateUtils;
import vipo.haozi.orclib.utils.ImageFilterUtils;
import vipo.haozi.orclib.utils.OpenCvHelper;
import vipo.haozi.orclib.utils.SharedPreferencesHelper;
import vipo.haozi.orclib.utils.TessHelper;

import static android.Manifest.permission.CAMERA;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

/**
 * Created by Android Studio.
 * ProjectName: shenbian_android_cloud_speaker
 * Author: yh
 * Date: 2016/12/5
 * Time: 16:05
 */
public class PhoneScanBaseOldActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener{

    private static final String TAG = "PhoneScan";
    private static final int HANDLER_RECOVERSCAN = 8008;

    private int srcWidth, srcHeight;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private boolean isTouch = false;//操作中不进行识别

    private boolean isTakePic = false;//是否截图
    private Handler mHandler;
    private TimerTask autoFoucusTimer;
    private Timer focustimeAuto;
    protected boolean isRecogSuccess;
    private RecogThread recogThread;
    private ServiceConnection recogConn;

    // 扫描区域坐标
    private int[] regionPos = new int[4];
    // 设置是否刷新扫描区域参数
    private boolean isRefreshScanarea = true;
    // 预览区域尺寸
    private Camera.Size priviewSize;
    // 扫描视频数据
    private byte[] previewImgData;
    // 识别结果返回值
    private int returnResult = -1;
    // 返回结果中的字符数
    private int[] nCharCount = new int[2];
    // 返回结果图片保存路径
    private String SavePicPath;

    private View iv_camera_scanarea;
    private ImageView iv_camera_back, iv_camera_flash,img_rst;
    private ImageButton imbtn_takepic;
    private TextView txv_rst;
    private Vibrator mVibrator;

    /**
     * (non-Javadoc)
     * @see Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //加载资源文件到指定目录
        TessHelper.getInstance().init(this);

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
                        initView();
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
                    }
                }
            };
        }
        return mHandler;
    }

    public ServiceConnection initRecogConn() {
        if(recogConn == null){
            recogConn = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    recogConn = null;
                }
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    try{
//                        recogBinder = (RecogService.MyBinder) service;
//                        iTH_InitSmartVisionSDK = recogBinder.getInitSmartVisionOcrSDK();
//                        if (iTH_InitSmartVisionSDK == 0) {
//                            recogBinder.AddTemplateFile();//添加识别模板
//                            recogBinder.SetCurrentTemplate(ORC_ID);//设置当前识别模板ID
//                            Log.i(TAG,"OCR 核心初始化成功!");
//                        } else {
//                            String log = "核心初始化失败，错误码：" + iTH_InitSmartVisionSDK;
//                            Toast.makeText(PhoneScanBaseOldActivity.this,log,Toast.LENGTH_LONG).show();
//                            Log.i(TAG,log);
//                        }
                    }catch (Exception e){
                        Toast.makeText(PhoneScanBaseOldActivity.this,"核心初始化错误",Toast.LENGTH_LONG).show();
                        Log.i(TAG,"核心初始化错误" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
        }
        return recogConn;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isTouch || camera == null) {
            return;
        }
        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        previewImgData = CameraViewRotateUtils.rotateYUV420Degree90(data,width, height);
        //进行识别
        synchronized (recogThread) {
            ThreadMangaer.getInstance().excuteTaskInSingle(recogThread);
        }
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
        recogThread = new RecogThread();
    }

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

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Message msg = new Message();
        msg.what = 1;
        getHandler().sendMessage(msg);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

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
            isTakePic = true;
            //Message msg = new Message();
            //msg.what = 3;
            //getHandler().sendMessage(msg);
        }
    }

    public void autoFocus() {
        if (camera != null) {
            try {
                if (camera.getParameters().getSupportedFocusModes() != null
                        && camera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
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

    // 监听返回键事件
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

    @Override
    protected void onStart() {
        super.onStart();
        isTouch = false;
        isTakePic = false;
        isRefreshScanarea = true;
//        //绑定识别Service
//        if (recogBinder == null) {
//            Intent authIntent = new Intent(this, RecogService.class);
//            bindService(authIntent, recogConn, Service.BIND_AUTO_CREATE);
//        }
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

    @Override
    protected void onDestroy() {
//        if (recogBinder != null) {
//            unbindService(recogConn);
//            recogBinder = null;
//        }
        super.onDestroy();
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
                focustimeAuto.schedule(autoFoucusTimer, 200, 2500);
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

    class RecogThread extends Thread {

        public long time;

        public RecogThread() {
            time = 0;
        }

        @Override
        public void run() {
            if (isRecogSuccess) {
                return;
            }
            time = System.currentTimeMillis();
            //重新设置识别区域和识别OCRID
            if (isRefreshScanarea && camera != null) {
                //获取预览尺寸
                priviewSize = camera.getParameters().getPreviewSize();

                isRefreshScanarea = false;
            }

            String recogResultString = "";
            //初始化扫描区域缓存
            Rect scanareaRect = new Rect();
            //获取扫描坐标
            iv_camera_scanarea.getGlobalVisibleRect(scanareaRect);

            if (isTakePic) {
                //handleTackPic(scanareaRect);
                Bitmap imgBitmap= CameraUtils.getBitmapFromPreview(previewImgData, camera, scanareaRect);
                OpenCvHelper.getInstance().mattingImage(imgBitmap,new OpenCvCallback());
            } else {
                try{
                    //recogBinder.LoadStreamNV21(previewImgData, priviewSize.height, priviewSize.width);

                    //Bitmap imgBitmap= CameraUtils.getBitmapFromPreview(previewImgData,camera);
                    //imgBitmap = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    //TessHelper.getTessBaseAPI().setImage(imgBitmap);
                    //TessHelper.getTessBaseAPI().setRectangle(scanareaRect);
                    //// 获取返回值
                    //recogResultString = TessHelper.getTessBaseAPI().getUTF8Text();
                    //System.out.println("识别结果:" +recogResultString);
                    //TessHelper.getTessBaseAPI().end();
                    //TessHelper.getInstance().parseImageToString(TessConstantConfig.getTessDataDirectory()+"number.jpg");

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            //拍照标记更新
            isTakePic = false;

            if (returnResult == 0) {
                time = System.currentTimeMillis() - time;
                System.out.println("识别时间:" + time + " ms");
                if ((recogResultString != null && !recogResultString.equals("") && nCharCount[0] > 0) || isTakePic) {
                    //没有点击拍照按钮下保存图片
                    if (!isTakePic) {
                        ////2016-12-16更新，不保存图片到用户手机（产生无用图片）
                        //SavePicPath = savePicture();
                        ////上传图片处理
                        //if (SavePicPath != null && !"".equals(SavePicPath)) {}
                    }
                    if ((recogResultString == null || recogResultString.equals("")) && isTakePic) {
                        recogResultString = " ";
                    }
                    //震动提醒扫码成功
                    if(mVibrator == null){
                        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                    }
                    mVibrator.vibrate(200);
                    //拍照标记更新
                    isTakePic = false;
                    //暂停识别
                    pauseScan();
                    //处理结果
                    recogResultHandle(recogResultString);
                }else{
                    //Log.i(TAG,"识别失败");
                    //Toast.makeText(getApplicationContext(),"识别错误，错误码：" + returnResult, Toast.LENGTH_LONG).show();
                }
            }else{
                Log.i(TAG,"识别错误，错误码：" + returnResult);
            }
        }
    }

    protected String handleTackPic(Rect scanareaRect){
        //SavePicPath = savePicture();
        //SavePicPath = CameraUtils.savePreviewPic(previewImgData, camera, scanareaRect);
        //if (SavePicPath != null && !"".equals(SavePicPath)) {
        //    //recogBinder.LoadImageFile(SavePicPath);
        //}
        Bitmap imgBitmap= CameraUtils.getBitmapFromPreview(previewImgData,camera,scanareaRect);
        //imgBitmap =  ImageFilterUtils.gray2Binary(imgBitmap);// 图片二值化
        imgBitmap =  ImageFilterUtils.grayScaleImage(imgBitmap);// 图片灰度化

        //imgBitmap = ImageFilterUtils.cropScanImg(imgBitmap);

        img_rst.setImageBitmap(imgBitmap);
        TessHelper.getTessBaseAPI().setImage(imgBitmap);
        String recogResultString = TessHelper.getTessBaseAPI().getUTF8Text();

        //recogResultString = TessHelper.getTessBaseAPI().getResultIterator().getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        TessHelper.getTessBaseAPI().clear();
        txv_rst.setText(recogResultString);
        CameraUtils.saveBitmap(imgBitmap);
        //震动提醒扫码成功
        if(mVibrator == null){
            mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(200);

        return recogResultString;
    }

    /**
     * 识别结果处理
     * @param recogResultString 识别结果
     * */
    protected void recogResultHandle(String recogResultString){
        //取消自动对焦
        //if (autoFoucusTimer != null) {
        //    autoFoucusTimer.cancel();
        //    autoFoucusTimer = null;
        //}
        //关闭摄像头
        //camera = CameraSetting.getInstance(this).closeCamera(camera);

        //组装识别结果
        //ArrayList<String> list_recogSult = new ArrayList<>();
        //list_recogSult.add("电话:"+recogResultString);
        //ArrayList<String> savePath = new ArrayList<>();
        //savePath.add(SavePicPath);
        //跳转到结果页面
        //Intent intent = new Intent(PhoneScanBaseOldActivity.this, ShowResultActivity.class);
        //intent.putStringArrayListExtra("recogResult", list_recogSult);
        //intent.putStringArrayListExtra("savePath", savePath);
        //intent.putExtra("templateName",wlci.template.get(selectedTemplateTypePosition).templateName);
        //intent.putExtra("templateName","电话识别");
        //startActivity(intent);
        //overridePendingTransition(
        //        getResources().getIdentifier("zoom_enter", "anim",getApplication().getPackageName()),
        //        getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
        //finish();
    }

    public String savePicture() {
        String picPathString = "";
        String PATH = Environment.getExternalStorageDirectory().toString()+ "/DCIM/Camera/";
        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        String name = CameraUtils.pictureName();
        picPathString = PATH + "smartVisition" + name + ".jpg";

        //recogBinder.svSaveImageResLine(picPathString);

        return picPathString;
    }

    protected void pauseScan(){
        isRecogSuccess = true;
    }

    protected void continueScan(){
        isRecogSuccess = false;
    }

    protected void continueScanDely(){
        //1秒以后恢复自动识别
        continueScanDely(1000);
    }

    protected void continueScanDely(long delyMsec){
        //delyMsec毫秒以后恢复识别功能（防止极短时间内重复识别）
        getHandler().sendEmptyMessageDelayed(HANDLER_RECOVERSCAN,delyMsec);
    }

    protected class OpenCvCallback implements OpenCvHelper.ImageMattingCallback{

        private DisplayMetrics metric = new DisplayMetrics();
        private int width;
        private int height;

        public OpenCvCallback() {
            //获取屏幕的分辨率
            getWindowManager().getDefaultDisplay().getMetrics(metric);
        }

        @Override
        public void onImageProcessing(ArrayList<MatOfPoint> paramList, Mat paramMat,Bitmap rstMap) {
            int i = 0;
            org.opencv.core.Rect localq = null;
            if (paramList.size() <= 0 || i >= paramList.size()){
                //震动提醒扫码成功
                if(mVibrator == null){
                    mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                }
                mVibrator.vibrate(200);
                return;
            }
            while (i < paramList.size()){
                //计算轮廓的垂直边界最小矩形
                localq = Imgproc.boundingRect(paramList.get(i++));
                if (localq == null){
                    continue;
                }
                if ((localq.width / localq.height < 5) || (localq.height < 20)){
                   continue;
                }
                Log.i("gudd", "rect " + localq.toString() + "   rect-->tl " + localq.tl() + "    rect-->br " + localq.br());
                Mat localMat = new Mat(paramMat, localq);
                Bitmap localBitmap = Bitmap.createBitmap(localMat.width(), localMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(localMat.clone(), localBitmap);

//                this.width = this.metric.widthPixels;
//                this.height = (this.width / 11);
//                int j = localBitmap.getWidth();
//                int k = localBitmap.getHeight();
//                float f1 = this.width / j;
//                float f2 = this.height / k;
//                Matrix localMatrix = new Matrix();
//                localMatrix.postScale(f1, f2);
//                String strRst = doOcr(Bitmap.createBitmap(localBitmap, 0, 0, localBitmap.getWidth(), localBitmap.getHeight(), localMatrix, true));
                String strRst = doOcr(localBitmap);
                System.err.println("doOcr--->  " + strRst);
            }
            //doOcr(rstMap);
        }

        private String doOcr(Bitmap localBitmap){
            try {
                TessHelper.getTessBaseAPI().setImage(localBitmap);
            }catch (RuntimeException e){
                return "read failed";
            }
            String recogResultString = TessHelper.getTessBaseAPI().getUTF8Text();
            //recogResultString = TessHelper.getTessBaseAPI().getResultIterator().getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            TessHelper.getTessBaseAPI().clear();
            //UI反馈操作
            txv_rst.setText(recogResultString);
            img_rst.setImageBitmap(localBitmap);
            CameraUtils.saveBitmap(localBitmap);
            Log.i(TAG, "doOcr: "+recogResultString);
            //结果处理
            String strRst = recogResultString.replaceAll("[^0-9]", "");
            if (TessHelper.isMobilePhone(strRst)){
                //震动提醒扫码成功
                if(mVibrator == null){
                    mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                }
                mVibrator.vibrate(200);
            }
            //返回识别结果
            return recogResultString;
        }
    }
}
