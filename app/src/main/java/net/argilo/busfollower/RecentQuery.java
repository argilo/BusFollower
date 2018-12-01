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
 * <http://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import java.io.Serializable;
import java.util.Date;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

class RecentQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Stop stop;
    private Route route;
    private int timesQueried = -1;
    private Date lastQueried;

    RecentQuery(Stop stop) {
        this.stop = stop;
    }

    RecentQuery(Stop stop, Route route) {
        this.stop = stop;
        this.route = route;
        this.timesQueried = 1;
        this.lastQueried = new Date();
    }

    public Stop getStop() {
        return stop;
    }

    public Route getRoute() {
        return route;
    }

    public int getTimesQueried() {
        return timesQueried;
    }

    Date getLastQueried() {
        return lastQueried;
    }

    void queriedAgain() {
        timesQueried++;
        lastQueried = new Date();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof RecentQuery) {
            RecentQuery otherRecentQuery = (RecentQuery) other;
            return stop.equals(otherRecentQuery.stop) && route.equals(otherRecentQuery.route);
        } else {
            return false;
        }
    }
}
