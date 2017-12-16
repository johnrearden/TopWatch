package com.intricatech.topwatch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Created by Bolgbolg on 04/12/2017.
 */

public class CustomMapFragment extends SupportMapFragment
                                implements OnMapReadyCallback{

    private static String TAG;
    private static final float DEFAULT_ZOOM = 16.0f;
    private static int instanceNumber = 0;

    private GoogleMap map;
    private DatabaseFacade databaseFacade;
    private PolylineOptions polyLineOptions;
    private Polyline polyLine;
    private boolean mapIsAvailable;

    public void setUserZoom(float userZoom) {
        this.userZoom = userZoom;
    }

    private float userZoom;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // todo initialize map with last known location.
        TAG = getClass().getSimpleName();
        Log.d(TAG, "onCreate() invoked ... Instance number " + ++instanceNumber);

        mapIsAvailable = false;
        userZoom = DEFAULT_ZOOM;
        databaseFacade = DatabaseFacade.getInstance();
        getMapAsync(this);

        initializePolylineOptions();
        for (LatLng latLng : databaseFacade.getAllLocationsFromCurrentSession()) {
            polyLineOptions.add(latLng);
        }
        Log.d(TAG, "polyLineOptions.size() after pull from database == " + polyLineOptions.getPoints().size());
    }

    private void initializePolylineOptions() {
        polyLineOptions = new PolylineOptions();
        polyLineOptions.color(Color.WHITE);
        polyLineOptions.width(10.0f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        map.setOnCameraIdleListener(null);
        onCameraIdleListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        mapIsAvailable = true;
        map.setOnCameraIdleListener(onCameraIdleListener);
        Log.d(TAG, "onMapReady() callback invoked");

        try {
            boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed");
            }
        } catch (Resources.NotFoundException nfe) {
            Log.d(TAG, "cant find style");
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.animateCamera(CameraUpdateFactory.zoomTo(userZoom));
        map.getUiSettings().setScrollGesturesEnabled(false);

        // Assign the polyline to the map, and draw any LatLngs drawn from the database.
        polyLine = map.addPolyline(polyLineOptions);

    }

    public void updateMapWithLocationOnly(LatLng latLng, boolean addToPolyline) {
        if (addToPolyline) {
            List<LatLng> list = polyLine.getPoints();
            list.add(latLng);
            polyLine.setPoints(list);
        }
        Log.d(TAG, "polyLineOptions.size() after update == " + polyLineOptions.getPoints().size());
        if (mapIsAvailable) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, userZoom));
        }
    }

    GoogleMap.OnCameraIdleListener onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
        @Override
        public void onCameraIdle() {
            setUserZoom(map.getCameraPosition().zoom);
        }
    };

    public void onResetSession() {
        initializePolylineOptions();
        if (mapIsAvailable) {
            map.clear();
            map.addPolyline(polyLineOptions);
        }
    }



}
