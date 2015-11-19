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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import net.argilo.busfollower.ocdata.GetRoutesOrTripsResult;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Trip;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class BusFollowerActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "BusFollowerActivity";
    private static final double MIN_LAT_SPAN = 0.01;
    private static final double MIN_LON_SPAN = 0.01;
    private boolean zoomAndCenter = true;

    private SQLiteDatabase db;
    private static FetchTripsTask task;
    private TripsQuery query = null;
    private GetRoutesOrTripsResult result = null;

    private GoogleMap map = null;
    private int padding = 0;
    private ListView tripList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Util.useAndroidTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.busfollower);

        db = ((BusFollowerApplication) getApplication()).getDatabase();

        Util.setDisplayHomeAsUpEnabled(this, true);

        tripList = (ListView) findViewById(R.id.tripList);
        tripList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Trip trip = (Trip) tripList.getAdapter().getItem(position);
                RouteDirection rd = trip.getRouteDirection();
                AlertDialog.Builder dialog = new AlertDialog.Builder(BusFollowerActivity.this);
                dialog.setTitle(rd.getRouteNumber() + " " + rd.getRouteLabel());
                dialog.setMessage(Util.getBusInformationString(BusFollowerActivity.this, rd, trip));
                dialog.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                dialog.show();
            }
        });

        query = (TripsQuery) getIntent().getSerializableExtra("query");
        result = (GetRoutesOrTripsResult) getIntent().getSerializableExtra("result");
        if (savedInstanceState != null) {
            if (task != null) {
                // Let the AsyncTask know we're back.
                task.setActivityContext(this);
            }
            query = (TripsQuery) savedInstanceState.getSerializable("query");
            result = (GetRoutesOrTripsResult) savedInstanceState.getSerializable("result");

            if (result != null) {
                // A configuration change has occurred. Don't reset zoom & center.
                zoomAndCenter = false;
            } else {
                // Zoom to OC Transpo service area if it's our first time.
                zoomAndCenter = true;
            }
        } else {
            if (query.getRouteDirections().size() == 1) {
                RecentQueryList.addOrUpdateRecent(this, db, result.getStopNumber(), query);
            }
            // We're arriving from another activity, so set zoom & center.
            zoomAndCenter = true;
        }

        if (query.getRouteDirections().size() == 1) {
            RouteDirection rd = query.getRouteDirections().iterator().next();
            setTitle(getString(R.string.stop_number) + " " + result.getStopNumber() +
                    ", " + getString(R.string.route_number) + " " + rd.getRouteNumber() + " " + rd.getRouteLabel());
        } else {
            setTitle(getString(R.string.stop_number) + " " + result.getStopNumber() +
                    ", all routes"); // TODO: make string
        }

        padding = ContextCompat.getDrawable(this, R.drawable.pin_red).getIntrinsicHeight();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ((TripAdapter) tripList.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (task != null) {
            // Let the AsyncTask know we're gone.
            task.setActivityContext(null);
        }
        outState.putSerializable("query", query);
        outState.putSerializable("result", result);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.busfollower_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_refresh:
                task = new FetchTripsTask(this, db);
                task.execute(query);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void displayGetNextTripsForStopResult() {
        double minLatitude = Double.MAX_VALUE;
        double maxLatitude = Double.MIN_VALUE;
        double minLongitude = Double.MAX_VALUE;
        double maxLongitude = Double.MIN_VALUE;

        map.clear();

        Stop stop = new Stop(this, db, result.getStopNumber());
        LatLng stopLocation = stop.getLocation();
        if (stopLocation != null) {
            map.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop))
                    .anchor(0.0f, 1.0f)
                    .position(stopLocation)
                    .title(stop.getNumber() == null ? stop.getName() : getString(R.string.stop_number) + " " + stop.getNumber())
                    .snippet(stop.getNumber() == null ? getString(R.string.no_departures) : stop.getName())
            );
            minLatitude = maxLatitude = stopLocation.latitude;
            minLongitude = maxLongitude = stopLocation.longitude;
        }

        ArrayList<Trip> trips = new ArrayList<>();
        for (RouteDirection rd : result.getFilteredRouteDirections(query.getRouteDirections())) {
            trips.addAll(rd.getTrips());
        }
        Collections.sort(trips, new Comparator<Trip>() {
            @Override
            public int compare(Trip lhs, Trip rhs) {
                return lhs.getAdjustedScheduleTime().compareTo(rhs.getAdjustedScheduleTime());
            }
        });
        tripList.setAdapter(new TripAdapter(BusFollowerActivity.this, R.layout.tripitem, trips));

        int number = 0;
        for (Trip trip : trips) {
            number++;
            LatLng point = trip.getLocation();
            if (point != null) {
                minLatitude = Math.min(minLatitude, point.latitude);
                maxLatitude = Math.max(maxLatitude, point.latitude);
                minLongitude = Math.min(minLongitude, point.longitude);
                maxLongitude = Math.max(maxLongitude, point.longitude);

                map.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(getLabeledPin(trip.getRouteDirection().getRouteNumber())))
                                .anchor(0.5f, 1.0f)
                                .position(point)
                                .title(trip.getRouteDirection().getRouteNumber() + " " + trip.getRouteDirection().getRouteLabel())
                                .snippet(trip.getDestination())
                );
            }
        }

        if (maxLatitude - minLatitude < MIN_LAT_SPAN) {
            double middle = (minLatitude + maxLatitude) / 2;
            minLatitude = middle - MIN_LAT_SPAN / 2;
            maxLatitude = middle + MIN_LAT_SPAN / 2;
        }
        if (maxLongitude - minLongitude < MIN_LON_SPAN) {
            double middle = (minLongitude + maxLongitude) / 2;
            minLongitude = middle - MIN_LON_SPAN / 2;
            maxLongitude = middle + MIN_LON_SPAN / 2;
        }

        if (zoomAndCenter) {
            LatLngBounds bounds = new LatLngBounds(new LatLng (minLatitude, minLongitude), new LatLng (maxLatitude, maxLongitude));
            final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            final View layout = findViewById(R.id.layout);

            layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    map.moveCamera(cameraUpdate);
                }
            });
        }
    }

    public void setResult(GetRoutesOrTripsResult result) {
        this.result = result;
        // The user requested a refresh. Don't reset zoom & center.
        zoomAndCenter = false;
        displayGetNextTripsForStopResult();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        displayGetNextTripsForStopResult();
    }

    public Bitmap getLabeledPin(String text) {
        float fontSize = text.length() > 2 ? 13.0f : 16.0f;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.pin_red).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(fontSize * getResources().getDisplayMetrics().density);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float textWidth = paint.measureText(text);
        float textHeight = -paint.ascent();

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, (bm.getWidth() - textWidth) / 2, bm.getHeight() * 0.27f + (textHeight / 2), paint);

        return bm;
    }

    private class TripAdapter extends ArrayAdapter<Trip> {
        private Context context;
        private int resourceId;
        private ArrayList<Trip> trips;

        public TripAdapter(Context context, int resourceId, ArrayList<Trip> trips) {
            super(context, resourceId, trips);
            this.context = context;
            this.resourceId = resourceId;
            this.trips = trips;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            if (v == null) {
                LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(resourceId, null);
            }
            final Trip trip = trips.get(position);
            if (trip != null) {
                TextView text1 = (TextView) v.findViewById(android.R.id.text1);
                TextView text2 = (TextView) v.findViewById(android.R.id.text2);
                ImageView busPin = (ImageView) v.findViewById(R.id.busPin);
                text1.setText(getHumanReadableTime(trip.getAdjustedScheduleTime()) + " (" + context.getResources().getString(trip.isEstimated() ? R.string.estimated : R.string.scheduled) + ")");
                text2.setText(context.getString(R.string.destination) + " " + trip.getDestination());
                if (trip.getLocation() == null) {
                    busPin.setImageDrawable(null);
                } else {
                    busPin.setImageDrawable(new BitmapDrawable(context.getResources(), getLabeledPin(trip.getRouteDirection().getRouteNumber())));
                }
            }
            return v;
        }

        private String getHumanReadableTime(Date date) {
            StringBuilder result = new StringBuilder();

            // Relative time
            long difference = date.getTime() - Calendar.getInstance().getTimeInMillis();
            if (difference >= 0) {
                int differenceMinutes = (int) ((difference + 30000) / 60000);
                result.append(context.getResources().getQuantityString(R.plurals.inMinutes, differenceMinutes, differenceMinutes));
            } else {
                int differenceMinutes = (int) ((-difference + 30000) / 60000);
                result.append(context.getResources().getQuantityString(R.plurals.minutesAgo, differenceMinutes, differenceMinutes));
            }

            return result.toString();
        }
    }
}
