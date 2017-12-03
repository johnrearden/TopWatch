package com.intricatech.topwatch;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

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

    private static final String LAP_BUTTON_RUNNING_TEXT = "LAP";
    private static final String LAP_BUTTON_PAUSED_TEXT = "SAVE";

    private String routeNameChosenByUser;

    private LocationManager locationManager;
    private Vibrations vibrations;

    private StopWatch stopWatch;
    private DatabaseFacade databaseFacade;
    private Timer timer;

    private Toolbar toolbar;
    private TextView mainTimerTV;
    private TextView timeSinceLastSplitTV;
    private FrameLayout routeChooserLayout;
    private LinearLayout splitLayout;
    private GridLayout GPSGridLayout;
    private ImageButton playButton;
    private Button lapButton, resetButton;
    private TextView totalDistanceTV, accuracyTV;

    private boolean locationPermissionGranted;
    private List<OnDestroyObserver> observers;

    private LocationRecordServer locationService = null;
    private boolean locationServiceBound;
    private LocationRecordClient locationRecordClient = new LocationRecordClient() {
        @Override
        public void setLocationRecord(LocationRecord locationRecord) {
            Log.d(TAG, locationRecord.toString());
        }

        @Override
        public void setTotalDistance(double totalDistanceTravelled) {
            stopWatch.setTotalDistance(totalDistanceTravelled);
            totalDistanceTV.setText(
                    String.format("%.2f", stopWatch.getTotalDistance())
            );
        }

        @Override
        public void setSplitDistance(double splitDistance) {

        }

        @Override
        public void setAccuracy(double accuracy) {
            accuracyTV.setText(
                    String.format("%.2f", accuracy)
            );
        }

        @Override
        public void updateMapWithPolyline(PolylineOptions options) {
            // NOT USED BY MAIN ACTIVITY.
        }

        @Override
        public void updateMapWithLocationOnly(LatLng latLng) {
            // NOT USED BY MAIN ACTIVITY.
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

        Vibrations.initialize(getApplicationContext());
        vibrations = Vibrations.getInstance();

        TAG = getClass().getSimpleName();
        Log.d(TAG, "onCreate() invoked");
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        observers = new ArrayList<>();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        stopWatch = new StopWatch(this);

        mainTimerTV = (TextView) findViewById(R.id.main_time_readout);
        mainTimerTV.setText(StopWatch.getTimeAsSpannableString(0));
        timeSinceLastSplitTV = (TextView) findViewById(R.id.time_since_last_split);
        timeSinceLastSplitTV.setText(StopWatch.getTimeAsSpannableString(0));
        totalDistanceTV = (TextView) findViewById(R.id.total_dist_travelled);
        accuracyTV = (TextView) findViewById(R.id.accuracy);
        splitLayout = (LinearLayout) findViewById(R.id.split_times_linearlayout);
        routeChooserLayout = (FrameLayout) findViewById(R.id.route_chooser_layout);
        playButton = (ImageButton) findViewById(R.id.play_pause_button);
        resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stopWatch.onResetButtonPressed();
                splitLayout.removeAllViews();
                playButton.setImageResource(R.drawable.play_icon_stopwatch);
                locationService.resetSession();
                totalDistanceTV.setText("0.00");
                return true;
            }
        });
        lapButton = (Button) findViewById(R.id.lap_button);

        databaseFacade = DatabaseFacade.getInstance(getApplicationContext());
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
                        timeSinceLastSplitTV.setText(stopWatch.getTimeSinceLastSplitAsString());
                    }
                });
            }
        }, 4, 4);

        Intent bindIntent = new Intent(this, LocationTrackerService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        locationServiceBound = true;

        if (stopWatch.isRunning()) {
            playButton.setImageResource(R.drawable.pause_icon_stopwatch);
            setResetButtonEnabled(false);
            setLapButtonText(true);
        } else {
            playButton.setImageResource(R.drawable.play_icon_stopwatch);
            setResetButtonEnabled(true);
            setLapButtonText(false);
        }
    }



    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() invoked");
        timer.cancel();

        if (locationServiceBound) {
            unbindService(serviceConnection);
        }
        if (stopWatch.isNewSessionReadyToStart()) {
            Log.d(TAG, "******Session is inactive ... stopping service");
            stopLocationService();
        }
        locationServiceBound = false;
    }

    private void stopLocationService() {
        Intent intent = new Intent(this, LocationTrackerService.class);
        stopService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() invoked");
        stopWatch.saveSharedPreferencesOnStop();
        splitLayout.removeAllViews();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");
        locationService.unregisterActivity(this);
        //unbindService(serviceConnection);
    }

    /**
     * Method polls the LocationManager to check whether the user has granted access to
     * the device's location information.
     * @return true if permission has been granted, false otherwise.
     */
    private boolean checkForLocationPermission() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
            playButton.setImageResource(R.drawable.pause_icon_stopwatch);
            setResetButtonEnabled(false);
            setLapButtonText(true);
        } else {
            playButton.setImageResource(R.drawable.play_icon_stopwatch);
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
        } else {
            lapButton.setText(LAP_BUTTON_PAUSED_TEXT);
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
        stopWatch.saveNewRouteToDB(routeName);
    }

    public void onRouteNameTVPressed(View view) {
        Log.d(TAG, "onRouteNameTVPressed() invoked");
    }

    /**
     * Method takes a spannableString representation of a split-time, inflates a new TextView from
     * the xml layout, sets the text of the TextView and adds it to the split layout.
     * @param spannableString The pre-formatted string representation of the new split time.
     */
    public void onNewSplitCreated(SpannableString spannableString) {
        TextView newSplitTV = (TextView) getLayoutInflater().inflate(R.layout.split_time_element, null, false);
        newSplitTV.setText(spannableString);
        splitLayout.addView(newSplitTV, 0);
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
            case R.id.action_show_map:
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
                break;
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
    public void updateObservers() {
        for (OnDestroyObserver ob : observers) {
            ob.onActivityDestroyed();
        }
    }

    @Override
    public void onRouteChosen(String routeName) {
        getSupportActionBar().setTitle(routeName);
        getSupportFragmentManager().popBackStack();
        routeChooserLayout.setVisibility(View.GONE);
    }

    public void onStopServiceButtonPressed(View view) {
        stopLocationService();
    }

    private void startLocationServiceIfNotStarted() {
        Intent startIntent = new Intent(this, LocationTrackerService.class);
        startService(startIntent);
    }

}
