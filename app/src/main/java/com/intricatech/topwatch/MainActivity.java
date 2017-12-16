package com.intricatech.topwatch;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements  NewSplitListener,
                    OnDestroyDirector,
                    OnRouteChosenListener{

    private String TAG;  // The tag
    private static final int FINE_LOCATION_REQUEST_CODE = 111;
    private static int instanceNumber = 0;

    private static final String LAP_BUTTON_RUNNING_TEXT = "LAP";
    private static final String LAP_BUTTON_PAUSED_TEXT = "SAVE";

    private String routeNameChosenByUser;

    private Vibrations vibrations;
    private StopWatch stopWatch;
    private DatabaseFacade databaseFacade;
    private Timer timer;
    private RouteTrackerFragment routeTrackerFragment;
    private CustomMapFragment customMapFragment;
    private SplitDisplayFragment splitDisplayFragment;
    //todo Put the fragments back into the enum, check if it works. Log message when instantiating for timing check.

    private Toolbar toolbar;
    private TextView mainTimerTV;
    private FrameLayout routeChooserLayout;
    private ImageButton playButton;
    private Button lapButton, resetButton;
    private TextView totalDistanceTV, accuracyTV;
    private ImageView gpsQualityIV;
    private GPSQuality gpsQuality;

    private boolean locationPermissionGranted;
    private List<OnDestroyObserver> observers;

    private ViewPager viewPager;
    private SwipeViewPagerAdapter swipeViewPagerAdapter;

    private LocationRecordServer locationService = null;
    private boolean locationServiceBound;
    private LocationRecordClient locationRecordClient = new LocationRecordClient() {

        @Override
        public void setTotalDistance(double totalDistanceTravelled) {
            stopWatch.setTotalDistance(totalDistanceTravelled);
            totalDistanceTV.setText(
                    String.format("%.2f", stopWatch.getTotalDistance())
            );
            routeTrackerFragment.onTotalDistanceCoveredUpdated(totalDistanceTravelled);
        }

        @Override
        public void setAccuracy(double accuracy) {
            accuracyTV.setText(
                    String.format("%.2f", accuracy)
            );
            gpsQuality = gpsQuality.setQuality(accuracy);
            gpsQualityIV.setImageResource(gpsQuality.getResourceID());
        }

        @Override
        public void updateMapWithLocationOnly(Location location, boolean addToPolyline) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            customMapFragment.updateMapWithLocationOnly(latLng, addToPolyline);
            routeTrackerFragment.onLocationUpdatedWhileStopped(location);
        }
    };
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            locationService = (LocationRecordServer) binder;
            try {
                locationService.registerActivity(MainActivity.this, locationRecordClient);
            } catch (Throwable t) {
                Log.d(TAG, "Woah man, WTF?");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        TAG = getClass().getSimpleName();
        observers = new ArrayList<>();
        DatabaseFacade.initialize(getApplicationContext());
        databaseFacade = DatabaseFacade.getInstance();
        Vibrations.initialize(getApplicationContext());
        vibrations = Vibrations.getInstance();
        boolean b = databaseFacade.doesTableExist(DBContract.LocationRecords.CURRENT_SESSION_TABLE_NAME);
        Log.d(TAG, "current session table exists : " + String.valueOf(b));


        Log.d(TAG, "onCreate() invoked, instance number " + ++instanceNumber);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        stopWatch = new StopWatch(this);

        swipeViewPagerAdapter = new SwipeViewPagerAdapter(getSupportFragmentManager());
        customMapFragment = swipeViewPagerAdapter.getCustomMapFragment();
        splitDisplayFragment = swipeViewPagerAdapter.getSplitDisplayFragment();
        routeTrackerFragment = swipeViewPagerAdapter.getRouteTrackerFragment();

        mainTimerTV = (TextView) findViewById(R.id.main_time_readout);
        mainTimerTV.setText(StopWatch.getTimeAsSpannableString(0));
        totalDistanceTV = (TextView) findViewById(R.id.total_dist_travelled);
        accuracyTV = (TextView) findViewById(R.id.accuracy);
        routeChooserLayout = (FrameLayout) findViewById(R.id.route_chooser_layout);
        playButton = (ImageButton) findViewById(R.id.play_pause_button);
        gpsQualityIV = (ImageView) findViewById(R.id.gps_quality_readout);
        resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stopWatch.onResetButtonPressed();
                customMapFragment.onResetSession();
                playButton.setImageResource(R.drawable.play_button);
                splitDisplayFragment.removeViewsFromSplitLayout();
                locationService.resetSession();
                totalDistanceTV.setText("0.00");
                return true;
            }
        });
        lapButton = (Button) findViewById(R.id.lap_button);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(swipeViewPagerAdapter);
        viewPager.setCurrentItem(1);

        gpsQuality = GPSQuality.OFF;
    }



    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() invoked");

        stopWatch.loadSharedPreferencesOnStart();
        locationPermissionGranted = checkForLocationPermission();
        if (locationPermissionGranted) {
            startLocationServiceIfNotStarted();
        } else {
            requestPermissionIfAppropriate();
        }
        Intent bindIntent = new Intent(this, LocationTrackerService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        locationServiceBound = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() invoked");
        setTitle("TopWatch");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainTimerTV.setText(stopWatch.getTotalTimeAsString());
                        SpannableString ss = stopWatch.getTimeSinceLastSplitAsString();
                        splitDisplayFragment.updateTimeSinceLastSplitTV(ss);
                        routeTrackerFragment.updateCurrentSplitTime(ss);
                    }
                });
            }
        }, 4, 4);

        stopWatch.loadSharedPreferencesOnStart();
        if (stopWatch.isRunning()) {
            playButton.setImageResource(R.drawable.stop_button);
            setResetButtonEnabled(false);
            setLapButtonText(true);
        } else {
            playButton.setImageResource(R.drawable.play_button);
            setResetButtonEnabled(true);
            setLapButtonText(false);
        }

    }



    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() invoked");
        timer.cancel();
        timer = null;

        if (stopWatch.isNewSessionReadyToStart()) {
            Log.d(TAG, "******Session is inactive ... stopping service");
            stopLocationService();
        }

    }

    private void stopLocationService() {
        Intent intent = new Intent(this, LocationTrackerService.class);
        stopService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() invoked");
        locationService.unregisterActivity(this);
        if (locationServiceBound) {
            unbindService(serviceConnection);
            locationServiceBound = false;
        }
        stopWatch.saveSharedPreferencesOnStop();
        splitDisplayFragment.removeViewsFromSplitLayout();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");

        updateDestructionObservers();
        //databaseFacade.onMainActivityDestroyed();
        stopWatch.onMainActivityDestroyed();
        customMapFragment = null;
        splitDisplayFragment = null;
        routeTrackerFragment = null;
        swipeViewPagerAdapter.onMainActivityDestroyed();
        viewPager = null;
    }

    /**
     * Method polls the LocationManager to check whether the user has granted access to
     * the device's location information.
     * @return true if permission has been granted, false otherwise.
     */
    private boolean checkForLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method checks whether the user needs an explanation of why location permission should
     * be granted, and displays this explanation if necessary. It then requests the permission.
     */
    private void requestPermissionIfAppropriate() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Location permission is necessary for this app to work properly", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_REQUEST_CODE);
        }
    }

    /**
     * Method receives a callback upon granting or denial of permission by the user, and sets the flag
     * locationPermissionGranted appropriately. If permission IS granted, it requests location updates
     * from the device's GPS system and provides a LocationListener object for callbacks.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch(requestCode) {
            case FINE_LOCATION_REQUEST_CODE : {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                    startLocationServiceIfNotStarted();
                } else {
                    locationPermissionGranted = false;
                }
                Log.d(TAG, "permission granted : " + locationPermissionGranted);
            }
        }
    }

    /**
     * Method passes the button press onto the model StopWatch object and alters the button icon
     * and the enabled field to match the new state.
     * @param view
     */
    public void onPlayButtonPressed(View view) {

        // Inform the LocationService of the change in the state of the StopWatch.
        if (stopWatch.isRunning()) {
            locationService.pauseSession();
        } else {
            if (stopWatch.isNewSessionReadyToStart()) {
                locationService.startSession();
            } else {
                locationService.restartSession();
            }
        }

        // Instruct the StopWatch to toggle state.
        stopWatch.onPlayButtonPressed();
        vibrations.doLongVibrate();

        // Update the buttons to reflect the new StopWatch state.
        updateControlGUI(stopWatch.isRunning());
    }

    private void updateControlGUI(boolean stopWatchIsRunning) {
        if (stopWatchIsRunning) {
            playButton.setImageResource(R.drawable.stop_button);
            setResetButtonEnabled(false);
            setLapButtonText(true);
        } else {
            playButton.setImageResource(R.drawable.play_button);
            setResetButtonEnabled(true);
            setLapButtonText(false);
        }
    }

    public void onShowRouteButtonPressed(View view) {
        Log.d(TAG, "Show Route button pressed");

        RouteChooserFragment routeChooserFragment = new RouteChooserFragment();

        routeChooserLayout.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .add(R.id.route_chooser_layout, routeChooserFragment)
                .commit();
    }

    /**
     * Method combines setting the enabled property of the reset Button and its textColor.
     * @param enabled The desired status of the Reset Button.
     */
    private void setResetButtonEnabled(boolean enabled) {
        if (enabled) {
            resetButton.setEnabled(true);
            resetButton.setTextColor(Color.WHITE);
        } else {
            resetButton.setEnabled(false);
            resetButton.setTextColor(Color.GRAY);
        }
    }

    /**
     * Sets the text of the lap button. If the stopWatch is running, the lap button should show "LAP".
     * If not, it shows "SAVE", as this button serves the second function of storing the completed
     * route data.
     * @param isRunning stopWatch.isRunning should be supplied as the parameter.
     */
    private void setLapButtonText(boolean isRunning) {
        if (isRunning) {
            lapButton.setText(LAP_BUTTON_RUNNING_TEXT);
            //lapButton.setBackgroundResource(R.drawable.round_blue_button);
        } else {
            lapButton.setText(LAP_BUTTON_PAUSED_TEXT);
            //lapButton.setBackgroundResource(R.drawable.round_purple_button);
        }
    }

    /**
     * Method passes the button press on to the model Stopwatch object.
     * @param view
     */
    public void onLapButtonPressed(View view) {

        if (stopWatch.isRunning()) {
            stopWatch.onLapButtonPressed();
            vibrations.doShortVibrate();
            locationService.newSplitStarted();
        } else {
            switch (stopWatch.getRecordingType()) {
                case NEW_RECORDING:
                    promptUserForRouteName(); // calls back to onRouteNameEntered.
                    break;
                case EXISTING_ROUTE:
                    stopWatch.saveNewSessionToDB();
                    break;
            }
        }
    }

    private void onRouteNameEntered(String routeName) {
        long rowID = stopWatch.saveNewRouteToDB(routeName);
        long currentSessionTime = stopWatch.getTotalTimeInNanos();
        Log.d(TAG, "onRouteNameEntered() : currentSessionTime ==  " + currentSessionTime);
        long PBSessionTime = databaseFacade.getPBTimeForRoute(rowID);
        Log.d(TAG, "onRouteNameEntered() : PBSessionTime ==  " + PBSessionTime);

        if (currentSessionTime < PBSessionTime) {
            databaseFacade.copyCurrentSessionToPBSession(rowID);
            Toast.makeText(this, "New PB set!", Toast.LENGTH_LONG).show();
        }
    }

    public void onRouteNameTVPressed(View view) {
        Log.d(TAG, "onRouteNameTVPressed() invoked");
    }

    /**
     * Method takes a spannableString representation of a split-time, inflates a new TextView from
     * the xml layout, sets the text of the TextView and adds it to the split layout.
     * @param timeSS The pre-formatted string representation of the new split time.
     * @param distanceSS The pre-formatted string representation of the new split distance.
     */
    public void onNewSplitCreated(SpannableString timeSS, SpannableString distanceSS) {
        swipeViewPagerAdapter.getSplitDisplayFragment().onNewSplitCreated(timeSS, distanceSS);
    }

    public void getSavedSplitsIfAny() {
        stopWatch.rePostSavedSplits();
    }

    private String promptUserForRouteName() {

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.route_name_prompt, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(promptView);
        final EditText userInput = (EditText) promptView.findViewById(R.id.routename_edittext);

        dialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                routeNameChosenByUser = userInput.getText().toString();
                                getSupportActionBar().setTitle(routeNameChosenByUser);
                                onRouteNameEntered(routeNameChosenByUser);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }
                );

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
        return userInput.getText().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_choose_route:
                onShowRouteButtonPressed(null);
                break;
            case R.id.action_settings:
                return true;
            case R.id.action_design_new_route:

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void register(OnDestroyObserver observer) {
        observers.add(observer);
    }

    @Override
    public void deregister(OnDestroyObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void deregisterAll() {
        observers.clear();
    }

    @Override
    public void updateDestructionObservers() {
        for (OnDestroyObserver ob : observers) {
            ob.onActivityDestroyed();
        }
    }

    @Override
    public void onRouteChosen(long rowID) {
        String titleString = databaseFacade.getRouteNameFromID(rowID);
        getSupportActionBar().setTitle(titleString);
        getSupportFragmentManager().popBackStack();
        routeChooserLayout.setVisibility(View.GONE);
        routeTrackerFragment.onRouteLoaded(rowID);
    }

    private void startLocationServiceIfNotStarted() {
        Intent startIntent = new Intent(getApplicationContext(), LocationTrackerService.class);
        startService(startIntent);
    }



}
