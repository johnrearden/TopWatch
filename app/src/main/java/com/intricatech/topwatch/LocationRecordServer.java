package com.intricatech.topwatch;

import android.app.Activity;

/**
 * Created by Bolgbolg on 17/11/2017.
 */

public interface LocationRecordServer {

    void registerActivity(Activity activity, LocationRecordClient callback);

    void unregisterActivity(Activity activity);

    LocationRecord requestNewLocationRecord();

    void startSession();

    void pauseSession();

    void restartSession();

    void newSplitStarted();

    void finishSessionAndCommit(String routeName);

    void finishSessionAndDelete();

}
