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
import android.widget.ImageButton;
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
    private ImageButton routeDeleteButton;
    private int indexToDelete;
    private SimpleCursorAdapter simpleCursorAdapter;

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
        getActivity().setTitle("Choose a route.");

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
        simpleCursorAdapter = new SimpleCursorAdapter(
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

        routeDeleteButton = (ImageButton) getListView().getChildAt(position).findViewById(R.id.route_delete_button);
        if (routeDeleteButton.getVisibility() == View.VISIBLE) {
            routeDeleteButton.setVisibility(View.GONE);
        } else if (routeDeleteButton.getVisibility() == View.GONE) {
            routeDeleteButton.setVisibility(View.VISIBLE);
            routeDeleteButton.setOnClickListener(deleteRouteListener);
            indexToDelete = position;
        }
        return true;
    }

    private View.OnClickListener deleteRouteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "delete this fucking view man");
            // Confirm user action.
            UserUtilities.getGenericUserConfirmation(getContext(), new UserUtilities.UserConfirmationCallback() {
                @Override
                public void onUserConfirms() {
                    deleteRouteAndDataTable(indexToDelete);
                }

                @Override
                public void onUserDenies() {}
            });
        }
    };

    private void deleteRouteAndDataTable(int indexToDelete) {
        cursor.moveToPosition(indexToDelete);
        long rowId = cursor.getLong(cursor.getColumnIndexOrThrow(RouteList.COLUMN_NAME_ID));
        DatabaseFacade.getInstance(getContext()).deleteRoute(rowId);
        cursor = DatabaseFacade.getInstance(getContext()).getRouteListCursor();
        simpleCursorAdapter.changeCursor(cursor);
        simpleCursorAdapter.notifyDataSetChanged();
    }

}

interface OnRouteChosenListener {
    public void onRouteChosen(String routeName);
}
