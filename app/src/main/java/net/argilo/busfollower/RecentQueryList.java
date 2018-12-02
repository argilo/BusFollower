/*
 * Copyright 2012-2017 Clayton Smith
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
 * <https://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

import android.content.Context;

class RecentQueryList {
    private static final String FILENAME = "recent_queries";
    private static final int MAX_RECENT_QUERIES = 10;

    @SuppressWarnings("unchecked")
    static synchronized ArrayList<RecentQuery> loadRecents(Context context) {
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

    static synchronized void addOrUpdateRecent(Context context, Stop stop, Route route) {
        ArrayList<RecentQuery> recents = loadRecents(context);

        RecentQuery query = new RecentQuery(stop, route);

        boolean foundQuery = false;
        for (RecentQuery recent : recents) {
            if (recent.equals(query)) {
                foundQuery = true;
                recent.queriedAgain();
                break;
            }
        }

        if (!foundQuery) {
            recents.add(query);
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
