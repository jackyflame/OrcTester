package vipo.haozi.orclib.task;

import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Android Studio.
 * ProjectName: OrcTester
 * Author: haozi
 * Date: 2017/7/19
 * Time: 17:46
 */

public class ThreadMangaer {

    private static final String TAG = "RecogThread";
    private boolean pauseTaskLine = false;
    private RecogThread nowTask;

    Executor executorSingle = Executors.newSingleThreadExecutor();
    Executor executorFixed = Executors.newFixedThreadPool(5);

    public static ThreadMangaer getInstance() {
        return ThreadMangaer.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        /***
         * 单例对象实例
         */
        static final ThreadMangaer INSTANCE = new ThreadMangaer();
    }

    public void excuteTaskInSingle(Thread task){
        executorSingle.execute(task);
    }

    public void excuteTaskInSingle(RecogThread task){
        if(task.isAlive()){
            Log.i(TAG, "[ThreadMangaer]------------->>RecogThread is running");
            return;
        }else if(RecogThread.isRecoging()){
            Log.i(TAG, "[ThreadMangaer]------------->>RecogThread is isRecoging");
            return;
        }else if(pauseTaskLine == true){
            Log.i(TAG, "[ThreadMangaer]------------->>taskline is paused");
            return;
        }
        executorSingle.execute(task);
    }

    public void excuteTaskInFixed(Thread task){
        executorFixed.execute(task);
    }

    public void setPauseTaskLine(boolean pauseTaskLine){
        this.pauseTaskLine = pauseTaskLine;
    }
}
