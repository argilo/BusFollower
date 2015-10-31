/*
 * Copyright 2012-2015 Clayton Smith
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

package net.argilo.busfollower.ocdata;

import net.argilo.busfollower.R;
import android.content.Context;
import android.util.Log;

public class Util {
    private static final String TAG = "Util";
    
    public static String getErrorString(Context context, String error) {
        if ("".equals(error)) {
            return null;
        }
        
        try {
            int errorNumber = Integer.parseInt(error);
            switch (errorNumber) {
            case 1:
                return context.getString(R.string.invalid_api_key);
            case 2:
                return context.getString(R.string.unable_to_query_data_source);
            case 10:
            case 11:
                return context.getString(R.string.invalid_stop_number);
            case 12:
                return context.getString(R.string.invalid_route_number);
            default:
                Log.w(TAG, "Unknown error code: " + error);
                return null;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Couldn't parse error code: " + error);
            return null;
        }
    }
}
