package com.intricatech.topwatch;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by Bolgbolg on 16/11/2017.
 */

public class Vibrations {

    private static final int LONG_VIBRATION_MILLIS = 200;
    private static final int SHORT_VIBRATION_MILLIS = 100;
    private static Vibrations instance;
    private static Vibrator vibrator;

    public static void initialize(Context context) {
        instance = new Vibrations(context);
    }

    public static Vibrations getInstance() {
        return instance;
    }

    private Vibrations(Context context) {
        vibrator = (Vibrator )context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void doLongVibrate() {
        vibrator.vibrate(LONG_VIBRATION_MILLIS);
    }

    public void doShortVibrate() {
        vibrator.vibrate(SHORT_VIBRATION_MILLIS);
    }

}
