package com.intricatech.topwatch;

/**
 * Created by Bolgbolg on 16/12/2017.
 */

// todo - have stopwatch TimerTask call MainActivity periodically to check for revocation of location permission.
public enum GPSQuality {
    EXCELLENT(R.drawable.gps_green),
    OK(R.drawable.gps_yellow),
    POOR(R.drawable.gps_orange),
    BAD(R.drawable.gps_red),
    OFF(R.drawable.gps_off);

    int resourceID;

    GPSQuality(int id) {
        this.resourceID = id;
    }

    int getResourceID() {
        return resourceID;
    }

    GPSQuality setQuality(double accuracy) {
        if (accuracy == 0) {
            return OFF;
        } else if (accuracy <= 10.0f) {
            return EXCELLENT;
        } else if (accuracy <= 20.0f) {
            return OK;
        } else if (accuracy <= 50.0f) {
            return POOR;
        } else return BAD;
    }
}
