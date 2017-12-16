package com.intricatech.topwatch;

/**
 * Created by Bolgbolg on 06/12/2017.
 */

public class LocationRecord {

    private final double latitude;
    private final double longitude;
    private final double elevation;
    private final double accuracy;
    private final long timeStamp;
    private final int parentSplit;

    public LocationRecord(
            double latitude,
            double longitude,
            double elevation,
            double accuracy,
            long timeStamp,
            int parentSplit) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.accuracy = accuracy;
        this.timeStamp = timeStamp;
        this.parentSplit = parentSplit;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getElevation() {
        return elevation;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    public double getParentSplit() {
        return parentSplit;
    }
}
