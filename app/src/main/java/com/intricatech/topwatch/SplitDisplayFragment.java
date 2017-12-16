package com.intricatech.topwatch;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Bolgbolg on 04/12/2017.
 */

public class SplitDisplayFragment extends Fragment {

    private static String TAG;
    private static int instanceNumber = 0;

    private TextView timeSinceLastSplitTV;
    private LinearLayout splitTimesLL;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();
        Log.d(TAG, "instance number " + ++instanceNumber);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.split_display_fragment_main, container, false);
        timeSinceLastSplitTV = (TextView) view.findViewById(R.id.time_since_last_split);
        splitTimesLL = (LinearLayout) view.findViewById(R.id.split_times_linearlayout);

        // Old saved splits can't be loaded until the Fragment view has been created, so
        // call back for them now.
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.getSavedSplitsIfAny();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void updateTimeSinceLastSplitTV(SpannableString timeString) {
        if (timeSinceLastSplitTV != null) {
            timeSinceLastSplitTV.setText(timeString);
        }
    }

    public void onNewSplitCreated(SpannableString timeSS, SpannableString distanceSS) {
        Activity activity = getActivity();
        Log.d(TAG, "onNewSplitCreated() invoked");
        if (activity == null) {
            Log.d(TAG, "Activity is null, instance = " + instanceNumber);
            return;
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout ll = (LinearLayout) inflater.inflate(
                R.layout.split_time_element,
                null,
                false);
        TextView newSplitTimeTV = (TextView) ll.findViewById(R.id.split_time_textview);
        TextView newSplitDistTV = (TextView) ll.findViewById(R.id.split_distance_textview);
        newSplitTimeTV.setText(timeSS);
        newSplitDistTV.setText(distanceSS);
        splitTimesLL.addView(ll);
    }

    public void removeViewsFromSplitLayout() {
        Log.d(TAG, "removeAllViews() invoked on instance " + instanceNumber);
        splitTimesLL.removeAllViews();
    }
}
