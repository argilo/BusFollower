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

package net.argilo.busfollower;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

class RecentQueryList {
    private static final String FILENAME = "recent_queries";
    private static final int MAX_RECENT_QUERIES = 10;

    public static synchronized ArrayList<TripsQuery> loadRecentQueries(Context context) {
        ArrayList<TripsQuery> result = new ArrayList<>();

        for (RecentQuery recentQuery : loadRecents(context)) {
            HashSet<RouteDirection> routeDirections = new HashSet<>();
            if (recentQuery.getRoute() != null) {
                routeDirections.add(new RouteDirection(recentQuery.getRoute()));
            }
            result.add(new TripsQuery(recentQuery.getStop().getNumber(), routeDirections));
        }

        return result;
    }

    public static synchronized void addOrUpdateRecent(Context context, SQLiteDatabase db, TripsQuery tripsQuery) {
        ArrayList<RecentQuery> recents = loadRecents(context);

        Stop stop = new Stop(context, db, tripsQuery.getStopNumber());
        Route route = null;
        if (tripsQuery.getRouteDirections().size() == 1) {
            route = new Route(tripsQuery.getRouteDirections().iterator().next());
        }
        RecentQuery recentQuery = new RecentQuery(stop, route);

        boolean foundQuery = false;
        for (RecentQuery recent : recents) {
            if (recent.equals(recentQuery)) {
                foundQuery = true;
                recent.queriedAgain();
                break;
            }
        }

        if (!foundQuery) {
            recents.add(recentQuery);
            if (recents.size() > MAX_RECENT_QUERIES) {
                // Boot the least recently used query.
                Collections.sort(recents, new QueryDateComparator());
                recents.remove(0);
            }
            // Sort by stop and route number for use.
            Collections.sort(recents, new QueryStopRouteComparator());
        }

        try {
            ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(FILENAME, Context.MODE_PRIVATE));
            out.writeObject(recents);
            out.close();
        } catch (IOException e) {
            // No big deal if the recent queries didn't get saved.
        }
    }

    @SuppressWarnings("unchecked")
    private static synchronized ArrayList<RecentQuery> loadRecents(Context context) {
        ArrayList<RecentQuery> recents;

        try {
            ObjectInputStream in = new ObjectInputStream(context.openFileInput(FILENAME));
            recents = (ArrayList<RecentQuery>) in.readObject();
            in.close();
        } catch (Exception e) {
            // Start a new recent list.
            recents = new ArrayList<>();
        }

        return recents;
    }

    private static class QueryDateComparator implements Comparator<RecentQuery> {
        @Override
        public int compare(RecentQuery lhs, RecentQuery rhs) {
            return lhs.getLastQueried().compareTo(rhs.getLastQueried());
        }
    }

    private static class QueryStopRouteComparator implements Comparator<RecentQuery> {
        @Override
        public int compare(RecentQuery lhs, RecentQuery rhs) {
            int lhsStopNumber = Integer.parseInt(lhs.getStop().getNumber());
            int rhsStopNumber = Integer.parseInt(rhs.getStop().getNumber());
            int lhsRouteNumber = Integer.parseInt(lhs.getRoute().getNumber());
            int rhsRouteNumber = Integer.parseInt(rhs.getRoute().getNumber());
            return (lhsStopNumber - rhsStopNumber) * 10000 + (lhsRouteNumber - rhsRouteNumber);
        }
    }
}
