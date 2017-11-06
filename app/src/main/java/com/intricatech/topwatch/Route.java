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

    private Session currentSession;
    private Session PBSession;
    private List<Split> bestIndividualSplits;

    public Route(String name, Session session) {
        this.name = name;
        this.currentSession = session;
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public Session getPBSession() {
        return null;
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
}
