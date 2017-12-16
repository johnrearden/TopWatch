package com.intricatech.topwatch;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class LocationTrackerService extends IntentService
                                    implements GoogleApiClient.ConnectionCallbacks,
                                               GoogleApiClient.OnConnectionFailedListener,
                                                com.google.android.gms.location.LocationListener {

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private static int instanceNumber = 0;
    private static final String TAG = "LocationTrackerService";
    public static final String ACTION_LOG = "com.intricatech.topwatch.action.LOG";
    public static final int ONGOING_NOTIFICATION_ID = 101;
    private static final long MIN_TIME_BETWEEN_UPDATES = 3000;

    private Map<Activity, LocationRecordClient> clients = new ConcurrentHashMap<>();
    private final Binder localBinder = new LocalBinder();
    private DatabaseFacade databaseFacade;

    private List<LatLng> locations;

    private int currentSplitIndex;
    private long elapsedTime;
    private long currentSessionRestartTime;
    private long elapsedSplitTime;
    private long currentSplitRestartTime;
    private int currentHeartRate;
    private double cumulativeDistance;

    public LocationTrackerService() {
        super("LocationTrackerService");
    }

    enum ServiceResponsibilities {
        UPDATE_GUI_ONLY,
        UPDATE_CLIENTS_AND_DATABASE
    }
    private ServiceResponsibilities serviceResponsibilities;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() invoked, instance number " + ++instanceNumber);

        // Get databaseFacade instance and create/clear current session table.
        databaseFacade = DatabaseFacade.getInstance();
        if (databaseFacade.doesTableExist(DBContract.LocationRecords.CURRENT_SESSION_TABLE_NAME)) {
            // todo create or clear table.
        }

        locations = new ArrayList<>();
        serviceResponsibilities = ServiceResponsibilities.UPDATE_GUI_ONLY;

        if (checkPermission(getApplicationContext())) {
            setupLocationService(getApplicationContext());
        } else {
            Log.d(TAG, "location permission not granted");
            // todo handle lack of permission gracefully, stop service.
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");
        stopLocationUpdates();
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        googleApiClient.disconnect();
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

    private void updateGUI(Location location) {
        for (Activity client : clients.keySet()) {
            clients.get(client).setAccuracy(location.getAccuracy());
            clients.get(client).updateMapWithLocationOnly(
                    location,
                    serviceResponsibilities == ServiceResponsibilities.UPDATE_CLIENTS_AND_DATABASE);
        }
        Log.d(TAG, "locs.size() = " + locations.size()
                + ", acc = " + location.getAccuracy());
    }

    private void updateDistanceAndPolyline(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        locations.add(currentLatLng);
        int size = locations.size();
        if (size > 1) {
            cumulativeDistance += SphericalUtil.computeDistanceBetween(
                    locations.get(size - 1), locations.get(size - 2)
            );
        }
        double totalDist = SphericalUtil.computeLength(locations);
        Log.d(TAG, "total distance = " + totalDist + ", cumulative distance = " + cumulativeDistance);
        for (Activity client : clients.keySet()) {
            clients.get(client).setTotalDistance(totalDist);
        }
    }

    private void updateDatabaseAndLocalCache(Location location) {
        long currentTime = SystemClock.elapsedRealtimeNanos();
        long timeStamp = elapsedTime + currentTime - currentSessionRestartTime;
        Log.d(TAG, "currentTime == " + timeStamp + ", location = " + location + ", splitIndex = " + currentSplitIndex);

        databaseFacade.commitLocationRecord(
                timeStamp,
                location,
                currentSplitIndex,
                cumulativeDistance,
                currentHeartRate);
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
        Log.d(TAG, "Committing location to database ...");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
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
        public void startSession() {
            Log.d(TAG, "startSession() invoked");
            serviceResponsibilities = ServiceResponsibilities.UPDATE_CLIENTS_AND_DATABASE;

            long currentTime = SystemClock.elapsedRealtimeNanos();
            currentSessionRestartTime = currentTime;
            elapsedTime = 0;
            currentSplitRestartTime = currentTime;
            elapsedSplitTime = 0;
            cumulativeDistance = 0;
            databaseFacade.createNewCurrentSessionTable();
        }

        @Override
        public void pauseSession() {
            Log.d(TAG, "pauseSession() invoked");
            serviceResponsibilities = ServiceResponsibilities.UPDATE_GUI_ONLY;

            long currentTime = SystemClock.elapsedRealtimeNanos();
            elapsedTime += currentTime - currentSessionRestartTime;
            elapsedSplitTime += currentTime - currentSplitRestartTime;
        }

        @Override
        public void restartSession() {
            Log.d(TAG, "restartSession()  invoked");
            serviceResponsibilities = ServiceResponsibilities.UPDATE_CLIENTS_AND_DATABASE;

            long currentTime = SystemClock.elapsedRealtimeNanos();
            currentSessionRestartTime = currentTime;
            currentSplitRestartTime = currentTime;
        }

        @Override
        public void newSplitStarted() {
            Log.d(TAG, "newSplitStarted()  invoked");
            currentSplitIndex++;
        }

        @Override
        public void resetSession() {
            Log.d(TAG, "resetSession() invoked()");
            databaseFacade.clearCurrentSessionTable();
            databaseFacade.deleteCurrentSessionTable();
            locations.clear();
            cumulativeDistance = 0;
        }
    }

    private boolean checkPermission(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    private void setupLocationService(Context context) {
        if (checkPlayServices()) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            createLocationRequest();
        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest().create();
        locationRequest.setInterval(MIN_TIME_BETWEEN_UPDATES);
        locationRequest.setFastestInterval(MIN_TIME_BETWEEN_UPDATES / 2);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        googleApiClient.connect();
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            Log.d(TAG, "can't connect to google play services");
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (googleApiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "no location permission granted");
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
            Log.d(TAG, "requestLocationUpdates() invoked");
        } else {
            Log.d(TAG, "googleApiClient is not connected");
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient == null) {
            return;
        } else if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(googleApiClient, this);
        } else {
            Log.d(TAG, "googleApiClient not connected - cant disconnect");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d(TAG, "location update received by instance number " + instanceNumber);
            switch (serviceResponsibilities) {
                case UPDATE_GUI_ONLY:
                    updateGUI(location);
                    break;
                case UPDATE_CLIENTS_AND_DATABASE:
                    updateGUI(location);
                    updateDistanceAndPolyline(location);
                    updateDatabaseAndLocalCache(location);
                    break;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
