package com.intricatech.topwatch;

import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
                + RouteList.COLUMN_NAME_SPLIT_DISTANCES + " TEXT" // stored as csv
                + ")";

        public static final String SQL_DELETE_ROUTE_LIST_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static String constructSplitDistancesCSVString(List<Split> splitList) {
            int size = splitList.size();
            if (size == 0) {
                return null;
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size - 1; i++) {
                    sb.append(String.valueOf(splitList.get(i).getDistance()) + ",");
                }
                sb.append(String.valueOf(splitList.get(size - 1).getDistance()) + "*");
                return sb.toString();
            }
        }

        /**
         * Decodes a given CSV String into an ArrayList<Double>.
         * Uses null object pattern - creates an empty List and returns it if String is null
         * or empty.
         * @param csvString
         * @return An ArrayList<Double> containing the split distances.
         */
        public static List<Double> decodeSplitDistancesCSVString(String csvString) {
            if (csvString == null || csvString == "") {
                return new ArrayList<>();
            } else {
                List<Double> list = new ArrayList<>();
                String[] splitDistanceStrings = csvString.split(",");
                for (String string : splitDistanceStrings) {
                    if (string.contains("*")) {
                        String lastString = string.substring(0, string.length() - 1);
                        Double d = Double.parseDouble(lastString);
                        list.add(d);
                    } else {
                        Double d = Double.parseDouble(string);
                        list.add(d);
                    }

                }
                return list;
            }
        }
    }

    public static class RouteInfo implements BaseColumns {

        private static String tableName;

        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_TOTAL_TIME = "total_time";
        public static final String COLUMN_NAME_SPLIT_TAG = "split_";
        public static final String TABLE_NAME_PREFIX = "route_";

        public static String getCreateRouteDataTableString (long routeListRowID, int numberOfSplits) {

            tableName = getRouteDataTableName(routeListRowID);

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

        public static String getDeleteRouteInfoString(long routeListRowID) {

            tableName = getRouteDataTableName(routeListRowID);

            final String SQL_DELETE_ROUTE_INFO_TABLE =
                    "DROP TABLE IF EXISTS " + tableName;

            return SQL_DELETE_ROUTE_INFO_TABLE;

        }

        public static String getRouteDataTableName(long routeListRowID) {
            return TABLE_NAME_PREFIX + routeListRowID;
        }
    }

    public static class LocationRecords implements BaseColumns{

        public static String tableName;

        public static final String COL_ID = "_id";
        public static final String COL_TIMESTAMP = "timestamp";
        public static final String COL_LATITUDE = "latitude";
        public static final String COL_LONGITUDE = "longitude";
        public static final String COL_ELEVATION = "elevation";
        public static final String COL_ACCURACY = "accuracy";
        public static final String COL_CUMULATIVE_DISTANCE = "cumulative_distance";
        public static final String COL_HEARTRATE = "heart_rate";
        public static final String COL_PARENT_SPLIT = "parent_split";

        public static final String CURRENT_SESSION_TABLE_NAME = "current_session";
        public static final String PERSONAL_BEST_SESSION_PREFIX = "pb_session_";

        public static String getCreatePBSessionTableString (long rowID) {
            tableName =  getPBSessionTableName(rowID);
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE "
                    + tableName + " ("
                    + COL_ID + " INTEGER PRIMARY KEY,"
                    + COL_TIMESTAMP + " INTEGER,"
                    + COL_LATITUDE + " REAL,"
                    + COL_LONGITUDE + " REAL,"
                    + COL_ELEVATION + " REAL,"
                    + COL_ACCURACY + " REAL,"
                    + COL_CUMULATIVE_DISTANCE + " REAL,"
                    + COL_HEARTRATE + " INTEGER,"
                    + COL_PARENT_SPLIT + " INTEGER"
                    + ")");
            return sb.toString();
        }

        public static String getCreateCurrentSessionTableString () {
            tableName =  CURRENT_SESSION_TABLE_NAME;
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS "
                    + tableName + " ("
                    + COL_ID + " INTEGER PRIMARY KEY,"
                    + COL_TIMESTAMP + " INTEGER,"
                    + COL_LATITUDE + " REAL,"
                    + COL_LONGITUDE + " REAL,"
                    + COL_ELEVATION + " REAL,"
                    + COL_ACCURACY + " REAL,"
                    + COL_CUMULATIVE_DISTANCE + " REAL,"
                    + COL_HEARTRATE + " INTEGER,"
                    + COL_PARENT_SPLIT + " INTEGER"
                    + ")");
            return sb.toString();
        }

        public static String getClearPBSessionTableString (long rowID) {
            tableName = getPBSessionTableName(rowID);
            final String SQL_CLEAR_PB_SESSION_TABLE =
                    "DELETE * FROM " + tableName;
            return SQL_CLEAR_PB_SESSION_TABLE;
        }

        public static String getPBSessionTableName(long rowID) {
            return  PERSONAL_BEST_SESSION_PREFIX + String.valueOf(rowID);
        }

        public static String getClearCurrentSessionTableString() {
            tableName = CURRENT_SESSION_TABLE_NAME;
            String SQL_CLEAR_CURRENT_SESSION_TABLE =
                    "DELETE FROM " + tableName;
            return SQL_CLEAR_CURRENT_SESSION_TABLE;
        }

        public static String getDeleteCurrentSessionTableString() {
            return "DROP TABLE IF EXISTS " + CURRENT_SESSION_TABLE_NAME;
        }

        public static String getDeletePBSessionTableString(long rowID) {
            return "DROP TABLE IF EXISTS " + getPBSessionTableName(rowID);
        }

        public static String getCopyCurrentToPBSessionTableString(long rowID) {
            String PBSessionTableName = getPBSessionTableName(rowID);
            return "INSERT INTO " + PBSessionTableName + " SELECT * FROM " + CURRENT_SESSION_TABLE_NAME;
        }

    }

}
