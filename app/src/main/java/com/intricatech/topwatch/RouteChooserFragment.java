package com.intricatech.topwatch;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

import static com.intricatech.topwatch.DBContract.RouteList;
/**
 * Created by Bolgbolg on 11/11/2017.
 */

public class RouteChooserFragment extends ListFragment
                implements AdapterView.OnItemClickListener,
                           AdapterView.OnItemLongClickListener {

    private static String TAG;
    private OnRouteChosenListener routeChosenListener;
    private Cursor cursor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach() invoked");
        try {
            routeChosenListener = (OnRouteChosenListener) context;
        } catch (ClassCastException cce) {
            cce.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() invoked");
        View view = inflater.inflate(R.layout.route_chooser_fragment_layout, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() invoked");

        cursor = DatabaseFacade.getInstance(getActivity()).getRouteListCursor();
        String[] cols = new String[]{
                RouteList.COLUMN_NAME_NAME,
                RouteList.COLUMN_NAME_DISTANCE,
                RouteList.COLUMN_NAME_NUMBER_OF_SPLITS};
        int[] views = new int[]{
                R.id.route_name,
                R.id.route_distance,
                R.id.route_number_of_splits
        };
        Log.d(TAG, cursor.toString());
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.route_chooser_list_element,
                cursor,
                cols,
                views);
        setListAdapter(simpleCursorAdapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        cursor.moveToPosition(position);
        String str = cursor.getString(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_NAME));
        routeChosenListener.onRouteChosen(str);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Vibrations.getInstance().doLongVibrate();
        return true;
    }
}

interface OnRouteChosenListener {
    public void onRouteChosen(String routeName);
}
