package ar.com.craving.ffmpeg;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import ar.com.craving.ffmpeg.MainActivity;

public class WorkerAndroidLogger extends Handler{

    private final WeakReference<MainActivity> loggingToActivity;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public WorkerAndroidLogger(MainActivity loggingToActivity) {

        this.loggingToActivity = new WeakReference<>(loggingToActivity);
    }

    @Override
    public void handleMessage(Message message){
        MainActivity activity = loggingToActivity.get();
        if (activity!= null){
            activity.updateResults(message.getData().getString("result"));
        }
    }

    public void logToUI (String TAG, String stringToLog) {

        MainActivity activity = loggingToActivity.get();
        if (activity!= null) {
            Bundle msgBundle = new Bundle();

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            msgBundle.putString("result", sdf.format(timestamp) + " - " + stringToLog);

            Message msg = new Message();
            msg.setData(msgBundle);

            this.sendMessage(msg);

            Log.d(TAG, stringToLog);
        }
    }
}
