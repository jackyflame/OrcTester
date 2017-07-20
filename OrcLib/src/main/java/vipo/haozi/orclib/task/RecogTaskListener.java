package vipo.haozi.orclib.task;

import android.graphics.Bitmap;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 14:46
 */

public interface RecogTaskListener {

    void recogSuccess(String strRst, Bitmap localBitmap);

    void recogError(Exception e);
}
