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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SavedTripsQuery {
    private TripsQuery tripsQuery;
    private int timesQueried = -1;
    private Date lastQueried = null;

    public SavedTripsQuery(TripsQuery tripsQuery, int timesQueried, Date lastQueried) {
        this.tripsQuery = tripsQuery;
        this.timesQueried = timesQueried;
        this.lastQueried = lastQueried;
    }

    public SavedTripsQuery(TripsQuery tripsQuery) {
        this(tripsQuery, 1, new Date());
    }

    public SavedTripsQuery(JSONObject obj) throws JSONException {
        this(new TripsQuery(obj.getJSONObject("tripsQuery")),
                obj.getInt("timesQueried"), new Date(obj.getLong("lastQueried")));
    }

    public TripsQuery getTripsQuery() {
        return tripsQuery;
    }

    public int getTimesQueried() {
        return timesQueried;
    }

    public Date getLastQueried() {
        return lastQueried;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("tripsQuery", tripsQuery.toJSON());
        obj.put("timesQueried", timesQueried);
        obj.put("lastQueried", lastQueried.getTime());
        return obj;
    }
}
