package com.intricatech.topwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

/**
 * Created by Bolgbolg on 29/11/2017.
 */

public class UserUtilities {

    private static String CONFIRMATION_MESSAGE = "Are you sure?";
    private static String TAG = "UserUtilities";

    static AlertDialog dialog;
    private static AlertDialog.Builder builder;
    private static boolean result;

    public static void getUserConfirmation(Context context, final UserConfirmationCallback callback, String alertMessage) {
        builder = new AlertDialog.Builder(context);
        builder.setMessage(alertMessage)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onUserConfirms();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onUserDenies();
                    }
                })
                .setCancelable(false);
        builder.setMessage(alertMessage);
        dialog = builder.create();
        dialog.show();
        Log.d(TAG, "result == " + result);

    }

    public static void getGenericUserConfirmation(Context context, final UserConfirmationCallback callback) {
        builder = new AlertDialog.Builder(context);
        builder.setMessage(CONFIRMATION_MESSAGE)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onUserConfirms();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onUserDenies();
                    }
                })
                .setCancelable(false);
        dialog = builder.create();
        dialog.show();
        Log.d(TAG, "result == " + result);

    }

    public interface UserConfirmationCallback {
        public void onUserConfirms();
        public void onUserDenies();
    }
}
