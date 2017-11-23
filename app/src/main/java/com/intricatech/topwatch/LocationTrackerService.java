package com.intricatech.topwatch;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


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

    private Map<Activity, LocationRecordClient> clients = new ConcurrentHashMap<>();
    private final Binder localBinder = new LocalBinder();
    private DatabaseFacade databaseFacade;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<LatLng> locations;
    private double totalDistance;
    private double splitDistance;
    private PolylineOptions polylineOptions;

    private static Timer timer;
    private Location currentLocation;
    private boolean isRecording = false;
    private int currentSplitIndex;

    private long elapsedTime;
    private long currentSessionRestartTime;
    private long elapsedSplitTime;
    private long currentSplitRestartTime;

    public LocationTrackerService() {
        super("LocationTrackerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() invoked");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        databaseFacade = DatabaseFacade.getInstance(getApplicationContext());
        locations = new ArrayList<>();
        polylineOptions = new PolylineOptions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");
        if (timer != null) {
            timer.cancel();
        }
    }

    public LocationRecord createNewLocationRecord() {
        commitFusedLocation();
        LocationRecord locRec = new LocationRecord(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                currentLocation.getAltitude(),
                SystemClock.elapsedRealtimeNanos());
        return locRec;
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
                        .setContentText("Location tracking is on.")
                        .setSmallIcon(R.drawable.pause_icon_stopwatch)
                        .setContentIntent(pendingIntent)
                        .setTicker("Ticker")
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        return Service.START_STICKY;
    }

    private void commitFusedLocation() {
        try {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(onSuccessListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    OnSuccessListener<Location> onSuccessListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            if (location != null) {
                currentLocation = location;
                location.getAccuracy();
                long currentTime = SystemClock.elapsedRealtimeNanos();
                long timeStamp = elapsedTime + currentTime - currentSessionRestartTime;
                Log.d(TAG, "currentTime == " + timeStamp + ", location = " + location + ", splitIndex = " + currentSplitIndex);
                databaseFacade.commitLocationRecord(currentTime, location, currentSplitIndex);
                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                locations.add(currentLatLng);
                polylineOptions.add(currentLatLng).width(25.0f).color(Color.WHITE);
                double totalDist = SphericalUtil.computeLength(locations);
                for (Activity client : clients.keySet()) {
                    clients.get(client).setTotalDistance(totalDist);
                    clients.get(client).setAccuracy(currentLocation.getAccuracy());
                    clients.get(client).updateMap(polylineOptions, currentLatLng);
                }
            }
        }
    };


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
        Log.d(TAG, "Committing location to database ...");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    private class LogTask extends TimerTask {
        @Override
        public void run() {
            handleActionLOG();
            commitFusedLocation();
        }
    }

    public class LocalBinder extends Binder
                              implements LocationRecordServer {
        @Override
        public void registerActivity(Activity activity, LocationRecordClient callback) {
            clients.put(activity, callback);
        }

        @Override
        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }

        @Override
        public LocationRecord requestNewLocationRecord() {
            return createNewLocationRecord();
        }

        @Override
        public void startSession() {
            Log.d(TAG, "startSession() invoked");
            long currentTime = SystemClock.elapsedRealtimeNanos();
            currentSessionRestartTime = currentTime;
            elapsedTime = 0;
            currentSplitRestartTime = currentTime;
            elapsedSplitTime = 0;

            isRecording = true;
            databaseFacade.deleteCurrentSessionTable();
            databaseFacade.createNewCurrentSessionTable();
            locations.clear();
            timer = new Timer();
            timer.scheduleAtFixedRate(new LogTask(), 0, 3000);
        }

        @Override
        public void pauseSession() {
            Log.d(TAG, "pauseSession() invoked");

            long currentTime = SystemClock.elapsedRealtimeNanos();
            elapsedTime += currentTime - currentSessionRestartTime;
            elapsedSplitTime += currentTime - currentSplitRestartTime;
            isRecording = false;
            if (timer != null) {
                timer.cancel();
            }
        }

        @Override
        public void restartSession() {
            Log.d(TAG, "restartSession()  invoked");

            long currentTime = SystemClock.elapsedRealtimeNanos();
            currentSessionRestartTime = currentTime;
            currentSplitRestartTime = currentTime;
            isRecording = true;
            timer = new Timer();
            timer.scheduleAtFixedRate(new LogTask(), 0, 5000);
        }

        @Override
        public void newSplitStarted() {
            Log.d(TAG, "newSplitStarted()  invoked");
            currentSplitIndex++;
            commitFusedLocation();
        }

        @Override
        public void finishSessionAndCommit(String routeName) {
            Log.d(TAG, "finishSessionAndCommit() invoked");

            isRecording = false;
            if (timer != null) {
                timer.cancel();
            }
            databaseFacade.copyCurrentSessionToPBSession(routeName);
        }

        @Override
        public void finishSessionAndDelete() {
            Log.d(TAG, "finishSessionAndDelete() invoked");

            isRecording = false;
            if (timer != null) {
                timer.cancel();
            }
            databaseFacade.deleteCurrentSessionTable();
            locations.clear();
        }
    }
}
