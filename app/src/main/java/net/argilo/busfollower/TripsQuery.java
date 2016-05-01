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

import net.argilo.busfollower.ocdata.RouteDirection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashSet;

public class TripsQuery extends Query implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stopNumber = null;
    private HashSet<RouteDirection> routeDirections = null;

    public TripsQuery(String stopNumber, HashSet<RouteDirection> routeDirections) {
        this.stopNumber = stopNumber;
        this.routeDirections = routeDirections;
    }

    public TripsQuery(JSONObject obj) throws JSONException {
        stopNumber = obj.getString("stopNumber");

        JSONArray arr = obj.getJSONArray("routeDirections");
        routeDirections = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            routeDirections.add(new RouteDirection(arr.getJSONObject(i)));
        }
    }

    public String getStopNumber() {
        return stopNumber;
    }

    public HashSet<RouteDirection> getRouteDirections() {
        return routeDirections;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("stopNumber", stopNumber);

        JSONArray arr = new JSONArray();
        for (RouteDirection rd : routeDirections) {
            arr.put(rd.toJSON());
        }
        obj.put("routeDirections", arr);
        return obj;
    }
}
