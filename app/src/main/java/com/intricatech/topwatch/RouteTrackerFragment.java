package com.intricatech.topwatch;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Bolgbolg on 05/12/2017.
 */

public class RouteTrackerFragment extends Fragment {

    private boolean routeLoaded;
    private PBTracker pbTracker;
    private Route currentRoute;
    private boolean PBExists;

    private TextView currentSplitTimeTV;
    private TextView targetTimeTV;
    private TextView targetDistanceTV;
    private TextView percentCompleteTV;
    private TextView distanceDifferentialTV;
    private LinearLayout previousSplitsDisplay;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeLoaded = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.route_tracker_fragment_main, container, false);
        currentSplitTimeTV = (TextView) view.findViewById(R.id.route_tracker_current_split_time);
        targetTimeTV = (TextView) view.findViewById(R.id.route_tracker_target_time);
        targetDistanceTV = (TextView) view.findViewById(R.id.route_tracker_target_distance);
        percentCompleteTV = (TextView) view.findViewById(R.id.route_tracker_percent_complete);
        distanceDifferentialTV = (TextView) view.findViewById(R.id.route_tracker_distance_differential);

        return view;

    }

    public void onLocationUpdatedWhileStopped(Location location) {
        if (!routeLoaded) {
            return;
        }
    }

    public void onTotalDistanceCoveredUpdated(double totalDistance) {
        if (!routeLoaded) {
            return;
        }
        double percentage = totalDistance / currentRoute.getDistance() * 100;
        String percentageString = String.valueOf((int) percentage) + "%";
        percentCompleteTV.setText(percentageString);
    }

    public void updateCurrentSplitTime(SpannableString timeString) {
        if (currentSplitTimeTV != null) {
            currentSplitTimeTV.setText(timeString);
        }
    }

    public void onRouteLoaded(long routeRowID) {
        routeLoaded = true;
        pbTracker = new PBTracker(routeRowID);

    }
}
