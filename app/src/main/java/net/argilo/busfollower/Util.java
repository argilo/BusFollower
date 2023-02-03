/*
 * Copyright 2012-2021 Clayton Smith
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

package net.argilo.busfollower;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import net.argilo.busfollower.ocdata.BusType;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

class Util {
    static void useAndroidTheme(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            context.setTheme(android.R.style.Theme_Material);
        } else {
            context.setTheme(android.R.style.Theme_Holo);
        }
    }

    static void setDisplayHomeAsUpEnabled(Activity activity, boolean bool) {
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(bool);
        }
    }

    static String getBusInformationString(Context context, RouteDirection rd, Trip trip) {
        return context.getString(R.string.destination) + " " + trip.getDestination() +
                "\n" + context.getString(R.string.start_time) + " " + getHumanReadableTime(context, trip.getStartTime()) +
                "\n" + context.getString(trip.isEstimated() ? R.string.estimated_arrival : R.string.scheduled_arrival) +
                " " + getHumanReadableTime(context, trip.getAdjustedScheduleTime()) +
                (trip.getBusType().isPresent() ? "\n" + context.getString(R.string.bus_type) + " " + getBusTypeString(context, trip.getBusType()) : "") +
                (trip.isLastTrip() ? "\n" + context.getString(R.string.last_trip) : "");
    }

    private static String getBusTypeString(Context context, BusType busType) {
        ArrayList<String> pieces = new ArrayList<>();

        switch (busType.getLength()) {
        case 40:
            pieces.add(context.getString(R.string.length_40));
            break;
        case 60:
            pieces.add(context.getString(R.string.length_60));
            break;
        }

        if (busType.hasBikeRack()) {
            pieces.add(context.getString(R.string.bike_rack));
        }

        if (busType.isDoubleDecker()) {
            pieces.add(context.getString(R.string.double_decker));
        }

        if (busType.isHybrid()) {
            pieces.add(context.getString(R.string.hybrid));
        }

        return TextUtils.join(", ", pieces);
    }

    private static String getHumanReadableTime(Context context, Date date) {
        StringBuilder result = new StringBuilder();
        DateFormat formatter = android.text.format.DateFormat.getTimeFormat(context);
        formatter.setTimeZone(TimeZone.getTimeZone("America/Toronto"));
        result.append(formatter.format(date));
        result.append(" (");

        // Relative time
        long difference = date.getTime() - Calendar.getInstance().getTimeInMillis();
        if (difference >= 0) {
            int differenceMinutes = (int) ((difference + 30000) / 60000);
            result.append(context.getResources().getQuantityString(R.plurals.minutes, differenceMinutes, differenceMinutes));
        } else {
            int differenceMinutes = (int) ((-difference + 30000) / 60000);
            result.append(context.getResources().getQuantityString(R.plurals.minutesAgo, differenceMinutes, differenceMinutes));
        }

        result.append(")");
        return result.toString();
    }
}
