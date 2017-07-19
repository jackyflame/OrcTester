package vipo.haozi.orclib.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Created by Android Studio.
 * ProjectName: shenbian_android_cloud_speaker
 * Author: yh
 * Date: 2016/12/1
 * Time: 15:34
 */

public class CameraUtils{

    private static final String TAG = CameraUtils.class.getName();

    public static final String CPU_ARCHITECTURE_TYPE_32 = "32";
    public static final String CPU_ARCHITECTURE_TYPE_64 = "64";
    private static final String PROC_CPU_INFO_PATH = "/proc/cpuinfo";
    private static boolean LOGENABLE = false;
    public static final boolean mIsKitKat;

    static {
        mIsKitKat = Build.VERSION.SDK_INT >= 19;
    }

    public static String pictureName() {
        String str = "";
        Time t = new Time();
        t.setToNow();
        int year = t.year;
        int month = t.month + 1;
        int date = t.monthDay;
        int hour = t.hour;
        int minute = t.minute;
        int second = t.second;
        if(month < 10) {
            str = String.valueOf(year) + "0" + month;
        } else {
            str = String.valueOf(year) + String.valueOf(month);
        }

        if(date < 10) {
            str = str + "0" + date;
        } else {
            str = str + String.valueOf(date);
        }

        if(hour < 10) {
            str = str + "0" + hour;
        } else {
            str = str + String.valueOf(hour);
        }

        if(minute < 10) {
            str = str + "0" + minute;
        } else {
            str = str + String.valueOf(minute);
        }

        if(second < 10) {
            str = str + "0" + second;
        } else {
            str = str + String.valueOf(second);
        }

        return str;
    }

    public static ArrayList<Camera.Size> splitSize(String str, Camera camera) {
        if(str == null) {
            return null;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList sizeList = new ArrayList();

            while(tokenizer.hasMoreElements()) {
                Camera.Size size = strToSize(tokenizer.nextToken(), camera);
                if(size != null) {
                    sizeList.add(size);
                }
            }

            if(sizeList.size() == 0) {
                return null;
            } else {
                return sizeList;
            }
        }
    }

    public static Camera.Size strToSize(String str, Camera camera) {
        if(str == null) {
            return null;
        } else {
            int pos = str.indexOf(120);
            if(pos != -1) {
                String width = str.substring(0, pos);
                String height = str.substring(pos + 1);
                return camera.new Size(Integer.parseInt(width), Integer.parseInt(height));
            } else {
                return null;
            }
        }
    }

    public static int[] getBitmapIntArray(Bitmap bitmap) {
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();
        int[] mIntArray = new int[mWidth * mHeight];
        bitmap.getPixels(mIntArray, 0, mWidth, 0, 0, mWidth, mHeight);
        return mIntArray;
    }

    public static void freeFileLock(FileLock fl, File file) {
        if(file != null) {
            file.delete();
        }

        if(fl != null && fl.isValid()) {
            try {
                fl.release();
            } catch (IOException var3) {
                ;
            }

        }
    }

    @TargetApi(19)
    public static String getPath(Context context, Uri uri) {
        boolean isKitKat = Build.VERSION.SDK_INT >= 19;
        if(isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            String docId;
            String[] split;
            String type;
            if(isExternalStorageDocument(uri)) {
                docId = DocumentsContract.getDocumentId(uri);
                split = docId.split(":");
                type = split[0];
                if("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else {
                if(isDownloadsDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    Uri split1 = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue());
                    return getDataColumn(context, split1, (String)null, (String[])null);
                }

                if(isMediaDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    split = docId.split(":");
                    type = split[0];
                    Uri contentUri = null;
                    if("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if("video".equals(type)) {
                        contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if("audio".equals(type)) {
                        contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, "_id=?", selectionArgs);
                }
            }
        } else {
            if("content".equalsIgnoreCase(uri.getScheme())) {
                if(isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }

                return getDataColumn(context, uri, (String)null, (String[])null);
            }

            if("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{"_data"};

        String var9;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, (String)null);
            if(cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int index = cursor.getColumnIndexOrThrow("_data");
            var9 = cursor.getString(index);
        } finally {
            if(cursor != null) {
                cursor.close();
            }

        }

        return var9;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getPathBefore(Context context, Uri uri) {
        if("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = new String[]{"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, (String)null, (String[])null, (String)null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if(cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception var5) {
                ;
            }
        } else if("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static byte[] Stream2Byte(String infile) {
        BufferedInputStream in = null;
        ByteArrayOutputStream out = null;

        byte[] content;
        try {
            in = new BufferedInputStream(new FileInputStream(infile));
            out = new ByteArrayOutputStream(1024);
            content = new byte[1024];
            boolean size = false;

            int size1;
            while((size1 = in.read(content)) != -1) {
                out.write(content, 0, size1);
            }
        } catch (Exception var13) {
            var13.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException var12) {
                var12.printStackTrace();
            }

        }

        content = out.toByteArray();
        return content;
    }

    public void savePic(String path, byte[] list_bytes) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeByteArray(list_bytes, 0, list_bytes.length, opts);

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
            bos.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        if(bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }

    }

    public static boolean isCPUInfo64() {
        File cpuInfo = new File("/proc/cpuinfo");
        if(cpuInfo != null && cpuInfo.exists()) {
            FileInputStream inputStream = null;
            BufferedReader bufferedReader = null;

            try {
                inputStream = new FileInputStream(cpuInfo);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 512);
                String line = bufferedReader.readLine();
                if(line == null || line.length() <= 0 || !line.toLowerCase(Locale.US).contains("arch64")) {
                    return false;
                }
            } catch (Throwable var18) {
                return false;
            } finally {
                try {
                    if(bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (Exception var17) {
                    var17.printStackTrace();
                }

                try {
                    if(inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception var16) {
                    var16.printStackTrace();
                }

            }

            return true;
        } else {
            return false;
        }
    }

    public static int clamp(int x, int min, int max) {
        return x > max?max:(x < min?min:x);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth, int viewHeight) {
        matrix.setScale((float)(mirror?-1:1), 1.0F);
        matrix.postRotate((float)displayOrientation);
        matrix.postScale((float)viewWidth / 2000.0F, (float)viewHeight / 2000.0F);
        matrix.postTranslate((float)viewWidth / 2.0F, (float)viewHeight / 2.0F);
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static void copyFile(Context context) {
        String smartVisitionPath = getSDPath() + "/Yunlaba/orc/";
        String rootpath = getSDPath() + "/AndroidWT";
        AlertDialog.Builder versionName;
        if(!Environment.getExternalStorageState().equals("mounted")) {
            versionName = new AlertDialog.Builder(context);
            versionName.setTitle("提示");
            versionName.setMessage("请装载SD卡后再运行本程序,否则识别会出现问题");
            versionName.show();
        } else {
            try {
                versionName = null;
                InputStream iStream = context.getClass().getResourceAsStream("/assets/num.traineddata");
                int size_is = iStream.available();
                byte[] byte_new = new byte[size_is];
                iStream.read(byte_new);
                iStream.close();
                String versionName1 = new String(byte_new);
                String versiontxt = "";
                String paths = getSDPath();
                if(paths != null && !paths.equals("")) {
                    String versionpath = smartVisitionPath + "version.txt";
                    File versionfile = new File(versionpath);
                    if(versionfile.exists()) {
                        FileReader dir = new FileReader(versionpath);
                        BufferedReader pntWTPENPDA = new BufferedReader(dir);

                        for(String r = pntWTPENPDA.readLine(); r != null; r = pntWTPENPDA.readLine()) {
                            versiontxt = versiontxt + r;
                        }

                        pntWTPENPDA.close();
                        dir.close();
                    }

                    if(!versionName1.equals(versiontxt)) {
                        File dir1 = new File(rootpath);
                        if(!dir1.exists()) {
                            dir1.mkdirs();
                        }

                        copyDataBase(context);
                        String[] pntWTPENPDA1 = new String[]{"pntWTPENPDA1.lib", "pntWTPENPDA2.lib"};
                        mergeFile(pntWTPENPDA1, "pntWTPENPDA.lib", context);
                    }
                }
            } catch (Exception var14) {
                ;
            }
        }

    }

    public static void copyDataBase(Context context) {
        String dst = getSDPath() + "/AndroidWT/smartVisition/";
        String[] filename = new String[]{"SZHY.xml", "appTemplateConfig.xml", "version.txt", "WTPENPDA.lib"};

        for(int i = 0; i < filename.length; ++i) {
            String outFileName = dst + filename[i];
            File file = new File(dst);
            if(!file.exists()) {
                file.mkdirs();
            }

            file = new File(outFileName);
            if(file.exists()) {
                file.delete();
            }

            try {
                InputStream e = context.getClass().getResourceAsStream("/assets/SmartVisition/" + filename[i]);
                FileOutputStream myOutput = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];

                int length;
                while((length = e.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
                e.close();
            } catch (Exception var10) {
                System.out.println(filename[i] + "is not find");
            }
        }

    }

    public static void mergeFile(String[] file, String filename, Context context) throws IOException {
        String filepath = getSDPath() + "/AndroidWT/smartVisition/" + filename;
        File newfile = new File(filepath);
        if(newfile != null && newfile.exists() && newfile.isFile()) {
            newfile.delete();
        }

        FileOutputStream out = new FileOutputStream(filepath);
        byte[] buffer = new byte[1024];
        boolean readLen = false;

        for(int i = 0; i < file.length; ++i) {
            InputStream in = context.getClass().getResourceAsStream("/assets/SmartVisition/" + file[i]);

            int var10;
            while((var10 = in.read(buffer)) != -1) {
                out.write(buffer, 0, var10);
            }

            out.flush();
            in.close();
        }

        out.close();
    }

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
        if(sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        } else {
            sdDir = Environment.getRootDirectory();
        }

        return sdDir.toString();
    }

    public static Bitmap getBitmapFromPreview(byte[] data,Camera camera){
        return getBitmapFromPreview(data,camera,null);
    }

    public static Bitmap getBitmapFromPreview(byte[] data,Camera camera,Rect scanareaRect){
        //处理data
        Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(data,ImageFormat.NV21,previewSize.height,previewSize.width,null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(scanareaRect == null){
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
        }else{
            yuvimage.compressToJpeg(scanareaRect, 100, baos);// 80--JPG图片的质量[0-100],100最高
        }
        byte[] rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
    }

    public static String savePreviewPic(byte[] data, Camera camera,Rect scanareaRect){
        //获取尺寸,格式转换的时候要用到
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        //byte[] newData = CameraViewRotateUtils.rotateYUV420Degree90(data,previewSize.width, previewSize.height);
        //YuvImage yuvimage = new YuvImage(newData,ImageFormat.NV21, previewSize.height, previewSize.width,null);

        YuvImage yuvimage = new YuvImage(data,ImageFormat.NV21, previewSize.height, previewSize.width,null);

        String PATH = TessConstantConfig.getTessDataDirectory() + "scanImg/";
        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        String name = CameraUtils.pictureName();
        String picPathString = PATH + "smartVisition" + name + ".jpg";
        try {
            FileOutputStream filecon = new FileOutputStream(picPathString);
            if(scanareaRect != null){
                yuvimage.compressToJpeg(scanareaRect, 100, filecon);// 80--JPG图片的质量[0-100],100最高
            }else{
                yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, filecon);// 80--JPG图片的质量[0-100],100最高
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return picPathString;
    }

    public static void saveBitmap(Bitmap bitmap) {
        Log.e(TAG, "保存图片");
        String PATH = TessConstantConfig.getTessDataDirectory() + "scanImg/";
        String picName = CameraUtils.pictureName()+".png";
        File f = new File(PATH, picName);
        if (f.exists()) {
            f.delete();
        }else{
            f.getParentFile().mkdirs();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Log.i(TAG, "已经保存");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
