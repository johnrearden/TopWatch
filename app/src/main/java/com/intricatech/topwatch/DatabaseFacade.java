package com.intricatech.topwatch;

import android.content.Context;

/**
 * Created by Bolgbolg on 06/11/2017.
 */

public class DatabaseFacade {

    private DBHelper helper;

    public DatabaseFacade(Context context) {
        helper = new DBHelper(context);
    }

    public void saveNewRouteToDB(String name, Session session) {

        // ToDo - Just do it.
    }

    public Route getRouteFromDB(){

        // ToDo - Just do it.
        
        return null;
    }
}
