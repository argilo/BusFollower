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
import java.util.Collection;
import java.util.HashSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class GetRoutesOrTripsResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "GetNextTripFSR";

    private String stopNumber = null;
    private String stopLabel = null;
    private String error = null;
    private ArrayList<RouteDirection> routeDirections = new ArrayList<>();

    public GetRoutesOrTripsResult(XmlPullParser xpp) throws XmlPullParserException, IOException {
        while (xpp.next() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            if ("StopNo".equalsIgnoreCase(tagName)) {
                stopNumber = xpp.nextText();
            } else if ("StopLabel".equalsIgnoreCase(tagName) || "StopDescription".equalsIgnoreCase(tagName)) {
                stopLabel = xpp.nextText();
            } else if ("Error".equalsIgnoreCase(tagName)) {
                error = xpp.nextText();
            } else if ("Route".equalsIgnoreCase(tagName)) {
                while (xpp.next() == XmlPullParser.START_TAG) {
                    xpp.require(XmlPullParser.START_TAG, null, "RouteDirection");
                    routeDirections.add(new RouteDirection(xpp));
                    xpp.require(XmlPullParser.END_TAG, null, "RouteDirection");
                }
            } else if ("Routes".equalsIgnoreCase(tagName)) {
                while (xpp.next() == XmlPullParser.START_TAG) {
                    xpp.require(XmlPullParser.START_TAG, null, "Route");
                    routeDirections.add(new RouteDirection(xpp));
                    xpp.require(XmlPullParser.END_TAG, null, "Route");
                }
            } else {
                Log.w(TAG, "Unrecognized start tag: " + tagName);
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
        }
    }

    public String getStopNumber() {
        return stopNumber;
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

    public Collection<RouteDirection> getFilteredRouteDirections(HashSet<RouteDirection> filter) {
        if (filter.size() == 0) {
            return routeDirections;
        }
        Collection<RouteDirection> result = new ArrayList<>();
        for (RouteDirection rd : routeDirections) {
            if (filter.contains(rd)) {
                result.add(rd);
            }
        }
        return result;
    }
}
