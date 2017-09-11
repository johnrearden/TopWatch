package com.intricatech.topwatch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
    NewSplitListener{

    private String TAG;
    private static final int FINE_LOCATION_REQUEST_CODE = 111;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private DecimalFormat locationFormatter = new DecimalFormat("###.###");

    private StopWatch stopWatch;
    private Timer timer;
    private TextView mainTimerTV;
    private TextView timeSinceLastSplitTV;
    private LinearLayout splitLayout;
    private GridLayout GPSGridLayout;
    private ImageButton playButton;
    private Button lapButton, resetButton;
    private TextView latitudeTV, longitudeTV;
    private TextView altitudeTV, speedTV;

    private boolean locationPermissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        TAG = getClass().getSimpleName();
        Log.d(TAG, "onCreate() invoked");
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        longitudeTV = (TextView) findViewById(R.id.longitute_tv);
        latitudeTV = (TextView) findViewById(R.id.latitude_tv);
        altitudeTV = (TextView) findViewById(R.id.altitude_tv);
        speedTV = (TextView) findViewById(R.id.speed_tv);

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

        splitLayout = (LinearLayout) findViewById(R.id.split_times_linearlayout);
        GPSGridLayout = (GridLayout) findViewById(R.id.gps_gridlayout);

        playButton = (ImageButton) findViewById(R.id.play_pause_button);
        resetButton = (Button) findViewById(R.id.stop_button);
        resetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stopWatch.onResetButtonPressed();
                splitLayout.removeAllViews();
                playButton.setImageResource(R.drawable.play_icon_stopwatch);
                return true;
            }
        });
        lapButton = (Button) findViewById(R.id.lap_button);
    }



    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() invoked");
        stopWatch.loadSharedPreferencesOnStart();
        if (stopWatch.isRunning()) {
            setResetButtonEnabled(false);
        }
        locationPermissionGranted = checkForLocationPermission();
        if (!locationPermissionGranted) {
            requestPermissionIfAppropriate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() invoked");
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
        setGPSLayoutVisibility();
    }



    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() invoked");
        timer.cancel();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() invoked");
        stopWatch.saveSharedPreferencesOnStop();
        splitLayout.removeAllViews();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() invoked");
    }

    /**
     * Method polls the LocationManager to check whether the user has granted access to
     * the device's location information.
     * @return true if permission has been granted, false otherwise.
     */
    private boolean checkForLocationPermission() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener);
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
                    setGPSLayoutVisibility();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener);}
                } else {
                    locationPermissionGranted = false;
                    setGPSLayoutVisibility();
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
        stopWatch.onPlayButtonPressed();
        if (!stopWatch.isRunning()) {
            playButton.setImageResource(R.drawable.play_icon_stopwatch);
            setResetButtonEnabled(true);

        } else {
            playButton.setImageResource(R.drawable.pause_icon_stopwatch);
            setResetButtonEnabled(false);        }
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
     * Method passes the button press on to the model Stopwatch object.
     * @param view
     */
    public void onLapButtonPressed(View view) {
        stopWatch.onLapButtonPressed();
    }

    public void onResetButtonPressed(View view) {
        stopWatch.onResetButtonPressed();
        splitLayout.removeAllViews();
        playButton.setImageResource(R.drawable.play_icon_stopwatch);
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

    private class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location loc) {
            latitudeTV.setText("Lat: " + String.format(locationFormatter.format(loc.getLatitude())));
            longitudeTV.setText("Lng: " + String.format(locationFormatter.format(loc.getLongitude())));
            altitudeTV.setText("Alt: " + String.format(locationFormatter.format(loc.getAltitude())));
            speedTV.setText("Vel: " + String.format(locationFormatter.format(loc.getSpeed())));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    /**
     * Method shows the GPS information on the screen if location access has been granted,
     * and hides it otherwise.
     */
    private void setGPSLayoutVisibility() {
        if (locationPermissionGranted) {
            GPSGridLayout.setVisibility(View.VISIBLE);
        } else {
            GPSGridLayout.setVisibility(View.GONE);
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
