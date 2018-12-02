/*
 * Copyright 2012-2018 Clayton Smith
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
 * <https://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower.ocdata;

import java.io.Serializable;
import java.util.ArrayList;

import net.argilo.busfollower.R;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;

public class Stop implements Serializable {
    private static final long serialVersionUID = 1L;

    private String number;
    private String name;
    private double latitude;
    private double longitude;

    public Stop(Context context, SQLiteDatabase db, String number) throws IllegalArgumentException {
        // Zero-pad the stop number to 4 digits.
        while (number.length() < 4) {
            number = "0" + number;
        }

        Cursor result = db.rawQuery("SELECT stop_name, stop_lat, stop_lon FROM stops WHERE stop_code = ?", new String[] { number });
        if (result.getCount() == 0) {
            throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
        }

        result.moveToFirst();
        if (result.getCount() == 1) {
            name = result.getString(0);
        } else {
            // There are multiple names for this stop, so try to pick out
            // what's common to all of them.
            ArrayList<String> namesSoFar = new ArrayList<>();
            while (!result.isAfterLast()) {
                String currentName = result.getString(0);
                currentName = currentName.replaceAll("  ", " ");
                currentName = currentName.replaceAll(" ?[12345][ABCD]", "");
                currentName = currentName.replaceAll(" [NESW]\\.", "");
                if (namesSoFar.contains(currentName)) {
                    // If we're seeing the same name for a second time, use that.
                    name = currentName;
                    break;
                }
                namesSoFar.add(currentName);
                result.moveToNext();
            }
            // We couldn't find a common name. As a last resort, use the first name
            // from the database.
            if (name == null) {
                result.moveToFirst();
                name = result.getString(0);
            }
        }

        // Average out the stop locations in case there are multiple entries
        // (e.g. different platforms at a Transitway station)
        result.moveToFirst();
        double avgLatitude = 0;
        double avgLongitude = 0;
        while (!result.isAfterLast()) {
            avgLatitude += result.getDouble(1);
            avgLongitude += result.getDouble(2);
            result.moveToNext();
        }
        latitude = avgLatitude / result.getCount();
        longitude = avgLongitude / result.getCount();
        result.close();

        this.number = number;
    }

    public Stop(String number, String name, double latitude, double longitude) {
        this.number = number;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof Stop) {
            Stop otherStop = (Stop) other;
            if (number == null) {
                return name.equals(otherStop.name);
            } else {
                return number.equals(otherStop.number);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (number == null) ? name.hashCode() : number.hashCode();
    }
}
