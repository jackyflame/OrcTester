package vipo.haozi.orclib.task;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/20
 * Time: 11:57
 */

public interface RecognizeTaskItf extends Runnable{

    void continueScan();

    void stopScan();

    void setRecogTaskListener(RecogTaskListener recogTaskListener);
}
