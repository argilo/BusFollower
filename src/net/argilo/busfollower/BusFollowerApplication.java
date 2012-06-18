/*
 * Copyright 2012 Clayton Smith
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

import net.argilo.busfollower.ocdata.DatabaseHelper;
import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

public class BusFollowerApplication extends Application {
    private SQLiteDatabase db = null;
    
    @Override
    public void onCreate() {
        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
    }
    
    public SQLiteDatabase getDatabase() {
        return db;
    }
}
