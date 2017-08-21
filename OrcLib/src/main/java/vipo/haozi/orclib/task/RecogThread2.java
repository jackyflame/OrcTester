package vipo.haozi.orclib.task;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import vipo.haozi.orclib.ocrtools.CGlobal;
import vipo.haozi.orclib.utils.CameraUtils;
import vipo.haozi.orclib.utils.OpenCvHelper;
import vipo.haozi.orclib.utils.TessHelper;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 11:54
 */

public class RecogThread2 extends Thread{

    private static final String TAG = "RecogThread2";
    /**关闭标记*/
    private static boolean stopRecogMark = false;
    //初始化扫描区域缓存
    private Rect scanareaRect;
    //时间计算戳
    private long time;
    //时间计算戳
    private long mattime;
    //刷新识别区域标记
    private boolean isRefreshScanarea = true;

    //摄像头引用
    private Camera camera;
    //识别范围标记View
    private View iv_camera_scanarea;

    //识别回调
    private RecogTaskListener recogTaskListener;
    // 扫描视频数据
    private ArrayBlockingQueue<byte[]> mPreviewQueue = new ArrayBlockingQueue<>(1);

    public RecogThread2(Camera camera, View iv_camera_scanarea, RecogTaskListener recogTaskListener) {
        this.camera = camera;
        this.iv_camera_scanarea = iv_camera_scanarea;
        this.recogTaskListener = recogTaskListener;
    }

    @Override
    public void run() {
        try{
            while (true) {
                //如果关闭则退出识别
                if (checkRecogStop()) {
                    mPreviewQueue.clear();
                    return;
                }
                //获取数据
                Log.i(TAG, "------------->>take preview start");
                byte[] previewImgData = mPreviewQueue.take();
                Log.i(TAG, "------------->>take preview end");
                //记录起始时间
                time = System.currentTimeMillis();
                //转换原始图像数据
                int width = camera.getParameters().getPreviewSize().width;
                int height = camera.getParameters().getPreviewSize().height;
                //检查数据是否为空
                if (previewImgData == null || checkRecogStop()) {
                    continue;
                }
                //重新设置识别区域和识别OCRID
                if (isRefreshScanarea || scanareaRect == null) {
                    //初始化扫描区域缓存
                    scanareaRect = new Rect();
                    //获取扫描坐标
                    iv_camera_scanarea.getGlobalVisibleRect(scanareaRect);
                }
                //检查是否停止识别
                if (checkRecogStop()) {
                    return;
                }
                ////识别
                Rect scropRect = CGlobal.GetRotateRect(scanareaRect, 90);
                Bitmap imgBitmap = CGlobal.makeCropedGrayBitmap(previewImgData, width, height, 90, scropRect);
                //检查是否停止识别
                if (checkRecogStop()) {
                    return;
                }
                //拆分识别区域
                OpenCvHelper.getInstance().mattingImage(imgBitmap, new OpenCvCallback());
                Log.i(TAG, "------------->>Recoging finished...！！！！！");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void stopScan(){
        stopRecogMark = true;
        if(mPreviewQueue != null){
            mPreviewQueue.clear();
        }
    }

    public void setRecogTaskListener(RecogTaskListener recogTaskListener) {
        this.recogTaskListener = recogTaskListener;
    }

    /**图像处理回调*/
    protected class OpenCvCallback implements OpenCvHelper.ImageMattingCallback{

        @Override
        public void onImageProcessing(ArrayList<MatOfPoint> paramList, Mat paramMat, Bitmap rstMap) {
            int i = 0;
            org.opencv.core.Rect localq = null;
            //检查是否停止识别
            if(checkRecogStop()){return;}
            //检查识别结果
            if (paramList.size() <= 0 || i >= paramList.size()){
                if(recogTaskListener != null){
                    Exception exception = new Exception("ImageMatting for List<MatOfPoint> failed!!!");
                    recogTaskListener.recogError(exception);
                }
                return;
            }
            Log.i(TAG, "----->>starting recog from MatOfPoint list["+paramList.size()+"]");
            while (i < paramList.size()){
                mattime = System.currentTimeMillis();
                //计算轮廓的垂直边界最小矩形
                localq = Imgproc.boundingRect(paramList.get(i++));
                if (localq == null){
                    continue;
                }
                if ((localq.width / localq.height < 5) || (localq.height < 20)){
                    continue;
                }
                //检查是否停止识别
                if(checkRecogStop()){return;}
                //组装识别区域
                Log.i(TAG, "rect " + localq.toString() + "   rect-->tl " + localq.tl() + "    rect-->br " + localq.br());
                Mat localMat = new Mat(paramMat, localq);
                Bitmap localBitmap = Bitmap.createBitmap(localMat.width(), localMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(localMat.clone(), localBitmap);
                //如果成功识别电话号码，直接完成识别
                if (TessHelper.isMobilePhone(recogOnTessert(localBitmap))){
                    break;
                }else if((System.currentTimeMillis() - time) > 2000){
                    Log.i(TAG, "recog out of time ");
                    break;
                }
            }
            Log.i(TAG, "------------->>Recoging finished["+"识别时间:" + (System.currentTimeMillis() - time) + " ms]");
        }
    }

    public static boolean isRecogStoped() {
        return stopRecogMark;
    }

    private boolean checkRecogStop(){
        if(isRecogStoped() == true){
            if(recogTaskListener != null){
                Exception exception = new Exception("Recog stop by user...");
                recogTaskListener.recogError(exception);
            }
            return true;
        }
        return false;
    }

    private String recogOnTessert(Bitmap localBitmap){
        //进行识别
        String strRst = doOcr(localBitmap);
        //记录识别时间
        Log.i(TAG,"识别时间:" + (System.currentTimeMillis() - mattime) + " ms");
        String[] rcgArray = strRst.split("[^0-9]");
        for(String str : rcgArray){
            if(TessHelper.isMobilePhone(str)){
                strRst = str;
                break;
            }
        }
        //返回结果
        if(recogTaskListener != null){
            Log.i(TAG,"recogTaskListener callback");
            recogTaskListener.recogSuccess(strRst,localBitmap);
        }
        return strRst;
    }

    /**
     * 识别图像
     * */
    private String doOcr(Bitmap localBitmap){
        //检查是否停止识别
        if(checkRecogStop()){return "read canceled";}
        try {
            TessHelper.getTessBaseAPI().setImage(localBitmap);
        }catch (RuntimeException e){
            if(recogTaskListener != null){
                Exception exception = new Exception("read failed");
                recogTaskListener.recogError(exception);
            }
            return "read failed";
        }
        //识别文字内容
        String recogResultString = TessHelper.getTessBaseAPI().getUTF8Text();
        //清理识别缓存
        TessHelper.getTessBaseAPI().clear();
        //保存识别结果图
        CameraUtils.saveBitmap(localBitmap);
        //打印日志
        Log.i(TAG, "doOcr: "+recogResultString);
        //返回识别结果
        return recogResultString;
    }

    public void addDetect(byte[] data) {
        if (mPreviewQueue.size() >= 1) {
            mPreviewQueue.clear();
        }
        mPreviewQueue.add(data);
    }
}
