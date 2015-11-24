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
import java.util.HashSet;

import net.argilo.busfollower.ocdata.GetRoutesOrTripsResult;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class RouteChooserActivity extends ListActivity {
    private SQLiteDatabase db = null;
    private static FetchTripsTask task = null;

    private Stop stop;
    private ArrayList<RouteDirection> routeDirections;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Util.useAndroidTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routechooser);

        db = ((BusFollowerApplication) getApplication()).getDatabase();

        Util.setDisplayHomeAsUpEnabled(this, true);

        stop = (Stop) getIntent().getSerializableExtra("stop");
        GetRoutesOrTripsResult result = (GetRoutesOrTripsResult) getIntent().getSerializableExtra("result");
        routeDirections = result.getRouteDirections();

        if (routeDirections.size() == 1) {
            setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, routeDirections));
        } else {
            setListAdapter(new BaseAdapter() {
                private LayoutInflater layoutInflater = LayoutInflater.from(RouteChooserActivity.this);

                @Override
                public int getCount() {
                    return routeDirections.size() + 1;
                }

                @Override
                public Object getItem(int position) {
                    if (position == 0) {
                        return null;
                    } else {
                        return routeDirections.get(position - 1);
                    }
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView text = (TextView) layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                    if (position == 0) {
                        text.setText("All routes"); // TODO: Make resource
                    } else {
                        text.setText(getItem(position).toString());
                    }
                    return text;
                }
            });
        }
        setTitle(getString(R.string.stop_number) + " " + stop.getNumber() +
                (stop.getName() != null ? " " + stop.getName() : ""));

        if (savedInstanceState != null) {
            if (task != null) {
                // Let the AsyncTask know we're back.
                task.setActivityContext(this);
            }
        }
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        task = new FetchTripsTask(this, db);
        HashSet<RouteDirection> queryRouteDirections = new HashSet<>();
        if (position > 0) {
            queryRouteDirections.add(routeDirections.get(position - 1));
        }
        task.execute(new TripsQuery(stop.getNumber(), queryRouteDirections));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (task != null) {
            // Let the AsyncTask know we're gone.
            task.setActivityContext(null);
        }
    }
}
