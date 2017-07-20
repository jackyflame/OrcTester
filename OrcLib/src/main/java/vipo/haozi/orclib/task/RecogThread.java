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

import vipo.haozi.orclib.utils.CameraUtils;
import vipo.haozi.orclib.utils.CameraViewRotateUtils;
import vipo.haozi.orclib.utils.OpenCvHelper;
import vipo.haozi.orclib.utils.TessHelper;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 11:54
 */

public class RecogThread extends Thread implements RecognizeTaskItf{

    private static final String TAG = "RecogThread";
    /**关闭标记*/
    private static boolean stopRecogMark = false;
    /**关闭标记*/
    private static boolean recogingMark = false;
    //初始化扫描区域缓存
    private Rect scanareaRect;
    //时间计算戳
    private long time;
    //刷新识别区域标记
    private boolean isRefreshScanarea = true;
    // 扫描视频数据
    private byte[] previewImgData;

    //摄像头引用
    private Camera camera;
    //识别范围标记View
    private View iv_camera_scanarea;

    //识别回调
    private RecogTaskListener recogTaskListener;

    public RecogThread(Camera camera, View iv_camera_scanarea,byte[] previewImgData, RecogTaskListener recogTaskListener) {
        this.camera = camera;
        this.iv_camera_scanarea = iv_camera_scanarea;
        this.recogTaskListener = recogTaskListener;
        this.previewImgData = previewImgData;
    }

    @Override
    public void run() {
        //如果关闭则退出识别
        if (checkRecogStop()) {
            previewImgData = null;
            return;
        }
        setRecogingStart();
        //记录起始时间
        time = System.currentTimeMillis();
        //转换原始图像数据
        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        //检查数据是否为空
        if(previewImgData == null || checkRecogStop()){
            previewImgData = null;
            return;
        }
        //转换图像
        byte[] tempData = CameraViewRotateUtils.rotateYUV420Degree90(previewImgData,width, height);
        //清空引用数据
        previewImgData = null;
        //重新设置识别区域和识别OCRID
        if (isRefreshScanarea || scanareaRect == null) {
            //初始化扫描区域缓存
            scanareaRect = new Rect();
            //获取扫描坐标
            iv_camera_scanarea.getGlobalVisibleRect(scanareaRect);
        }
        //裁剪图形区域进行识别
        try{
            //检查是否停止识别
            if(checkRecogStop()){return;}
            //识别
            Bitmap imgBitmap= CameraUtils.getBitmapFromPreview(tempData, camera, scanareaRect);
            //检查是否停止识别
            if(checkRecogStop()){return;}
            //拆分识别区域
            OpenCvHelper.getInstance().mattingImage(imgBitmap,new OpenCvCallback());

            //recogOnTessert(imgBitmap);
            //setRecogingStop();
            //Log.i(TAG, "------------->>Recoging finished...");
        }catch (Exception e){
            setRecogingStop();
            e.printStackTrace();
        }
    }

    @Override
    public void continueScan() {
        stopRecogMark = false;
    }

    public void stopScan(){
        stopRecogMark = true;
        if(previewImgData != null){
            previewImgData = null;
        }
    }

    public static void stopAllScan(){
        stopRecogMark = true;
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
                setRecogingStop();
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
                }
            }
            setRecogingStop();
            Log.i(TAG, "------------->>Recoging finished...");
        }
    }

    public static boolean isRecoging() {
        return recogingMark;
    }

    public static boolean isRecogStoped() {
        return stopRecogMark;
    }

    private static void setRecogingStop(){
        recogingMark = false;
    }

    private static void setRecogingStart(){
        Log.i(TAG, "------------->>Start Recoging...");
        recogingMark = true;
    }

    private boolean checkRecogStop(){
        if(isRecogStoped() == true){
            if(recogTaskListener != null){
                Exception exception = new Exception("Recog stop by user...");
                recogTaskListener.recogError(exception);
            }
            setRecogingStop();
            return false;
        }
        return true;
    }

    private String recogOnTessert(Bitmap localBitmap){
        //进行识别
        String strRst = doOcr(localBitmap);
        //记录识别时间
        time = System.currentTimeMillis() - time;
        Log.i(TAG,"识别时间:" + time + " ms");
        String[] rcgArray = strRst.split("[^0-9]");
        for(String str : rcgArray){
            if(TessHelper.isMobilePhone(str)){
                strRst = str;
                break;
            }
        }
        //返回结果
        if(recogTaskListener != null){
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
}
