package com.intricatech.topwatch;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by Bolgbolg on 06/11/2017.
 */

public class Session {

    private LinkedList<Split> splitList;
    private Date date;

    public Session(Date date) {

        this.date = date;
        splitList = new LinkedList<>();
    }

    public Session() {
        this(null);
    }

    public double getTotalDistance() {
        double cumulativeDistance = 0;
        for (Split split : splitList) {
            cumulativeDistance += split.getDistance();
        }
        return cumulativeDistance;
    }

    public long getTotalTime() {
        long cumulativeTime = 0;
        for (Split split : splitList) {
            cumulativeTime += split.getSplitTime();
        }
        return cumulativeTime;
    }

    public LinkedList<Split> getSplitList() {
        return splitList;
    }

    public Date getDate() {
        return date;
    }

    public void resetSplitList() {
        splitList = new LinkedList<>();
    }

    public void setSplitList(LinkedList<Split> list) {
        this.splitList = list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(date.toString() + "\n");
        for (int i = 0; i < splitList.size(); i++) {
            sb.append("Time : " + splitList.get(i).getSplitTime() + "\n");
        }
        return sb.toString();
    }
}
