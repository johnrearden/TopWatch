package com.intricatech.topwatch;

/**
 * Created by Bolgbolg on 16/05/2017.
 */
public final class Split {

    /**
     * The time for this split.
     */
    private final long splitTime;

    /**
     * The distance for this split, in meters.
     */
    private float distance;

    /**
     * The index of this split, also the order of creation.
     */
    private final int index;

    private GPSCoords startCoords, finishCoords;


    /**
     *
     * @param nanos time
     * @param index
     */
    public Split(long nanos, int index) {
        this(nanos, index, null, null);
    }

    public Split(long nanos, int index, GPSCoords start, GPSCoords finish) {
        this.splitTime = nanos;
        this.index = index;
        this.startCoords = start;
        this.finishCoords = finish;
        if (start != null && finish != null) {
            distance = GPSCoords.getDistanceInMeters(startCoords, finishCoords);
        } else {
            distance = 0;
        }
    }

    /**
     * Sets the GPSCoords for this split (in the event that location services were unavailable at
     * the time of recording) and calculates the distance between them in meters.
     * @param start
     * @param finish
     */
    public void setGPSCoords(GPSCoords start, GPSCoords finish) {
        this.startCoords = start;
        this.finishCoords = finish;
        distance = GPSCoords.getDistanceInMeters(startCoords, finishCoords);
    }

    public long getSplitTime() {
        return splitTime;
    }

    public float getDistance() {
        return distance;
    }

    public int getIndex() {
        return index;
    }

}
