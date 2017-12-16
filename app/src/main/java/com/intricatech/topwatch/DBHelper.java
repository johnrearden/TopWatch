package com.intricatech.topwatch;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.intricatech.topwatch.DBContract.*;


/**
 * Created by Bolgbolg on 06/11/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //OnDestroyDirector director = (OnDestroyDirector) context;
        //director.register(this);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RouteList.SQL_CREATE_ROUTE_LIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onMainActivityDestroyed() {
        close();
    }
}
