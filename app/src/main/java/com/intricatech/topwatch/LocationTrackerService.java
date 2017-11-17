package com.intricatech.topwatch;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class LocationTrackerService extends IntentService {

    private static final String TAG = "LocationTrackerService";
    public static final String ACTION_LOG = "com.intricatech.topwatch.action.LOG";
    public static final int ONGOING_NOTIFICATION_ID = 101;


    private static Timer timer;

    public LocationTrackerService() {
        super("LocationTrackerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() invoked");



        timer = new Timer();
        timer.scheduleAtFixedRate(new LogTask(), 0, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");
        timer.cancel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() invoked");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle("LocationTrackerService")
                        .setContentText("Notification message")
                        .setSmallIcon(R.drawable.pause_icon_stopwatch)
                        .setContentIntent(pendingIntent)
                        .setTicker("Ticker")
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOG.equals(action)) {
                handleActionLOG();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionLOG() {
        Log.d(TAG, "Logging a tick ...");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private class LogTask extends TimerTask {
        @Override
        public void run() {
            handleActionLOG();
        }
    }
}
