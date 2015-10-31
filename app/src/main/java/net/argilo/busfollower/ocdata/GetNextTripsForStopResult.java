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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GetNextTripsForStopResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "GetNextTripFSR";

    private Stop stop = null;
    private String stopLabel = null;
    private String error = null;
    private ArrayList<RouteDirection> routeDirections = new ArrayList<>();
    
    public GetNextTripsForStopResult(Context context, SQLiteDatabase db, XmlPullParser xpp, String stopNumber)
            throws XmlPullParserException, IOException, IllegalArgumentException {
        stop = new Stop(context, db, stopNumber);
        
        while (xpp.next() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            if ("StopNo".equalsIgnoreCase(tagName)) {
                // We already know the stop number, so discard the result.
                xpp.nextText();
            } else if ("StopLabel".equalsIgnoreCase(tagName)) {
                stopLabel = xpp.nextText();
            } else if ("Error".equalsIgnoreCase(tagName)) {
                error = xpp.nextText();
            } else if ("Route".equalsIgnoreCase(tagName)) {
                while (xpp.next() == XmlPullParser.START_TAG) {
                    xpp.require(XmlPullParser.START_TAG, null, "RouteDirection");
                    routeDirections.add(new RouteDirection(xpp));
                    xpp.require(XmlPullParser.END_TAG, null, "RouteDirection");
                }
            } else {
                Log.w(TAG, "Unrecognized start tag: " + tagName);
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
        }
    }
    
    public Stop getStop() {
        return stop;
    }
    
    public String getStopLabel() {
        return stopLabel;
    }

    public String getError() {
        return error;
    }
    
    public ArrayList<RouteDirection> getRouteDirections() {
        return routeDirections;
    }
}
