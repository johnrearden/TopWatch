package com.intricatech.topwatch;

import java.util.List;

/**
 * Created by Bolgbolg on 16/05/2017.
 *
 * A run consists of a list of Splits, with a
 */
public class Route {

    private String name;
    private double distance;
    /**
     * The rowID of this route in the RouteList table (_id column).
     */
    private long rowID;

    private List<Split> bestIndividualSplits;

    public Route(String name) {
        this.name = name;

        rowID = -1;
    }

    public List<Split> getBestIndividualSplits() {
        return bestIndividualSplits;
    }

    public String getName() {
        return name;
    }

    public double getDistance() {
        return distance;
    }

    public long getRowID() {
        return rowID;
    }
}
