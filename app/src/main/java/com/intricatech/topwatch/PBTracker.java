package com.intricatech.topwatch;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bolgbolg on 06/12/2017.
 */

public class PBTracker{

    private static String TAG;

    private long routeID;
    private DatabaseFacade databaseFacade;
    private List<LocationRecord> pbLocationRecords;
    private boolean pbExistsForThisRoute;

    public PBTracker(long routeID) {
        TAG = getClass().getSimpleName();

        this.routeID = routeID;
        databaseFacade = DatabaseFacade.getInstance();
        pbLocationRecords = new ArrayList();
        pbExistsForThisRoute = !databaseFacade.isPBTableEmpty(routeID);
        Log.d(TAG, "PBSession for this route is empty : " + pbExistsForThisRoute);

    }

    public double getDistanceDifferential(Location location, double totalDistanceCovered) {

        // Iterate through the arraylist to find the 2 LocationRecords that straddle the current
        // LocationRecord from this session (comparison using timeStamp). Handle edge cases at
        // start and end of ArrayList.

        return 0.0d;
    }

    public void populatePBLocationRecords() {
        pbLocationRecords = databaseFacade.getPBSessionInfo(routeID);
    }


}
