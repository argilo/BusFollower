/*
 * Copyright 2015 Clayton Smith
 *
 * This file is part of Ottawa Bus Follower.
 *
 * Ottawa Bus Follower is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3, or (at
 * your option) any later version.
 *
 * Ottawa Bus Follower is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ottawa Bus Follower; see the file COPYING.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.argilo.busfollower.BusFollowerContract.SavedQuery;
import net.argilo.busfollower.BusFollowerContract.SavedRouteDirection;

public class BusFollowerDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "BusFollower.db";

    private static final String SQL_CREATE_SAVED_QUERIES =
            "CREATE TABLE " + SavedQuery.TABLE_NAME + " (" +
            SavedQuery._ID + " INTEGER PRIMARY KEY, " +
            SavedQuery.STOP_NUMBER + " TEXT, " +
            SavedQuery.TIMES_QUERIED + " INTEGER, " +
            SavedQuery.LAST_QUERIED + " INTEGER" +
            ")";

    private static final String SQL_CREATE_SAVED_ROUTE_DIRECTIONS =
            "CREATE TABLE " + SavedRouteDirection.TABLE_NAME + " (" +
            SavedRouteDirection._ID + " INTEGER PRIMARY KEY, " +
            SavedRouteDirection.SAVED_QUERY_ID + " INTEGER, " +
            SavedRouteDirection.ROUTE_NUMBER + " TEXT, " +
            SavedRouteDirection.ROUTE_LABEL + " TEXT, " +
            SavedRouteDirection.DIRECTION_ID + " TEXT, " +
            SavedRouteDirection.DIRECTION + " TEXT, " +
            ")";

    public BusFollowerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SAVED_QUERIES);
        db.execSQL(SQL_CREATE_SAVED_ROUTE_DIRECTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not needed yet.
    }
}
