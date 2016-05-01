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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class RecentQueryList {
    private static final String FILENAME_JSON = "recent_queries.json";
    private static final String FILENAME_LEGACY = "recent_queries";
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
            ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(FILENAME_LEGACY, Context.MODE_PRIVATE));
            out.writeObject(recents);
            out.close();
        } catch (IOException e) {
            // No big deal if the recent queries didn't get saved.
        }
    }

    private static synchronized ArrayList<SavedTripsQuery> loadRecents(Context context) {
        ArrayList<RecentQuery> legacyRecents = null;

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(context.openFileInput(FILENAME_LEGACY));
            legacyRecents = (ArrayList<RecentQuery>) in.readObject();
        } catch (Exception e) {
            // Carry on if the legacy file is unreadable or missing.
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
            context.deleteFile(FILENAME_LEGACY);
        }

        ArrayList<SavedTripsQuery> result = new ArrayList<>();
        if (legacyRecents != null) {
            for (RecentQuery legacyQuery : legacyRecents) {
                HashSet<RouteDirection> routeDirections = new HashSet<>();
                if (legacyQuery.getRoute() != null) {
                    routeDirections.add(new RouteDirection(legacyQuery.getRoute()));
                }
                TripsQuery tripsQuery = new TripsQuery(legacyQuery.getStop().getNumber(), routeDirections);
                result.add(new SavedTripsQuery(tripsQuery, legacyQuery.getTimesQueried(), legacyQuery.getLastQueried()));
            }
            saveRecents(context, result);
        } else {
            // TODO: Read JSON if possible
            FileInputStream fin = null;
            String jsonString = null;
            try {
                fin = context.openFileInput(FILENAME_LEGACY);
                byte[] buffer = new byte[fin.available()];
                fin.read(buffer);
                jsonString = new String(buffer, "UTF-8");
            } catch (Exception e) {
                // Carry on if we couldn't read the JSON file.
            } finally {
                if (fin != null) {
                    try { fin.close(); } catch (IOException e) {}
                }
            }

            if (jsonString != null) {
                try {
                    JSONObject json = new JSONObject(jsonString);
                    int version = json.getInt("version");
                    if (version == 1) {
                        JSONArray savedTripsJson = json.getJSONArray("savedTrips");
                        for (int i = 0; i < savedTripsJson.length(); i++) {
                            result.add(new SavedTripsQuery(savedTripsJson.getJSONObject(i)));
                        }
                    }
                } catch (JSONException e) {
                    // TODO: Handle?
                }
            }
        }

        return result;
    }

    private static synchronized void saveRecents(Context context, ArrayList<SavedTripsQuery> queries) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", 1);

        JSONArray arr = new JSONArray();
        for (SavedTripsQuery query : queries) {
            arr.put(query.toJSON());
        }
        json.put("savedTrips", arr);
    }

    private static class QueryDateComparator implements Comparator<SavedTripsQuery> {
        @Override
        public int compare(SavedTripsQuery lhs, SavedTripsQuery rhs) {
            return lhs.getLastQueried().compareTo(rhs.getLastQueried());
        }
    }

    private static class QueryStopRouteComparator implements Comparator<SavedTripsQuery> {
        @Override
        public int compare(SavedTripsQuery lhs, SavedTripsQuery rhs) {
            int lhsStopNumber = Integer.parseInt(lhs.getTripsQuery().getStopNumber());
            int rhsStopNumber = Integer.parseInt(rhs.getTripsQuery().getStopNumber());
            int lhsRouteNumber = routeDirectionsSortNumber(lhs.getTripsQuery().getRouteDirections());
            int rhsRouteNumber = routeDirectionsSortNumber(rhs.getTripsQuery().getRouteDirections());
            return (lhsStopNumber - rhsStopNumber) * 10000 + (lhsRouteNumber - rhsRouteNumber);
        }
    }

    private static int routeDirectionsSortNumber(HashSet<RouteDirection> routeDirections) {
        if (routeDirections.isEmpty()) { // All routes first
            return 0;
        } else if (routeDirections.size() > 1) { // Then multiple routes
            return Integer.parseInt(routeDirections.iterator().next().getRouteNumber());
        } else { // Finally single routes
            return Integer.parseInt(routeDirections.iterator().next().getRouteNumber()) + 1000;
        }
    }
}
