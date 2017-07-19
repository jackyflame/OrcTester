package vipo.haozi.orclib.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/19
 * Time: 11:30
 */

public class OpenCvHelper {

    private static final String TAG = "OpenCv";

    private BaseLoaderCallback mLoaderCallback;

    public static OpenCvHelper getInstance() {
        return OpenCvHelper.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        /***
         * 单例对象实例
         */
        static final OpenCvHelper INSTANCE = new OpenCvHelper();
    }

    public void mattingImage(Bitmap paramBitmap, ImageMattingCallback callback){

        if(paramBitmap == null){
            Log.e(TAG, "mattingImage: null paramBitmap!!!");
            return;
        }

        Mat srcMat = new Mat();
        Mat localMat2 = new Mat();
        Mat localMat3 = new Mat();
        //创建轮廓区域列表缓存
        ArrayList<MatOfPoint> localArrayList = new ArrayList();

        //讲bitmap转换成Mat数据
        Utils.bitmapToMat(paramBitmap, srcMat);
        //克隆原始数据
        Mat localRetrun = srcMat.clone();
        //颜色空间的转换
        Imgproc.cvtColor(srcMat, localMat2, 7);

        ////创建一个空白的对应尺寸的bitmap
        //Bitmap a = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        ////将Mat数据转换为bitmap
        //Utils.matToBitmap(localMat2, a);

        //自适应二值化(src:要二值化的灰度图,dst:二值化后的图,maxValue:二值化后要设置的那个值,
        //method:块计算的方法[ADAPTIVE_THRESH_MEAN_C 平均值，ADAPTIVE_THRESH_GAUSSIAN_C 高斯分布加权和]),
        //type:二值化类型（CV_THRESH_BINARY 大于为最大值，CV_THRESH_BINARY_INV 小于为最大值）,
        //blockSize:块大小（奇数，大于1）,delta:差值（负值也可以）)
        //Imgproc.adaptiveThreshold(localMat2, localMat3, 255.0D, 1, 0, 31, 15.0D);
        Imgproc.adaptiveThreshold(localMat2, localMat3, 255.0D, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 15.0D);
        ////创建新的对应尺寸的空白bitmap
        //Bitmap b = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        ////将Mat数据转换为bitmap
        //Utils.matToBitmap(localMat3, b);
        //定义新的缓存矩阵数据
        Mat localMat5 = new Mat();
        //定义一个合适大小的核
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20.0D, 3.0D));
        //扩大暗区（腐蚀）
        Imgproc.erode(localMat3, localMat5, kernelErode);
        ////创建新的对应尺寸的空白bitmap
        //Bitmap c = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        ////Mat转Bitmap
        //Utils.matToBitmap(localMat5, c);

        //定义新的缓存矩阵数据
        Mat localMat6 = new Mat();
        //边缘提取(image参数为已经二值化的原图;contours参数为检测的轮廓数组，每一个轮廓用一个MatOfPoint类型的List表示；
        // hiararchy参数和轮廓个数相同mode表示轮廓的检索模式[CV_RETR_EXTERNAL表示只检测外轮廓,CV_RETR_LIST检测的轮廓不建立等级关系,
        // CV_RETR_CCOMP建立两个等级的轮廓，上面的一层为外边界，里面的一层为内孔的边界信息。如果内孔内还有一个连通物体，这个物体的边界也在顶层。
        // CV_RETR_TREE建立一个等级树结构的轮廓。])
        Imgproc.findContours(localMat5.clone(), localArrayList, localMat6, 3, 1);

        ////绘制轮廓
        //Imgproc.drawContours(localMat3, localArrayList, -1, new Scalar(0.0D, 0.0D, 255.0D));
        ////绘制最终图像
        //Bitmap d = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.RGB_565);
        //Utils.matToBitmap(localMat3, d);

        //调用回调函数
        if (callback != null){
            callback.onImageProcessing(localArrayList, localRetrun, null);
        }
    }

    public interface ImageMattingCallback{
        void onImageProcessing(ArrayList<MatOfPoint> localArrayList,Mat localMat,Bitmap rstMap);
    }

    public void initOpencv(Activity paramActivity){

        final String simpleName = paramActivity.getClass().getSimpleName();

        mLoaderCallback = new BaseLoaderCallback(paramActivity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(TAG+"("+simpleName+")", "OpenCV loaded successfully");
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG+"("+simpleName+")", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, paramActivity, mLoaderCallback);
        } else {
            Log.d(TAG+"("+simpleName+")", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}
