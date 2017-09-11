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
     * The distance for this split.
     */
    private final float distance;

    /**
     * The index of this split, also the order of creation.
     */
    private final int index;


    /**
     * Public constructor
     * @param nanos The time taken by this split.
     */
    public Split(long nanos, int index) {
        this(nanos, index, 0);
    }

    public Split(long nanos, int index, float miles) {
        this.splitTime = nanos;
        this.index = index;
        this.distance = miles;
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
