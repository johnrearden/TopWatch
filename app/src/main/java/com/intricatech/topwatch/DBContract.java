package com.intricatech.topwatch;

import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by Bolgbolg on 20/09/2017.
 */

public class DBContract {

    private DBContract() {}

    public static final String TAG = "DBContract";
    public static String DATABASE_NAME = "topwatch_database";
    public static int DATABASE_VERSION = 1;

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
                + RouteList.COLUMN_NAME_SPLIT_DISTANCES + " STRING" // stored as csv
                + ")";

        public static final String SQL_DELETE_ROUTE_LIST_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class RouteInfo implements BaseColumns {

        private static String tableName;

        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_TOTAL_TIME = "total_time";
        public static final String COLUMN_NAME_SPLIT_TAG = "split_";
        public static final String TABLE_NAME_PREFIX = "route_";

        public static String getCreateRouteDataTableString (String routeName, int numberOfSplits) {

            tableName = TABLE_NAME_PREFIX + routeName;

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE "
                    + tableName + " ("
                    + RouteInfo.COLUMN_NAME_ID + " INTEGER PRIMARY KEY,"
                    + RouteInfo.COLUMN_NAME_DATE + " INTEGER,"
                    + RouteInfo.COLUMN_NAME_TOTAL_TIME + " INTEGER");
            for (int i = 0; i < numberOfSplits; i++) {
                sb.append("," + RouteInfo.COLUMN_NAME_SPLIT_TAG + String.valueOf(i) + " INTEGER");
            }
            sb.append(")");

            String str = sb.toString();
            Log.d(TAG, str);

            return str;
        }

        public static String getDeleteRouteInfoString(int tableID) {

            tableName = "route_" + String.valueOf(tableID);

            final String SQL_DELETE_ROUTE_INFO_TABLE =
                    "DROP TABLE IF EXISTS " + tableName;

            return SQL_DELETE_ROUTE_INFO_TABLE;

        }
    }

}
