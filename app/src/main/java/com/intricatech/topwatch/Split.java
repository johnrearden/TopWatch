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
    private double distance;

    /**
     * The index of this split, also the order of creation.
     */
    private final int index;

    /**
     *
     * @param nanos time
     * @param index
     */
    public Split(long nanos, int index) {
        this(nanos, 0, index);
    }

    public Split(long nanos, double distance, int index) {
        this.splitTime = nanos;
        this.distance = distance;
        this.index = index;
    }

    public long getSplitTime() {
        return splitTime;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double dist) {
        this.distance = dist;
    }

    public int getIndex() {
        return index;
    }

}
