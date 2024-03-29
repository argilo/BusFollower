/*
 * Copyright 2012-2022 Clayton Smith
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

import androidx.annotation.NonNull;

public class RouteDirection implements Comparable<RouteDirection>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "RouteDirection";

    private String routeNumber;
    private String routeLabel;
    private String directionID;
    private String direction;
    private String error;
    private String requestProcessingTime;
    private final ArrayList<Trip> trips = new ArrayList<>();

    RouteDirection(XmlPullParser xpp) throws XmlPullParserException, IOException {
        while (xpp.next() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            if ("RouteNo".equalsIgnoreCase(tagName)) {
                routeNumber = xpp.nextText();
            } else if ("RouteLabel".equalsIgnoreCase(tagName) || "RouteHeading".equalsIgnoreCase(tagName)) {
                routeLabel = xpp.nextText();
            } else if ("DirectionID".equalsIgnoreCase(tagName)) {
                directionID = xpp.nextText();
            } else if ("Direction".equalsIgnoreCase(tagName)) {
                direction = xpp.nextText();
            } else if ("Error".equalsIgnoreCase(tagName)) {
                error = xpp.nextText();
            } else if ("RequestProcessingTime".equalsIgnoreCase(tagName)) {
                requestProcessingTime = xpp.nextText();
            } else if ("Trips".equalsIgnoreCase(tagName)) {
                while (xpp.next() == XmlPullParser.START_TAG) {
                    xpp.require(XmlPullParser.START_TAG, null, "Trip");
                    trips.add(new Trip(xpp, this));
                    xpp.require(XmlPullParser.END_TAG, null, "Trip");
                }
            } else {
                Log.w(TAG, "Unrecognized start tag: " + tagName);
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
        }
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    String getDirectionID() {
        return directionID;
    }

    public String getDirection() {
        return direction;
    }

    public String getError() {
        return error;
    }

    Date getRequestProcessingTime() {
        if (requestProcessingTime == null || requestProcessingTime.length() < 14) {
            return null;
        }
        try {
            // Parse the request processing time as current local time in Ottawa.
            int year      = Integer.parseInt(requestProcessingTime.substring(0, 4));
            int month     = Integer.parseInt(requestProcessingTime.substring(4, 6)) - 1;
            int day       = Integer.parseInt(requestProcessingTime.substring(6, 8));
            int hourOfDay = Integer.parseInt(requestProcessingTime.substring(8, 10));
            int minute    = Integer.parseInt(requestProcessingTime.substring(10, 12));
            int second    = Integer.parseInt(requestProcessingTime.substring(12, 14));

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Toronto"));
            calendar.set(year, month, day, hourOfDay, minute, second);
            return calendar.getTime();
        } catch (NumberFormatException e) {
            Log.w(TAG, "Couldn't parse RequestProcessingTime: " + requestProcessingTime);
            return null;
        }
    }

    public ArrayList<Trip> getTrips() {
        return trips;
    }

    public boolean matchesDirection(Route route) {
        if (!routeNumber.equals(route.getNumber())) {
            return false;
        }
        if (direction.length() > 0) {
            return direction.equals(route.getDirection());
        } else {
            return routeLabel.equals(route.getHeading());
        }
    }

    @NonNull
    @Override
    public String toString() {
        String result = "";
        if (routeNumber != null) {
            result += routeNumber;
        }
        if (routeLabel != null) {
            result += "  " + routeLabel;
        }
        return result;
    }

    @Override
    public int compareTo(RouteDirection other) {
        return this.sortOrder() - other.sortOrder();
    }

    private int sortOrder() {
        int order = -1;

        try {
            if (routeNumber.startsWith("R")) {
                order = Integer.parseInt(routeNumber.substring(1)) * 10 + 5;
            } else {
                order = Integer.parseInt(routeNumber) * 10;
            }
            order += Integer.parseInt(directionID);

        } catch (NumberFormatException e) {
            // Ignore
        }

        return order;
    }
}
