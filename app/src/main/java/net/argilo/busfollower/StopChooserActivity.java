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
 * <https://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.Stop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class StopChooserActivity extends Activity {
    private static final String TAG = "StopChooserActivity";

    private SQLiteDatabase db;
    private static FetchRoutesTask fetchRoutesTask;
    private static FetchTripsTask fetchTripsTask;
    private RecentQueryAdapter recentQueryAdapter;
    private AutoCompleteTextView stopSearchField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Util.useAndroidTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopchooser);

        db = ((BusFollowerApplication) getApplication()).getDatabase();

        stopSearchField = findViewById(R.id.stopSearch);
        final Button chooseMapButton = findViewById(R.id.chooseMap);

        if (savedInstanceState != null) {
            // Let the AsyncTasks know we're back.
            if (fetchRoutesTask != null) {
                fetchRoutesTask.setActivityContext(this);
            }
            if (fetchTripsTask != null) {
                fetchTripsTask.setActivityContext(this);
            }
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, null,
                new String[] { "stop_desc" }, new int[] { android.R.id.text1 }, 0);
        stopSearchField.setAdapter(adapter);

        adapter.setCursorToStringConverter(cursor -> cursor.getString(cursor.getColumnIndexOrThrow("stop_code")));

        adapter.setFilterQueryProvider(constraint -> {
            Log.d(TAG, "Loading cursor in runQuery().");
            if (constraint == null) {
                return null;
            }
            String constraintStr = constraint.toString();
            String[] pieces = constraintStr.split(" ");

            StringBuilder query = new StringBuilder("SELECT stop_id AS _id, stop_code, stop_code || \"  \" || stop_name AS stop_desc FROM stops WHERE stop_code IS NOT NULL");
            ArrayList<String> params = new ArrayList<>();
            boolean validQuery = false;
            for (String piece : pieces) {
                if (piece.length() > 0) {
                    validQuery = true;
                    query.append(" AND (stop_name LIKE ?");
                    params.add("%" + piece + "%");
                    if (piece.matches("\\d\\d\\d?\\d?")) {
                        query.append(" OR stop_code LIKE ?");
                        params.add(piece + "%");
                    }
                    query.append(")");
                }
            }
            if (!validQuery) {
                return null;
            }
            query.append(" ORDER BY total_departures DESC");
            Cursor cursor = db.rawQuery(query.toString(), params.toArray(new String[0]));
            if (cursor != null) {
                cursor.moveToFirst();
                Log.d(TAG, "Done loading cursor.");
                return cursor;
            }

            Log.d(TAG, "Cursor was null.");
            return null;
        });

        stopSearchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (actionId == EditorInfo.IME_NULL && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
                fetchRoutesTask.execute(stopSearchField.getText().toString());
                return true;
            }
            return false;
        });

        stopSearchField.setOnItemClickListener((parent, view, position, id) -> {
            fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
            fetchRoutesTask.execute(stopSearchField.getText().toString());
        });

        chooseMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(StopChooserActivity.this, MapChooserActivity.class);
            startActivity(intent);
        });

        ListView recentList = findViewById(R.id.recentList);
        recentQueryAdapter = new RecentQueryAdapter(this, new ArrayList<>());
        recentList.setAdapter(recentQueryAdapter);

        recentList.setOnItemClickListener((parent, v, position, id) -> {
            RecentQuery query = recentQueryAdapter.getItem(position);
            if (query == null) {
                return;
            }
            if (query.getRoute() == null) {
                fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
                fetchRoutesTask.execute(query.getStop().getNumber());
            } else {
                fetchTripsTask = new FetchTripsTask(StopChooserActivity.this);
                fetchTripsTask.execute(query);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        recentQueryAdapter.clear();

        ArrayList<RecentQuery> recentQueryList = RecentQueryList.loadRecents(this);
        Stop lastStop = null;
        for (RecentQuery recentQuery : recentQueryList) {
            Stop thisStop = recentQuery.getStop();
            if (!thisStop.equals(lastStop)) {
                // Add a stop heading that will group all routes departing from this stop.
                recentQueryAdapter.add(new RecentQuery(thisStop));
                lastStop = thisStop;
            }
            recentQueryAdapter.add(recentQuery);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Let the AsyncTasks know we're gone.
        if (fetchRoutesTask != null) {
            fetchRoutesTask.setActivityContext(null);
        }
        if (fetchTripsTask != null) {
            fetchTripsTask.setActivityContext(null);
        }
    }

    private class RecentQueryAdapter extends ArrayAdapter<RecentQuery> {
        private final Context context;
        private final ArrayList<RecentQuery> queries;

        RecentQueryAdapter(Context context, ArrayList<RecentQuery> queries) {
            super(context, android.R.layout.simple_list_item_1, queries);
            this.context = context;
            this.queries = queries;
        }

        @NonNull
        @Override
        public View getView(int position, View v, @NonNull ViewGroup parent) {
            RecentQuery query = queries.get(position);

            if (v == null) {
                LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(android.R.layout.simple_list_item_1, null);
            }

            TextView text1 = v.findViewById(android.R.id.text1);
            text1.setSingleLine();
            text1.setEllipsize(TextUtils.TruncateAt.END);
            if (query.getRoute() == null) {
                // Stop number heading
                text1.setTypeface(null, Typeface.BOLD);
                text1.setText(context.getString(R.string.stop_number) + " " +
                        query.getStop().getNumber() + " " + query.getStop().getName());
            } else {
                // Route number
                text1.setTypeface(null, Typeface.NORMAL);
                text1.setText(" \u00BB " + context.getString(R.string.route_number) + " " +
                        query.getRoute().getNumber() + " " + query.getRoute().getHeading());
            }

            return v;
        }
    }
}
