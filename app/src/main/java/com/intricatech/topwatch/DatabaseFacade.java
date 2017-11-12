package com.intricatech.topwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import static com.intricatech.topwatch.DBContract.RouteInfo;
import static com.intricatech.topwatch.DBContract.RouteList;

/**
 * Created by Bolgbolg on 06/11/2017.
 */

public class DatabaseFacade {

    private final String TAG;
    private static DatabaseFacade instance;
    public static DatabaseFacade getInstance(Context context) {
        if (instance != null) {
            return instance;
        } else {
            instance = new DatabaseFacade(context);
            return instance;
        }
    }

    private DBHelper helper;
    private SQLiteDatabase database;

    private DatabaseFacade(Context context) {
        TAG = getClass().getSimpleName();

        helper = new DBHelper(context);
        database = helper.getWritableDatabase();
    }

    public void saveNewRouteToDB(String routeName, Session session, double distance) {

        int numberOfSplits = session.getSplitList().size();

        String splitDistances = null;
        if (session.getSplitDistances().size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(session.getSplitDistances().get(0));
            for (int i = 1; i < numberOfSplits; i++) {
                sb.append("," + String.valueOf(session.getSplitDistances().get(i)));
            }
            splitDistances = sb.toString();
        } else {
            splitDistances = "";
        }

        /* Todo - replace all spaces in routeName with underscores, and reverse process anywhere
        they are read.
         */

        ContentValues values = new ContentValues();
        values.put(RouteList.COLUMN_NAME_NAME, routeName);
        values.put(RouteList.COLUMN_NAME_NUMBER_OF_SPLITS, numberOfSplits);
        values.put(RouteList.COLUMN_NAME_DISTANCE, distance);
        values.put(RouteList.COLUMN_NAME_SPLIT_DISTANCES, splitDistances);

        database.insert(RouteList.TABLE_NAME, null, values);

        // Create a new table for this new route.
        database.execSQL(RouteInfo.getCreateRouteDataTableString(routeName, numberOfSplits));

        saveSessionToDB(routeName, session);

        getArrayListOfRoutes();
    }

    public void saveSessionToDB(String routeName, Session session){

        Log.d(TAG, "Session to save to database : " + session.toString());
        int numberOfSplits = session.getSplitList().size();

        ContentValues values = new ContentValues();
        values.put(RouteInfo.COLUMN_NAME_DATE, session.getDate().getTime());
        values.put(RouteInfo.COLUMN_NAME_TOTAL_TIME, session.getTotalTime());

        for (int i = 0; i < numberOfSplits; i++) {
            String columnName = RouteInfo.COLUMN_NAME_SPLIT_TAG + String.valueOf(i);
            values.put(columnName, session.getSplitList().get(i).getSplitTime());
        }

        Log.d(TAG, "Content values = \n" + values.toString());

        database.insert(RouteInfo.TABLE_NAME_PREFIX + routeName, null, values);
    }

    public Session getSessionsFromDB(String routeName) {

        Session session = new Session();

        // Get number of splits in this route.
        String rawQueryString = "SELECT " + RouteList.COLUMN_NAME_NUMBER_OF_SPLITS + " FROM "
                + RouteList.TABLE_NAME + " WHERE COLUMN = " + routeName;
        Cursor cursor = database.rawQuery(rawQueryString, null);
        cursor.moveToFirst();
        int numberOfSplits = cursor.getInt(0);
        Log.d(TAG, "getSessionFromDB : numberOfSplits == " + numberOfSplits);

        // Load the session data from the database.
        String[] projection = new String[numberOfSplits + 2];
        projection[0] = RouteInfo.COLUMN_NAME_DATE;
        projection[1] = RouteInfo.COLUMN_NAME_TOTAL_TIME;
        for (int i = 0; i < numberOfSplits; i++) {
            projection[i + 2] = (RouteInfo.COLUMN_NAME_SPLIT_TAG + i);
        }

        return session;
    }

    public ArrayList<String> getArrayListOfRoutes() {

        String[] projection = {
                RouteList.COLUMN_NAME_NAME,
                RouteList.COLUMN_NAME_DISTANCE
        };
        String sortOrder = RouteList.COLUMN_NAME_DISTANCE + " DESC";

        Cursor cursor = database.query(
                RouteList.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
        Log.d(TAG, "cursor.getCount() returns " + cursor.getCount());

        ArrayList<String> resultList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String aName = cursor.getString(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_NAME));
            double dist = cursor.getDouble(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_DISTANCE));
            String aDist = String.format("%.2f", dist);
            String aResult = aName + ", " + dist + "m";
            resultList.add(aResult);
            Log.d(TAG, "Route : " + aResult);
        }

        return resultList;

        /*while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_NAME));
            double distance = cursor.getDouble(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_DISTANCE));
            Log.d(TAG, "Name == " + name + ", dist : " + String.valueOf(distance));
        }

        return null;*/
    }

    public Session[] getListOfSessionsForRoute(Route route) {
        return null;
    }

    public Route getRouteFromDB(){

        // ToDo - Just do it.

        return null;
    }
}
