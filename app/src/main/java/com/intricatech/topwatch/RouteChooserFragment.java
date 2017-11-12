package com.intricatech.topwatch;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by Bolgbolg on 11/11/2017.
 */

public class RouteChooserFragment extends ListFragment
                implements AdapterView.OnItemClickListener {

    private static String TAG;
    private List<String> routeList;
    private OnRouteChosenListener routeChosenListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            routeChosenListener = (OnRouteChosenListener) context;
        } catch (ClassCastException cce) {
            cce.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "arguments : " + getArguments().toString());
        routeList = getArguments().getStringArrayList(getResources().getString(R.string.string_array_list));
        ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), R.layout.route_list_textview, routeList);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
        getListView().setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        routeChosenListener.onRouteChosen(routeList.get(position));
    }
}

interface OnRouteChosenListener {
    public void onRouteChosen(String routeName);
}
