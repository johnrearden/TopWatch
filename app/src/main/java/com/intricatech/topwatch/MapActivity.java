package com.intricatech.topwatch;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;


/**
 * Created by Bolgbolg on 13/11/2017.
 */

public class MapActivity extends AppCompatActivity
                         implements OnMapReadyCallback {

    private static String TAG;
    private static final float DEFAULT_ZOOM = 17.0f;

    private GoogleMap map;
    private LocationRecordServer locationTrackerService;
    private boolean locationServiceBound;

    private LocationRecordClient locationRecordClient = new LocationRecordClient() {
        @Override
        public void setLocationRecord(LocationRecord locationRecord) {
            // NOT USED BY MAP_ACTIVITY.
        }

        @Override
        public void setTotalDistance(double totalDistanceTravelled) {
            // NOT USED BY MAP_ACTIVITY.
        }

        @Override
        public void setSplitDistance(double splitDistance) {
            // NOT USED BY MAP_ACTIVITY.
        }

        @Override
        public void setAccuracy(double accuracy) {
            // NOT USED BY MAP_ACTIVITY.
        }

        @Override
        public void updateMapWithPolyline(PolylineOptions options) {
            map.clear();
            map.addPolyline(options);
        }

        @Override
        public void updateMapWithLocationOnly(LatLng latLng) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            locationTrackerService = (LocationRecordServer) binder;
            try {
                locationTrackerService.registerActivity(MapActivity.this, locationRecordClient);
            } catch (Throwable t) {
                Log.d(TAG, "Woah man, WTF?");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Intent bindIntent = new Intent(this, LocationTrackerService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        locationServiceBound = true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();
        Log.d(TAG, "onCreate() invoked");
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*Intent startIntent = new Intent(this, LocationTrackerService.class);
        startService(startIntent);*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(locationServiceBound) {
            unbindService(serviceConnection);
            locationServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        Log.d(TAG, "onMapReady() callback invoked");

        try {
            boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed");
            }
        } catch (Resources.NotFoundException nfe) {
            Log.d(TAG, "cant find style, dipshit");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
    }
}
