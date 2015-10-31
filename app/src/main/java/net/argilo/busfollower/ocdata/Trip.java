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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

public class Trip implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "Trip";
    
    private String destination = null;
    private String startTime = null;
    private String adjustedScheduleTime = null;
    private float adjustmentAge = Float.NaN;
    private boolean lastTripOfSchedule = false;
    private BusType busType = new BusType("");
    private float gpsSpeed = Float.NaN;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    
    // Needed to get the request processing time.
    private RouteDirection routeDirection;
    
    public Trip(XmlPullParser xpp, RouteDirection routeDirection) throws XmlPullParserException, IOException {
        this.routeDirection = routeDirection;
        
        while (xpp.next() == XmlPullParser.START_TAG) {
            String tagName = xpp.getName();
            if ("TripDestination".equalsIgnoreCase(tagName)) {
                destination = xpp.nextText();
            } else if ("TripStartTime".equalsIgnoreCase(tagName)) {
                startTime = xpp.nextText();
            } else if ("AdjustedScheduleTime".equalsIgnoreCase(tagName)) {
                adjustedScheduleTime = xpp.nextText();
            } else if ("AdjustmentAge".equalsIgnoreCase(tagName)) {
                try {
                    adjustmentAge = Float.parseFloat(xpp.nextText());
                } catch (Exception e) {
                    Log.w(TAG, "Couldn't parse AdjustmentAge.");
                }
            } else if ("LastTripOfSchedule".equalsIgnoreCase(tagName)) {
                lastTripOfSchedule = "true".equalsIgnoreCase(xpp.nextText());
            } else if ("BusType".equalsIgnoreCase(tagName)) {
                busType = new BusType(xpp.nextText());
            } else if ("GPSSpeed".equalsIgnoreCase(tagName)) {
                try {
                    gpsSpeed = Float.parseFloat(xpp.nextText());
                } catch (Exception e) {
                    // Ignore.
                }
            } else if ("Latitude".equalsIgnoreCase(tagName)) {
                try {
                    latitude = Double.parseDouble(xpp.nextText());
                } catch (NumberFormatException e) {
                    // Ignore.
                }
            } else if ("Longitude".equalsIgnoreCase(tagName)) {
                try {
                    longitude = Double.parseDouble(xpp.nextText());
                } catch (NumberFormatException e) {
                    // Ignore.
                }
            } else {
                Log.w(TAG, "Unrecognized start tag: " + tagName);
            }
            xpp.require(XmlPullParser.END_TAG, null, tagName);
        }
    }
    
    public String getDestination() {
        return destination;
    }
    
    public Date getStartTime() {
        // Start time is measured from "noon minus 12h" (effectively midnight, except for days
        // on which daylight savings time changes occur) at the beginning of the service date.

        // Start with the request processing time (or the current time, if it's unavailable),
        // which should be within a few hours of the start time.
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Toronto"));
        if (routeDirection.getRequestProcessingTime() != null) {
            calendar.setTime(routeDirection.getRequestProcessingTime());
        }
        
        int colonIndex = startTime.indexOf(":");
        int hours = Integer.parseInt(startTime.substring(0, colonIndex));
        int minutes = Integer.parseInt(startTime.substring(colonIndex + 1));
        
        // Subtracting the start time should put us within a few hours of the beginning of
        // the service date.
        calendar.add(Calendar.HOUR, -hours);
        calendar.add(Calendar.MINUTE, -minutes);
        
        // Now scan forward until we get to noon.
        while (calendar.get(Calendar.HOUR_OF_DAY) != 12) {
            calendar.add(Calendar.HOUR, 1);
        }
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // Subtract twelve hours.
        calendar.add(Calendar.HOUR, -12);
        
        // Add in the start time.
        calendar.add(Calendar.HOUR, hours);
        calendar.add(Calendar.MINUTE, minutes);
        
        return calendar.getTime();
    }
    
    public Date getAdjustedScheduleTime() {
        try {
            Calendar calendar = Calendar.getInstance();
            if (routeDirection.getRequestProcessingTime() != null) {
                calendar.setTime(routeDirection.getRequestProcessingTime());
            }
            calendar.add(Calendar.MINUTE, Integer.parseInt(adjustedScheduleTime));
            return calendar.getTime();
        } catch (NumberFormatException e) {
            Log.w(TAG, "Couldn't parse AdjustedScheduleTime: " + adjustedScheduleTime);
            return null;
        }
    }
    
    public float getAdjustmentAge() {
        return adjustmentAge;
    }
    
    public boolean isEstimated() {
        return (adjustmentAge >= 0);
    }
    
    public boolean isLastTrip() {
        return lastTripOfSchedule;
    }
    
    public BusType getBusType() {
        return busType;
    }
    
    public float getGpsSpeed() {
        return gpsSpeed;
    }
    
    public LatLng getLocation() {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return null;
        } else {
            return new LatLng(latitude, longitude);
        }
    }
    
    public RouteDirection getRouteDirection() {
        return routeDirection;
    }
}
