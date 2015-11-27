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

import android.provider.BaseColumns;

public final class BusFollowerContract {
    public static abstract class SavedQuery implements BaseColumns {
        public static final String TABLE_NAME = "saved_queries";
        public static final String STOP_NUMBER = "stop_number";
        public static final String TIMES_QUERIED = "times_queried";
        public static final String LAST_QUERIED = "last_queried";
    }

    public static abstract class SavedRouteDirection implements BaseColumns {
        public static final String TABLE_NAME = "saved_route_directions";
        public static final String SAVED_QUERY_ID = "saved_query_id";
        public static final String ROUTE_NUMBER = "route_number";
        public static final String ROUTE_LABEL = "route_label";
        public static final String DIRECTION_ID = "direction_id";
        public static final String DIRECTION = "direction";
    }

}
