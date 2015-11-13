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

import java.io.Serializable;

public class Route implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "Route";

    private String number = null;
    private String directionID = null;
    private String direction = null;
    private String heading = null;

    public Route(RouteDirection routeDirection) {
        this.number = routeDirection.getRouteNumber();
        this.directionID = routeDirection.getDirectionID();
        this.direction = routeDirection.getDirection();
        this.heading = routeDirection.getRouteLabel();
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
