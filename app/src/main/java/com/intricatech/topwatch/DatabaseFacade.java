package com.intricatech.topwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static com.intricatech.topwatch.DBContract.LocationRecords;
import static com.intricatech.topwatch.DBContract.RouteInfo;
import static com.intricatech.topwatch.DBContract.RouteList;

/**
 * Created by Bolgbolg on 06/11/2017.
 */

public class DatabaseFacade {

    private static final String TAG = "DatabaseFacade";

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
        helper = new DBHelper(context);
        database = helper.getWritableDatabase();
        //database.enableWriteAheadLogging();
    }

    public void saveNewRouteToDB(String routeName, Session session, double distance) {

        int numberOfSplits = session.getSplitList().size();

        String splitDistances;
        if (numberOfSplits > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(session.getSplitList().get(0).getDistance());
            for (int i = 1; i < numberOfSplits; i++) {
                sb.append("," + String.valueOf(session.getSplitList().get(i).getDistance()));
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

        long rowID = database.insert(RouteList.TABLE_NAME, null, values);

        // Create a new table for this new route.
        database.execSQL(RouteInfo.getCreateRouteDataTableString(rowID, numberOfSplits));

        saveSessionToDB(rowID, session);

        logAllDatabaseTables();

    }

    public void saveSessionToDB(long rowID, Session session){

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

        database.insert(RouteInfo.getRouteDataTableName(rowID), null, values);
    }

    public Session getSessionsFromDB(long rowID) {

        Session session = new Session();

        // Get number of splits in this route.
        String rawQueryString = "SELECT " + RouteList.COLUMN_NAME_NUMBER_OF_SPLITS + " FROM "
                + RouteList.TABLE_NAME + " WHERE COLUMN = "
                + RouteInfo.getRouteDataTableName(rowID);
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

    public Cursor getRouteListCursor() {
        String[] projection = {
                RouteList.COLUMN_NAME_ID,
                RouteList.COLUMN_NAME_NAME,
                RouteList.COLUMN_NAME_DISTANCE,
                RouteList.COLUMN_NAME_NUMBER_OF_SPLITS,
                RouteList.COLUMN_NAME_SPLIT_DISTANCES
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
        return cursor;
    }

    public Session[] getListOfSessionsForRoute(Route route) {
        return null;
    }

    public Route getRouteFromDB(){

        // ToDo - Just do it.

        return null;
    }

    public void deleteRoute(long rowID) {

        // Delete this entry from the RouteList table.
        String where = RouteList.COLUMN_NAME_ID + " = ?";
        String[] whereArgs = {String.valueOf(rowID)};
        database.delete(RouteList.TABLE_NAME, where, whereArgs);

        // Delete its corresponding RouteInfo table.
        String execSQLString = DBContract.RouteInfo.getDeleteRouteInfoString(rowID);
        database.execSQL(execSQLString);

        logAllDatabaseTables();
    }

    public void createNewCurrentSessionTable() {
        //database.execSQL(LocationRecords.getDeleteCurrentSessionTableString());
        database.execSQL(LocationRecords.getCreateCurrentSessionTableString());
        Log.d(TAG, "new session table created");
    }

    public void clearCurrentSessionTable() {
        database.execSQL(LocationRecords.getClearCurrentSessionTableString());
        Log.d(TAG, "current session table cleared");
    }

    public void copyCurrentSessionToPBSession(String routeName) {

    }

    public void commitLocationRecord(long timeStamp, Location location, int splitIndex) {
        ContentValues values = new ContentValues();
        values.put(LocationRecords.COL_TIMESTAMP, timeStamp);
        values.put(LocationRecords.COL_LATITUDE, location.getLatitude());
        values.put(LocationRecords.COL_LONGITUDE, location.getLongitude());
        values.put(LocationRecords.COL_ELEVATION, location.getAltitude());
        values.put(LocationRecords.COL_ACCURACY, location.getAccuracy());
        values.put(LocationRecords.COL_PARENT_SPLIT, splitIndex);

        database.insert(
                LocationRecords.CURRENT_SESSION_TABLE_NAME,
                null,
                values
        );
        Log.d(TAG, "location record committed to current session table");
    }

    public List<LatLng> getAllLocationsFromCurrentSession() {
        List<LatLng> locs = new ArrayList<>();
        String[] projection = new String[]{
                LocationRecords.COL_LATITUDE,
                LocationRecords.COL_LONGITUDE};
        String sortOrder = LocationRecords.COL_TIMESTAMP + " ASC";
        Cursor cursor = database.query(
                LocationRecords.CURRENT_SESSION_TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
        while (cursor.moveToNext()){
            locs.add(new LatLng(
                    cursor.getDouble(cursor.getColumnIndexOrThrow(LocationRecords.COL_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(LocationRecords.COL_LONGITUDE))
            ));
        }
        return locs;
    }

    public void logAllDatabaseTables() {
        Cursor c = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        Log.d(TAG, "Existing tables :");
        if (c.moveToFirst()) {
            while ( !c.isAfterLast() ) {
                Log.d(TAG, "... table : " + c.getString(0));
                c.moveToNext();
            }
        }
    }

    public long getPBTimeForRoute(long rowID) {
        List<Long> times = new ArrayList<>();
        String[] projection = {RouteInfo.COLUMN_NAME_TOTAL_TIME};
        Cursor cursor = database.query(
                RouteInfo.getRouteDataTableName(rowID),
                projection,
                null, null, null, null,
                RouteInfo.COLUMN_NAME_TOTAL_TIME + " DESC"
        );
        if (cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(RouteInfo.COLUMN_NAME_TOTAL_TIME));
        } else {
            return 0;
        }
    }

}
