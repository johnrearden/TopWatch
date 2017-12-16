package com.intricatech.topwatch;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

/**
 * Created by Bolgbolg on 05/12/2017.
 */

public class SwipeViewPagerAdapter extends FragmentStatePagerAdapter {

    private static int instanceNumber = 0;
    private static String TAG;

    private CustomMapFragment customMapFragment;
    private SplitDisplayFragment splitDisplayFragment;
    private RouteTrackerFragment routeTrackerFragment;

    public SwipeViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        TAG = getClass().getSimpleName();
        Log.d(TAG, "creating instance number " + ++instanceNumber);

        customMapFragment = new CustomMapFragment();
        splitDisplayFragment = new SplitDisplayFragment();
        routeTrackerFragment = new RouteTrackerFragment();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return customMapFragment;
            case 1:
                return splitDisplayFragment;
            case 2:
                return routeTrackerFragment;
            default:
                return splitDisplayFragment;
        }
    }

    public void onMainActivityDestroyed() {
        customMapFragment = null;
        splitDisplayFragment = null;
        routeTrackerFragment = null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    public CustomMapFragment getCustomMapFragment() {
        return customMapFragment;
    }

    public SplitDisplayFragment getSplitDisplayFragment() {
        return splitDisplayFragment;
    }

    public RouteTrackerFragment getRouteTrackerFragment() {
        return routeTrackerFragment;
    }
}
