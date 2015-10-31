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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class Route implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "Route";
    
    private String number = null;
    private String directionID = null;
    private String direction = null;
    private String heading = null;
    
    public Route(XmlPullParser xpp) throws XmlPullParserException, IOException {
        while (xpp.next() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            if ("RouteNo".equalsIgnoreCase(tagName)) {
                number = xpp.nextText();
            } else if ("DirectionID".equalsIgnoreCase(tagName)) {
                directionID = xpp.nextText();
            } else if ("Direction".equalsIgnoreCase(tagName)) {
                direction = xpp.nextText();
            } else if ("RouteHeading".equalsIgnoreCase(tagName)) {
                heading = xpp.nextText();
            } else {
                Log.w(TAG, "Unrecognized start tag: " + tagName);
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
        }
    }
    
    public String getNumber() {
        return number;
    }
    
    public String getDirectionID() {
        return directionID;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public String getHeading() {
        return heading;
    }
    
    @Override
    public String toString() {
        String result = "";
        if (number != null) {
            result += number;
        }
        if (heading != null) {
            result += "  " + heading;
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof Route) {
            Route otherRoute = (Route) other;
            return number.equals(otherRoute.number) && direction.equals(otherRoute.direction);
        } else {
            return false;
        }
    }
}
