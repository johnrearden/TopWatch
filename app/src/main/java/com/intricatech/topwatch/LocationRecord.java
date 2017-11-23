package com.intricatech.topwatch;

/**
 * Created by Bolgbolg on 17/11/2017.
 */

public class LocationRecord {

    private final double latitude;

    private final double longitude;

    private final double elevation;

    private final long timeStampAbsolute;

    private long stopwatchTimeStamp;

    public LocationRecord(
            double latitude,
            double longitude,
            double elevation,
            long currentTime) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.timeStampAbsolute = currentTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public long getTimeStampAbsolute() {
        return timeStampAbsolute;
    }

    public long getStopwatchTimeStamp() {
        return stopwatchTimeStamp;
    }

    public void setStopwatchTimeStamp(long stopwatchTimeStamp) {
        this.stopwatchTimeStamp = stopwatchTimeStamp;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("timeStampAbsolute == " + getTimeStampAbsolute() + "\n");
        sb.append("latitude == " + getLatitude() + "\n");
        sb.append("longitude == " + getLongitude() + "\n");
        sb.append("elevation == " + getElevation() + "\n");
        return sb.toString();
    }
}
