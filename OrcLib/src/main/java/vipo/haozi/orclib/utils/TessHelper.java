package vipo.haozi.orclib.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Android Studio.
 * ProjectName: shenbian_android_cloud_speaker
 * Author: yh
 * Date: 2017/1/19
 * Time: 17:39
 */

public class TessHelper {

    private static final String TAG = TessHelper.class.getName();
    private TessBaseAPI mTessBaseAPI;

    public static TessHelper getInstance() {
        return TessHelper.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        /***
         * 单例对象实例
         */
        static final TessHelper INSTANCE = new TessHelper();
    }

    private TessHelper(){}

    public void init(Context context){
        copyOrcFile(context);
        initTessAPI();
    }

    public void initTessAPI(){
        // 开始调用Tess函数对图像进行识别
        mTessBaseAPI = new TessBaseAPI();
        mTessBaseAPI.setDebug(true);
        // 使用默认语言初始化BaseApi
        mTessBaseAPI.init(TessConstantConfig.getTessDataDirectory(), TessConstantConfig.DEFAULT_LANGUAGE_NUM);
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"0123456789");
        mTessBaseAPI.setVariable("classify_bln_numeric_mode","1");//numeric-only mode.
    }

    public static TessBaseAPI getTessBaseAPI(){
        return getInstance().getTessAPI();
    }

    public TessBaseAPI getTessAPI(){
        if(mTessBaseAPI == null){
            initTessAPI();
        }
        return mTessBaseAPI;
    }

    public void copyOrcFile(Context context) {
        AlertDialog.Builder versionName;
        if(!Environment.getExternalStorageState().equals("mounted")) {
            versionName = new AlertDialog.Builder(context);
            versionName.setTitle("提示");
            versionName.setMessage("请装载SD卡后再运行本程序,否则识别会出现问题");
            versionName.show();
        } else {
            try {
                //String outFileName = getSDPath() + TessConstantConfig.TESSBASE_PATH_NUM;
                String outFileName = TessConstantConfig.getTessDataFilePath();
                //检查路径
                File file = new File(TessConstantConfig.getTessDataFileDirectory());
                if(!file.exists()) {
                    file.mkdirs();
                }
                //检查文件
                File newfile = new File(outFileName);
                if(newfile != null && newfile.exists() && newfile.isFile()) {
                    newfile.delete();
                }
                //重新创建新文件
                newfile.createNewFile();

                //复制文件
                InputStream inputStream = context.getClass().getResourceAsStream("/assets/" + TessConstantConfig.TESSBASE_NUM_FILENAME);
                FileOutputStream myOutput = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];

                int length;
                while((length = inputStream.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"copyOrcFile failed");
            }
        }
    }

    public String parseImageToString(String imagePath) throws IOException, IOException {
        // 检验图片地址是否正确
        if (imagePath == null || imagePath.equals("")){
            return "null file path";
        }

        // 获取Bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

//        // 图片旋转角度
//        int rotate = 0;
//
//        ExifInterface exif = new ExifInterface(imagePath);
//
//        // 先获取当前图像的方向，判断是否需要旋转
//        int imageOrientation = exif
//                .getAttributeInt(ExifInterface.TAG_ORIENTATION,
//                        ExifInterface.ORIENTATION_NORMAL);
//
//        Log.i(TAG, "Current image orientation is " + imageOrientation);
//
//        switch (imageOrientation)
//        {
//            case ExifInterface.ORIENTATION_ROTATE_90:
//                rotate = 90;
//                break;
//            case ExifInterface.ORIENTATION_ROTATE_180:
//                rotate = 180;
//                break;
//            case ExifInterface.ORIENTATION_ROTATE_270:
//                rotate = 270;
//                break;
//            default:
//                break;
//        }
//
//        Log.i(TAG, "Current image need rotate: " + rotate);
//
//        // 获取当前图片的宽和高
//        int w = bitmap.getWidth();
//        int h = bitmap.getHeight();
//
//        // 使用Matrix对图片进行处理
//        Matrix mtx = new Matrix();
//        mtx.preRotate(rotate);
//
//        // 旋转图片
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
//        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 开始调用Tess函数对图像进行识别
        getTessAPI().setImage(bitmap);

        // 获取返回值
        String recognizedText = getTessAPI().getUTF8Text();
        //getTessAPI().end();
        return recognizedText;
    }
}
