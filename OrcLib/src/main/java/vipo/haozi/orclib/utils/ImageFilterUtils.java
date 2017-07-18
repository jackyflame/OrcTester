package vipo.haozi.orclib.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by Android Studio.
 * ProjectName: shenbian_android_cloud_speaker
 * Author: yh
 * Date: 2017/1/20
 * Time: 12:00
 */

public class ImageFilterUtils {

    // 该函数实现对图像进行二值化处理
    public static Bitmap gray2Binary(Bitmap graymap) {
        // 得到图形的宽度和长度
        int width = graymap.getWidth();
        int height = graymap.getHeight();
        // 创建二值化图像
        Bitmap binarymap = null;
        binarymap = graymap.copy(Bitmap.Config.ARGB_8888, true);
        // 依次循环，对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 得到当前像素的值
                int col = binarymap.getPixel(i, j);
                // 得到alpha通道的值
                int alpha = col & 0xFF000000;
                // 得到图像的像素RGB的值
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                // 对图像进行二值化处理
                if (gray <= 95) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 新的ARGB
                int newColor = alpha | (gray << 16) | (gray << 8) | gray;
                // 设置新图像的当前像素值
                binarymap.setPixel(i, j, newColor);
            }
        }
        return binarymap;
    }

    // 图像灰度化
    public static Bitmap grayScaleImage(Bitmap src) {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;

//      final double GS_RED = 0.235;
//      final double GS_GREEN = 0.589;
//      final double GS_BLUE = 0.119;

        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        // pixel information
        int A, R, G, B;
        int pixel;

        // get image size
        int width = src.getWidth();
        int height = src.getHeight();

        // scan through every single pixel
        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                // get one pixel color
                pixel = src.getPixel(x, y);
                // retrieve color of all channels
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                // take conversion up to one single value
                R = G = B = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return final image
        return bmOut;
    }

    // 对图像进行线性灰度变化
    public static Bitmap lineGrey(Bitmap image) {
        // 得到图像的宽度和长度
        int width = image.getWidth();
        int height = image.getHeight();
        // 创建线性拉升灰度图像
        Bitmap linegray = null;
        linegray = image.copy(Bitmap.Config.ARGB_8888, true);
        // 依次循环对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 得到每点的像素值
                int col = image.getPixel(i, j);
                int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                // 增加了图像的亮度
                red = (int) (1.1 * red + 30);
                green = (int) (1.1 * green + 30);
                blue = (int) (1.1 * blue + 30);
                // 对图像像素越界进行处理
                if (red >= 255) {
                    red = 255;
                }

                if (green >= 255) {
                    green = 255;
                }

                if (blue >= 255) {
                    blue = 255;
                }
                // 新的ARGB
                int newColor = alpha | (red << 16) | (green << 8) | blue;
                // 设置新图像的RGB值
                linegray.setPixel(i, j, newColor);
            }
        }
        return linegray;
    }

    // 图像灰度化
    public static Bitmap bitmap2Gray(Bitmap bmSrc) {
        // 得到图片的长和宽
        int width = bmSrc.getWidth();
        int height = bmSrc.getHeight();
        // 创建目标灰度图像
        Bitmap bmpGray = null;
        bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 创建画布
        Canvas c = new Canvas(bmpGray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmSrc, 0, 0, paint);
        return bmpGray;
    }

    /**
     * 将彩色图转换为黑白图
     * @param bmp
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }

    /**
     * 图片锐化（拉普拉斯变换）
     * @param bmp
     * @return
     */
    public static Bitmap sharpenImageAmeliorate(Bitmap bmp) {
        // 拉普拉斯矩阵
        int[] laplacian = new int[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 };
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        int pixR = 0;
        int pixG = 0;
        int pixB = 0;
        int pixColor = 0;
        int newR = 0;
        int newG = 0;
        int newB = 0;
        int idx = 0;
        float alpha = 0.3F;
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 1, length = height - 1; i < length; i++) {
            for (int k = 1, len = width - 1; k < len; k++) {
                idx = 0;
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        pixColor = pixels[(i + n) * width + k + m];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);
                        newR = newR + (int) (pixR * laplacian[idx] * alpha);
                        newG = newG + (int) (pixG * laplacian[idx] * alpha);
                        newB = newB + (int) (pixB * laplacian[idx] * alpha);
                        idx++;
                    }
                }
                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));
                pixels[i * width + k] = Color.argb(255, newR, newG, newB);
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static int[] convertYUV420_NV21toARGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int i = 0;

        for(int k = 0; i < size; k += 2) {
            int y1 = data[i] & 255;
            int y2 = data[i + 1] & 255;
            int y3 = data[width + i] & 255;
            int y4 = data[width + i + 1] & 255;
            int u = data[offset + k] & 255;
            int v = data[offset + k + 1] & 255;
            u -= 128;
            v -= 128;
            pixels[i] = convertYUVtoARGB(y1, u, v);
            pixels[i + 1] = convertYUVtoARGB(y2, u, v);
            pixels[width + i] = convertYUVtoARGB(y3, u, v);
            pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);
            if(i != 0 && (i + 2) % width == 0) {
                i += width;
            }

            i += 2;
        }

        return pixels;
    }

    private static int convertYUVtoARGB(int y, int u, int v) {
        int r = y + 1 * u;
        int g = y - (int)(0.344F * (float)v + 0.714F * (float)u);
        int b = y + 1 * v;
        r = r > 255?255:(r < 0?0:r);
        g = g > 255?255:(g < 0?0:g);
        b = b > 255?255:(b < 0?0:b);
        return -16777216 | r << 16 | g << 8 | b;
    }

    public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = (float)width / (float)w;
        float scaleHeight = (float)height / (float)h;
        matrix.reset();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        return newbmp;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if(initialSize <= 8) {
            for(roundedSize = 1; roundedSize < initialSize; roundedSize <<= 1) {
                ;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = (double)options.outWidth;
        double h = (double)options.outHeight;
        int lowerBound = maxNumOfPixels == -1?1:(int)Math.ceil(Math.sqrt(w * h / (double)maxNumOfPixels));
        int upperBound = minSideLength == -1?128:(int)Math.min(Math.floor(w / (double)minSideLength), Math.floor(h / (double)minSideLength));
        return upperBound < lowerBound?lowerBound:(maxNumOfPixels == -1 && minSideLength == -1?1:(minSideLength == -1?lowerBound:upperBound));
    }

    public static Bitmap cropScanImg(Bitmap imgBitmap){
        int width = imgBitmap.getWidth();
        int height = imgBitmap.getHeight();
        //获取左上裁剪点
        int topX = 0;
        int topY = 0;
        for(int w=0;w<width;w++){
            boolean isBreak = false;
            for(int h=0;h<height;h++){
                int pixel = imgBitmap.getPixel(w,h);
                if(pixel == Color.BLACK){
                    isBreak = true;
                    break;
                }else{
                    topX = w;
                    topY = h;
                }
            }
            if(isBreak){
                break;
            }
        }
        //获取右下裁剪点
        int bottomX = 0;
        int bottomY = 0;
        for(int w=width-1;w>=0;w--){
            boolean isBreak = false;
            for(int h=height-1;h>=0;h--){
                int pixel = imgBitmap.getPixel(w,h);
                if(pixel == Color.BLACK){
                    isBreak = true;
                    break;
                }else{
                    bottomX = w;
                    bottomY = h;
                }
            }
            if(isBreak){
                break;
            }
        }

        if(bottomX <= topX || bottomY <= topY){
            return imgBitmap;
        }

        try{
            imgBitmap = Bitmap.createBitmap(imgBitmap,topX,topY,bottomX-topX,bottomY-topY);
        }catch (Exception e){
            e.printStackTrace();
        }

        return imgBitmap;
    }

    public static void mattingImage(Activity paramActivity, Bitmap paramBitmap,ImageMattingCallback callback){

        Mat localMat1 = new Mat();
        Mat localMat2 = new Mat();
        Mat localMat3 = new Mat();
        ////获取屏幕的分辨率
        //DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        //paramActivity.getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        ////屏幕宽度
        ////int screenWidth = localDisplayMetrics.widthPixels;
        ////不明
        ////int f = screenWidth / 11;
        //讲bitmap转换成Mat数据
        Utils.bitmapToMat(paramBitmap, localMat1);
        //克隆原始数据
        Mat localMat4 = localMat1.clone();
        //颜色空间的转换
        Imgproc.cvtColor(localMat1, localMat2, 7);
        //创建一个空白的对应尺寸的bitmap
        Bitmap a = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //将Mat数据转换为bitmap
        Utils.matToBitmap(localMat2, a);
        //自适应二值化(src:要二值化的灰度图,dst:二值化后的图,maxValue:二值化后要设置的那个值,
        //method:块计算的方法[ADAPTIVE_THRESH_MEAN_C 平均值，ADAPTIVE_THRESH_GAUSSIAN_C 高斯分布加权和]),
        //type:二值化类型（CV_THRESH_BINARY 大于为最大值，CV_THRESH_BINARY_INV 小于为最大值）,
        //blockSize:块大小（奇数，大于1）,delta:差值（负值也可以）)
        //Imgproc.adaptiveThreshold(localMat2, localMat3, 255.0D, 1, 0, 31, 15.0D);
        Imgproc.adaptiveThreshold(localMat2, localMat3, 255.0D, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 15.0D);
        //创建新的对应尺寸的空白bitmap
        Bitmap b = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //将Mat数据转换为bitmap
        Utils.matToBitmap(localMat3, b);
        //定义新的缓存矩阵数据
        Mat localMat5 = new Mat();
        //定义一个合适大小的核
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20.0D, 3.0D));
        //扩大暗区（腐蚀）
        Imgproc.erode(localMat3, localMat5, kernelErode);
        //创建新的对应尺寸的空白bitmap
        Bitmap c = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //Mat转Bitmap
        Utils.matToBitmap(localMat5, c);

        //创建轮廓区域列表缓存
        ArrayList<MatOfPoint> localArrayList = new ArrayList();
        //定义新的缓存矩阵数据
        Mat localMat6 = new Mat();
        //边缘提取(image参数为已经二值化的原图;contours参数为检测的轮廓数组，每一个轮廓用一个MatOfPoint类型的List表示；
        // hiararchy参数和轮廓个数相同mode表示轮廓的检索模式[CV_RETR_EXTERNAL表示只检测外轮廓,CV_RETR_LIST检测的轮廓不建立等级关系,
        // CV_RETR_CCOMP建立两个等级的轮廓，上面的一层为外边界，里面的一层为内孔的边界信息。如果内孔内还有一个连通物体，这个物体的边界也在顶层。
        // CV_RETR_TREE建立一个等级树结构的轮廓。])
        Imgproc.findContours(localMat5.clone(), localArrayList, localMat6, 3, 1);
        //绘制轮廓
        Imgproc.drawContours(localMat3, localArrayList, -1, new Scalar(0.0D, 0.0D, 255.0D));

        //绘制最终图像
        Bitmap d = Bitmap.createBitmap(paramBitmap.getWidth(), paramBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(localMat3, d);
        //调用回调函数
        if (callback != null){
            callback.onImageProcessing(localArrayList, localMat4, d);
        }
    }

    public interface ImageMattingCallback{
        void onImageProcessing(ArrayList<MatOfPoint> localArrayList,Mat localMat,Bitmap rstMap);
    }
}
