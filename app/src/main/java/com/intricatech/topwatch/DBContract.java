package com.intricatech.topwatch;

import android.provider.BaseColumns;

/**
 * Created by Bolgbolg on 20/09/2017.
 */

public class DBContract {

    private DBContract() {}

    public static class RouteList implements BaseColumns{

        public static final String TABLE_NAME = "route_list";

        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_NUMBER_OF_SPLITS = "number_of_splits";
        public static final String COLUMN_NAME_DISTANCE = "distance";
        public static final String COLUMN_NAME_SPLIT_DISTANCES = "split_distances";

        public static final String SQL_CREATE_ROUTE_LIST_TABLE =
                "CREATE TABLE "
                + TABLE_NAME + " ("
                + RouteList.COLUMN_NAME_ID + " INTEGER PRIMARY KEY, "
                + RouteList.COLUMN_NAME_NAME + " TEXT NOT NULL, "
                + RouteList.COLUMN_NAME_NUMBER_OF_SPLITS + " INTEGER, "
                + RouteList.COLUMN_NAME_DISTANCE + " REAL, "
                + RouteList.COLUMN_NAME_SPLIT_DISTANCES + " BLOB"
                + ")";

        public static final String SQL_DELETE_ROUTE_LIST_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class RouteInfo implements BaseColumns {

        private static String TABLE_NAME;

        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_SPLIT_TIMES = "split_times";
        public static final String COLUMN_NAME_GPS_POINTS = "gps_points";

        public static String getCreateTableString (int tableID) {

            TABLE_NAME = "route_" + String.valueOf(tableID);

            final String SQL_CREATE_ROUTE_LIST_TABLE =
                    "CREATE TABLE"
                    + TABLE_NAME + " ("
                    + RouteInfo.COLUMN_NAME_ID + " INTEGER PRIMARY KEY"
                    + RouteInfo.COLUMN_NAME_SPLIT_TIMES + " BLOB"
                    + RouteInfo.COLUMN_NAME_GPS_POINTS + " BLOB"
                    + ")";

            return SQL_CREATE_ROUTE_LIST_TABLE;
        }

        public static String getDeleteRouteInfoString(int tableID) {

            TABLE_NAME = "route_" + String.valueOf(tableID);

            final String SQL_DELETE_ROUTE_INFO_TABLE =
                    "DROP TABLE IF EXISTS " + TABLE_NAME;

            return SQL_DELETE_ROUTE_INFO_TABLE;

        }
    }

}
