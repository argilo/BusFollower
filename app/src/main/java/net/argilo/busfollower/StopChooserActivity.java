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

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.Stop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class StopChooserActivity extends Activity {
    private static final String TAG = "StopChooserActivity";
    
    private SQLiteDatabase db = null;
    private static FetchRoutesTask fetchRoutesTask = null;
    private static FetchTripsTask fetchTripsTask = null;
    private RecentQueryAdapter recentQueryAdapter = null;
    private SimpleCursorAdapter adapter = null;
    private AutoCompleteTextView stopSearchField = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopchooser);

        db = ((BusFollowerApplication) getApplication()).getDatabase();
                
        stopSearchField = (AutoCompleteTextView) findViewById(R.id.stopSearch);
        final Button chooseMapButton = (Button) findViewById(R.id.chooseMap);
        
        if (savedInstanceState != null) {
            // Let the AsyncTasks know we're back.
            if (fetchRoutesTask != null) {
                fetchRoutesTask.setActivityContext(this);
            }
            if (fetchTripsTask != null) {
                fetchTripsTask.setActivityContext(this);
            }
        }

        if (android.os.Build.VERSION.SDK_INT < 11) {
            adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, null,
                    new String[] { "stop_desc" }, new int[] { android.R.id.text1 });
        } else {
            adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, null,
                    new String[] { "stop_desc" }, new int[] { android.R.id.text1 }, 0);
        }
        stopSearchField.setAdapter(adapter);
        
        adapter.setCursorToStringConverter(new CursorToStringConverter() {
            @Override
            public String convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndexOrThrow("stop_code"));
            }
        });
        
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                Log.d(TAG, "Loading cursor in runQuery().");
                if (constraint == null) {
                    return null;
                }
                String constraintStr = constraint.toString();
                String[] pieces = constraintStr.split(" ");

                String query = "SELECT stop_id AS _id, stop_code, stop_code || \"  \" || stop_name AS stop_desc FROM stops WHERE stop_code IS NOT NULL";
                ArrayList<String> params = new ArrayList<>();
                boolean validQuery = false;
                for (String piece : pieces) {
                    if (piece.length() > 0) {
                        validQuery = true;
                        query += " AND (stop_name LIKE ?";
                        params.add("%" + piece + "%");
                        if (piece.matches("\\d\\d\\d?\\d?")) {
                            query += " OR stop_code LIKE ?";
                            params.add(piece + "%");
                        }
                        query += ")";
                    }
                }
                if (!validQuery) {
                    return null;
                }
                query += " ORDER BY total_departures DESC";
                Cursor cursor = db.rawQuery(query, params.toArray(new String[params.size()]));
                if (cursor != null) {
                    cursor.moveToFirst();
                    Log.d(TAG, "Done loading cursor.");
                    return cursor;
                }

                Log.d(TAG, "Cursor was null.");
                return null;
            }
        });
        
        stopSearchField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                        (actionId == EditorInfo.IME_NULL && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
                    fetchRoutesTask.execute(stopSearchField.getText().toString());
                    return true;
                }
                return false;
            }
        });
        
        stopSearchField.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
                fetchRoutesTask.execute(stopSearchField.getText().toString());
            }
        });
        
        chooseMapButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StopChooserActivity.this, MapChooserActivity.class);
                startActivity(intent);
            }
        });

        ListView recentList = (ListView) findViewById(R.id.recentList);
        recentQueryAdapter = new RecentQueryAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<RecentQuery>());
        recentList.setAdapter(recentQueryAdapter);

        recentList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                RecentQuery query = recentQueryAdapter.getItem(position);
                if (query.getRoute() == null) {
                    fetchRoutesTask = new FetchRoutesTask(StopChooserActivity.this, db);
                    fetchRoutesTask.execute(query.getStop().getNumber());
                } else {
                    fetchTripsTask = new FetchTripsTask(StopChooserActivity.this, db);
                    fetchTripsTask.execute(query);
                }
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
        private Context context;
        private int resourceId;
        private ArrayList<RecentQuery> queries;
        
        public RecentQueryAdapter(Context context, int resourceId, ArrayList<RecentQuery> queries) {
            super(context, resourceId, queries);
            this.context = context;
            this.resourceId = resourceId;
            this.queries = queries;
        }
        
        @Override
        public View getView(int position, View v, ViewGroup parent) {
            RecentQuery query = queries.get(position);

            if (v == null) {
                LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(resourceId, null);
            }

            TextView text1 = (TextView) v.findViewById(android.R.id.text1);
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
