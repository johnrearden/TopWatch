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

    public double getTotalDistance() {
        return 0.0d;
    }

    public double getTotalTime() {
        return 0.0d;
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
}
