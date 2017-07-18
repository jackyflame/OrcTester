package vipo.haozi.orclib.base;

import android.app.Application;

import org.opencv.android.OpenCVLoader;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/18
 * Time: 18:27
 */

public class BaseApplication extends Application{

    public BaseApplication() {
        OpenCVLoader.initDebug();
    }
}
