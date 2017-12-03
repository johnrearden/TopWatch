package com.intricatech.topwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created by Bolgbolg on 16/05/2017.
 *
 * A timer's most important field is its elapsedTime.
 */
public class StopWatch {

    private final String TAG;
    /**
     * A reference to the parent Activity, used for callbacks.
     */
    private MainActivity callbackActivity;

    /**
     * A simplified interface for a DBHelper object (extends SQLiteOpenHelper)
     */
    private DatabaseFacade databaseFacade;

    /**
     * The session currently held in memory.
     */
    private Session session;

    /**
     * The current route, null if using stopwatch to record a new session.
     */
    private Route route;
    /**
     * The current date;
     */
    private Date currentDate;

    /**
     * The total time (nanoseconds) that the timer has been running.
     */
    private long elapsedTime;

    /**
     * The time since the last start event.
     */
    private long lastStartTime;

    /**
     * Flag showing status of StopWatch.
     */
    private boolean isRunning;

    /**
     * The elapsed time since the start of the current split.
     */
    private long currentSplitElapsedTime;

    private double totalDistance;

    /**
     * The time the current split last restarted at.
     */
    private long currentSplitLastStartTime;

    private boolean newSessionReadyToStart;

    private static final String COLON_SEPARATOR = " : ";

    /**
     * The SharedPreferences object for storing Stopwatch's persistent state, and its editor.
     */
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private RecordingType recordingType;

    /**
     * String fields for the SharedPreference Tags.
     */
    private String SHARED_PREF_TAG, SPLIT_LIST_TAG, ELAPSED_TIME, LAST_START_TIME,
                    CURRENT_SPLIT_ELAPSED_TIME, CURRENT_SPLIT_LAST_START_TIME, IS_RUNNING,
                    RECORDING_TYPE, NEW_SESSION_READY_TO_START;

    public StopWatch(MainActivity activity) {

        TAG = getClass().getSimpleName();

        this.callbackActivity = activity;
        databaseFacade = DatabaseFacade.getInstance(activity);

        SHARED_PREF_TAG = activity.getResources().getString(R.string.shared_preferences_tag);
        SPLIT_LIST_TAG = activity.getResources().getString(R.string.split_list_tag);
        ELAPSED_TIME = activity.getResources().getString(R.string.elapsed_time);
        LAST_START_TIME = activity.getResources().getString(R.string.last_start_time);
        CURRENT_SPLIT_ELAPSED_TIME = activity.getResources().getString(R.string.current_split_elapsed_time);
        CURRENT_SPLIT_LAST_START_TIME = activity.getResources().getString(R.string.current_split_last_start_time);
        IS_RUNNING = activity.getResources().getString(R.string.is_running);
        RECORDING_TYPE = "RECORDING_TYPE";
        NEW_SESSION_READY_TO_START = "NEW_SESSION_READY_TO_START";
        sharedPreferences = callbackActivity.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        elapsedTime = 0;
        currentSplitElapsedTime = 0;
        lastStartTime = 0;
        currentSplitLastStartTime = 0;
        isRunning = false;
        recordingType = RecordingType.NEW_RECORDING;

        currentDate = getCurrentDateWithoutTime();

        session = new Session(currentDate);
        route = null;
    }

    /**
     * Handles button press from the parent activity.
     */
    public void onPlayButtonPressed() {
        if(!isRunning()) {
            startTimer();
        } else {
            pauseTimer();
        }
    }

    /**
     * Handles button press from the parent activity. Creates a new split, initializes its
     * currentSplitElapsedTime to zero, and records the time this split was started at, in case the
     * timer is paused during it.
     */
    public void onLapButtonPressed() {
        if (isRunning) {
            createSplit();
            currentSplitElapsedTime = 0;
            currentSplitLastStartTime = getCurrentTime();
        }
    }

    /**
     * Save the current session wrapped in a new Route object to the database.
     * @param name A string chosen by the user.
     */
    public void saveNewRouteToDB(String name) {
        databaseFacade.saveNewRouteToDB(name, session, 0);
    }

    /**
     * Add the current session to the current Route in the database.
     */
    public void saveNewSessionToDB() {
        databaseFacade.saveSessionToDB(route.getRowID(), session);
    }

    /**
     * Handles button press from the parent activity. Sets all relevant fields to zero, and creates
     * a new empty splitList.
     */
    public void onResetButtonPressed() {
        elapsedTime = 0;
        currentSplitElapsedTime = 0;
        lastStartTime = 0;
        currentSplitLastStartTime = 0;
        isRunning = false;
        session.resetSplitList();
        newSessionReadyToStart = true;
    }

    /**
     * Sets the timer running, and stores the time of invocation. Does nothing if timer is
     * already running.
     * @return true if already running, false if not.
     */
    public boolean startTimer() {
        if (isRunning) {
            return true;
        } else {
            isRunning = true;
            long currentTime = getCurrentTime();
            lastStartTime = currentTime;
            currentSplitLastStartTime = currentTime;
            newSessionReadyToStart = false;
            return false;
        }
    }

    /**
     * Pauses the timer, and adds the time since the last startTime to the elapsedTime.
     * @return true if already paused, false if not.
     */
    public boolean pauseTimer() {
        if (!isRunning) {
            return true;
        } else {
            isRunning = false;
            long currentTime = getCurrentTime();
            elapsedTime += currentTime - lastStartTime;
            currentSplitElapsedTime += currentTime - currentSplitLastStartTime;
            return false;
        }
    }

    public boolean isNewSessionReadyToStart() {
        return newSessionReadyToStart;
    }

    public void createSplit() {
        long currentTime = getCurrentTime();
        long nanos = currentSplitElapsedTime + currentTime - currentSplitLastStartTime;
        double dist = calculateDistanceForCurrentSplit();
        Split split = new Split(nanos, dist, session.getSplitList().size());
        session.getSplitList().add(split);
        callbackActivity.onNewSplitCreated(getMostRecentSplitAsString());
    }

    private double calculateDistanceForCurrentSplit() {
        double distForAllPreviousSplits = 0;
        for (Split split : session.getSplitList()) {
            distForAllPreviousSplits += split.getDistance();
        }
        return totalDistance - distForAllPreviousSplits;
    }

    public SpannableString getTotalTimeAsString() {
        if (isRunning()) {
            return getTimeAsSpannableString(getTotalTimeInNanos());
        } else {
            return getTimeAsSpannableString(elapsedTime);
        }
    }

    public SpannableString getTimeSinceLastSplitAsString() {
        if (isRunning()) {
            return getTimeAsSpannableString(getTimeSinceStartOfCurrentSplit());
        } else {
            return getTimeAsSpannableString(currentSplitElapsedTime);
        }
    }

    private long getTotalTimeInNanos() {
        return elapsedTime + (getCurrentTime() - lastStartTime);
    }

    private long getTimeSinceStartOfCurrentSplit() {
        return currentSplitElapsedTime + (getCurrentTime() - currentSplitLastStartTime);
    }

    /**
     * Gets the most recently added split from splitList and returns a SpannableString
     * representation of it. Format as follows :
     *      Index : 2 digits + COLON_SEPARATOR (half-size)
     *      Hours - 1 digit + ":"
     *      Minutes - 2 digits + ":"
     *      Seconds - 2 digits + "."
     *      Hundredths - 2 digits (half-size).
     * @return The SpannableString representation of the most recent split time, with index prefix,
     * or null if splitList is empty.
     */
    public SpannableString getMostRecentSplitAsString() {
        if (!session.getSplitList().isEmpty()) {
            Split spl = session.getSplitList().getLast();
            long splitTime = spl.getSplitTime();
            double distance = spl.getDistance();
            int index = spl.getIndex();

            String indexString = String.format("%02d", index);
            String timeString = getTimeAsString(splitTime);
            String distanceString = "  " + String.format("%.1f", distance) + "m";
            String fullString = indexString + COLON_SEPARATOR + timeString + distanceString;

            SpannableString spStr = new SpannableString(fullString);
            spStr.setSpan(
                    new RelativeSizeSpan(2f),
                    indexString.length() + COLON_SEPARATOR.length(),
                    indexString.length() + COLON_SEPARATOR.length() + 8,
                    0);
            return spStr;
        } else {
            return null;
        }
    }

    public SpannableString getSplitAsString(Split split) {
        Split spl;
        if (split == null) {
            return new SpannableString("empty - split is null");
        } else {
            long splitTime = split.getSplitTime();
            int index = split.getIndex();

            String indexString = String.format("%02d", index);
            String timeString = getTimeAsString(splitTime);
            String fullString = indexString + COLON_SEPARATOR + timeString;

            SpannableString spStr = new SpannableString(fullString);
            spStr.setSpan(
                    new RelativeSizeSpan(2f),
                    indexString.length() + COLON_SEPARATOR.length(),
                    indexString.length() + COLON_SEPARATOR.length() + 8,
                    0);
            return spStr;
        }
    }

    /*
    TODO - Overload this method as follows : getMostRecentSplitAsString(split comparisonSplit) -
    using the best previous split to compare and format accordingly. Colour RED/GREEN?
     */

    /**
     * Takes a time in nanoseconds and returns a SpannableString representation with the following
     * layout :
     *      Hours - 1 digit + ":"
     *      Minutes - 2 digits + ":"
     *      Seconds - 2 digits + "."
     *      Hundredths - 2 digits (half the size of the other digits.
     *
     * @param nanos The time to display
     * @return The SpannableString representation of the time.
     */
    public static SpannableString getTimeAsSpannableString(long nanos) {

        String s = getTimeAsString(nanos);

        // Reduce hundredths in size to 50% of other digits.
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new RelativeSizeSpan(2f), 0, 8, 0);

        return spannableString;
    }

    /**
     * Takes a time in nanoseconds and returns an ordinary String representation with the following
     * layout :
     *      Hours - 1 digit + ":"
     *      Minutes - 2 digits + ":"
     *      Seconds - 2 digits + "."
     *      Hundredths - 2 digits
     * @param nanos The time to display
     * @return The String representation of the time.
     */
    public static String getTimeAsString(long nanos) {

        // Get hours/minutes/seconds String.
        long timeInMilliSeconds = TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
        int hours = (int) ((timeInMilliSeconds / 1000 / 60 / 60) % 24);
        int mins = (int) ((timeInMilliSeconds / 1000 / 60) % 60);
        int seconds = (int) ((timeInMilliSeconds / 1000) % 60);
        int hundredths = (int) ((timeInMilliSeconds / 10) % 100);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%01d", hours) + ":");
        sb.append(String.format("%02d", mins) + ":");
        sb.append(String.format("%02d", seconds) + ".");
        sb.append(String.format("%02d", hundredths));

        return sb.toString();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void saveSharedPreferencesOnStop() {
        Gson gson = new Gson();
        String jsonString = gson.toJson(session.getSplitList());

        sharedPreferencesEditor.putLong(ELAPSED_TIME, elapsedTime);
        sharedPreferencesEditor.putLong(LAST_START_TIME, lastStartTime);
        sharedPreferencesEditor.putLong(CURRENT_SPLIT_ELAPSED_TIME, currentSplitElapsedTime);
        sharedPreferencesEditor.putLong(CURRENT_SPLIT_LAST_START_TIME, currentSplitLastStartTime);
        sharedPreferencesEditor.putString(SPLIT_LIST_TAG, jsonString);
        sharedPreferencesEditor.putBoolean(IS_RUNNING, isRunning);
        sharedPreferencesEditor.putInt(RECORDING_TYPE, recordingType.ordinal());
        sharedPreferencesEditor.putBoolean(NEW_SESSION_READY_TO_START, newSessionReadyToStart);
        Log.d(TAG, "saving stopwatch to SharedPrefs : newSessionReadyToStart = " + newSessionReadyToStart);

        sharedPreferencesEditor.commit();
    }

    public void loadSharedPreferencesOnStart() {
        Gson gson = new Gson();
        LinkedList<Split> retrievedList;
        String jsonString = sharedPreferences.getString(SPLIT_LIST_TAG, "");
        Type type = new TypeToken<LinkedList<Split>>(){}.getType();
        if (jsonString != "") {
            retrievedList = gson.fromJson(jsonString, type);
            session.setSplitList(retrievedList);
            for(Split s : retrievedList) {
                callbackActivity.onNewSplitCreated(getSplitAsString(s));
            }
        }
        elapsedTime = sharedPreferences.getLong(ELAPSED_TIME, 0);
        lastStartTime = sharedPreferences.getLong(LAST_START_TIME, 0);
        currentSplitElapsedTime = sharedPreferences.getLong(CURRENT_SPLIT_ELAPSED_TIME, 0);
        currentSplitLastStartTime = sharedPreferences.getLong(CURRENT_SPLIT_LAST_START_TIME, 0);
        isRunning = sharedPreferences.getBoolean(IS_RUNNING, false);
        recordingType = RecordingType.values()[sharedPreferences.getInt(RECORDING_TYPE, 0)];
        newSessionReadyToStart = sharedPreferences.getBoolean(NEW_SESSION_READY_TO_START, true);
        Log.d(TAG, "loading stopwatch from SharedPrefs : newSessionReadyToStart = " + newSessionReadyToStart);
    }
    
    private long getCurrentTime() {
        return SystemClock.elapsedRealtimeNanos();
    }

    /**
     * Utility method to remove the time information from a date.
     * @return the date with zero time.
     */
    public static Date getCurrentDateWithoutTime() {
        Date dateWithTime = new Date();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date dateWithoutTime = null;
        try {
            dateWithoutTime = formatter.parse(formatter.format(dateWithTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateWithoutTime;
    }

    public RecordingType getRecordingType() {
        return recordingType;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
