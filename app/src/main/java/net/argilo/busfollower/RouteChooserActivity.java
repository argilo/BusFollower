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

import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
        GetRouteSummaryForStopResult result = (GetRouteSummaryForStopResult) getIntent().getSerializableExtra("result");
        routeDirections = result.getRouteDirections();

        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, routeDirections));
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
        // Here we just use RecentQuery as a convenience, since it can hold a stop and route.
        task = new FetchTripsTask(this, db);
        task.execute(new RecentQuery(stop, new Route(routeDirections.get(position))));
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
