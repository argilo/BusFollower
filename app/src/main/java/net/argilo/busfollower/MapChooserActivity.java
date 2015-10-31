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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.argilo.busfollower.ocdata.Stop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapChooserActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private static final String TAG = "MapChooserActivity";
    private static final double MAX_AREA = 0.04 * 0.04; // The maximum area for which stops will be displayed.
    
    private SQLiteDatabase db;
    private static FetchRoutesTask task = null;
    private GoogleMap map = null;
    private CameraUpdate startingPosition = null;
    private Map<Stop, Marker> displayedStops = new HashMap<>();
    private Map<Marker, Stop> displayedMarkers = new HashMap<>();

    // Values taken from stops.txt.
    private static final int globalMinLatitude = 45130104;
    private static final int globalMaxLatitude = 45519650;
    private static final int globalMinLongitude = -76040543;
    private static final int globalMaxLongitude = -75342690;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapchooser);
        
        db = ((BusFollowerApplication) getApplication()).getDatabase();
        
        Util.setDisplayHomeAsUpEnabled(this, true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (savedInstanceState != null) {
            if (task != null) {
                // Let the AsyncTask know we're back.
                task.setActivityContext(this);
                Log.d(TAG, "set task activity in onCreate");
            }
            startingPosition = CameraUpdateFactory.newCameraPosition(new CameraPosition(
                    new LatLng(
                            savedInstanceState.getDouble("mapTargetLatitude"),
                            savedInstanceState.getDouble("mapTargetLongitude")
                    ),
                    savedInstanceState.getFloat("mapZoom"),
                    savedInstanceState.getFloat("mapTilt"),
                    savedInstanceState.getFloat("mapBearing")
            ));
        } else {
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            float mapZoom = settings.getFloat("mapZoom", -1);
            if (mapZoom != -1) {
                startingPosition = CameraUpdateFactory.newCameraPosition(new CameraPosition(
                        new LatLng(
                                settings.getFloat("mapTargetLatitude", 0),
                                settings.getFloat("mapTargetLongitude", 0)
                        ),
                        settings.getFloat("mapZoom", 0),
                        settings.getFloat("mapTilt", 0),
                        settings.getFloat("mapBearing", 0)
                ));
            } else {
                // If it's our first time running, initially show OC Transpo's service area.
                final LatLngBounds bounds = new LatLngBounds(
                        new LatLng (globalMinLatitude / 1e6, globalMinLongitude / 1e6),
                        new LatLng (globalMaxLatitude / 1e6, globalMaxLongitude / 1e6)
                );
                startingPosition = CameraUpdateFactory.newLatLngBounds(bounds, 30);
            }
            /*
            myLocationOverlay.runOnFirstFix(new Runnable() {
                @Override
                public void run() {
                    mapController.setZoom(MIN_ZOOM_LEVEL);
                    mapController.setCenter(myLocationOverlay.getMyLocation());
                    new DisplayStopsTask().execute();
                }
            });
            */
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
/*
        myLocationOverlay.enableMyLocation();
*/
    }
    
    @Override
    protected void onPause() {
        super.onPause();

/*
        myLocationOverlay.disableMyLocation();
*/
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        CameraPosition pos = map.getCameraPosition();
        editor.putFloat("mapTargetLatitude", (float) pos.target.latitude);
        editor.putFloat("mapTargetLongitude", (float) pos.target.longitude);
        editor.putFloat("mapZoom", pos.zoom);
        editor.putFloat("mapTilt", pos.tilt);
        editor.putFloat("mapBearing", pos.bearing);
        editor.apply();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, StopChooserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
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
            Log.d(TAG, "cleared task activity");
        }

        CameraPosition pos = map.getCameraPosition();
        outState.putDouble("mapTargetLatitude", pos.target.latitude);
        outState.putDouble("mapTargetLongitude", pos.target.longitude);
        outState.putFloat("mapZoom", pos.zoom);
        outState.putFloat("mapTilt", pos.tilt);
        outState.putFloat("mapBearing", pos.bearing);
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true); // TODO: Check/request permission

        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition pos) {
                new DisplayStopsTask().execute(map.getProjection().getVisibleRegion().latLngBounds);
            }
        });

        map.moveCamera(startingPosition);
        map.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final Stop stop = displayedMarkers.get(marker);
        if (stop != null) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(stop.getNumber() == null ? stop.getName() : getString(R.string.stop_number) + " " + stop.getNumber());
            dialog.setMessage(stop.getNumber() == null ? getString(R.string.no_departures) : stop.getName());
            dialog.setNegativeButton(android.R.string.cancel, null); // dismisses by default
            if (stop.getNumber() != null) {
                dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        task = new FetchRoutesTask(MapChooserActivity.this, db);
                        Log.d(TAG, "set task activity in onMarkerClick");
                        task.execute(stop.getNumber());
                    }
                });
            }
            dialog.create();
            dialog.show();
        }
        return true;
    }

    private class DisplayStopsTask extends AsyncTask<LatLngBounds, Void, Collection<Stop>> {
        @Override
        protected Collection<Stop> doInBackground(LatLngBounds... params) {
            LatLngBounds mapBounds = params[0]; /* map.getProjection().getVisibleRegion().latLngBounds; */

            double latitudeSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude;
            double longitudeSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude;

            if (latitudeSpan * longitudeSpan > MAX_AREA) {
                return null;
            }

            double minLatitude = mapBounds.getCenter().latitude - latitudeSpan;
            double maxLatitude = mapBounds.getCenter().latitude + latitudeSpan;
            double minLongitude = mapBounds.getCenter().longitude - longitudeSpan;
            double maxLongitude = mapBounds.getCenter().longitude + longitudeSpan;
            
            Log.d(TAG, "Before rawQuery");
            long startTime = System.currentTimeMillis();
            Cursor cursor = db.rawQuery("SELECT stop_code, stop_name, stop_lat, stop_lon FROM stops " +
                    "WHERE stop_lat > ? AND stop_lat < ? AND stop_lon > ? AND stop_lon < ? " +
                    "ORDER BY total_departures DESC",
                    new String[] { String.valueOf((int) (minLatitude * 1e6)),
                                   String.valueOf((int) (maxLatitude * 1e6)),
                                   String.valueOf((int) (minLongitude * 1e6)),
                                   String.valueOf((int) (maxLongitude * 1e6)) });
            Log.d(TAG, "After rawQuery " + (System.currentTimeMillis() - startTime));
            HashSet<Stop> stops = new HashSet<>();
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String stopCode = cursor.getString(0);
                    String stopName = cursor.getString(1);
                    int stopLat = cursor.getInt(2);
                    int stopLon = cursor.getInt(3);
                    
                    Stop stop = new Stop(stopCode, stopName, stopLat, stopLon);
                    if (stop.getLocation() != null) { // TODO: Factor with code in BusFollowerActivity?
                        stops.add(stop);
                    }

                    cursor.moveToNext();
                }
                cursor.close();
                Log.d(TAG, "After cursor.close() " + (System.currentTimeMillis() - startTime));
            }
            return stops;
        }

        @Override
        protected void onPostExecute(Collection<Stop> result) {
            if (result == null) {
                return;
            }
            Iterator<Map.Entry<Stop, Marker>> iter = displayedStops.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Stop, Marker> entry = iter.next();
                Stop stop = entry.getKey();
                Marker marker = entry.getValue();
                if (!result.contains(stop)) {
                    marker.remove();
                    iter.remove();
                    displayedMarkers.remove(marker);
                }
            }
            for (Stop stop : result) {
                if (!displayedStops.containsKey(stop)) {
                    Marker marker = map.addMarker(new MarkerOptions() // TODO: Factor with code in BusFollowerActivity?
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop))
                                    .anchor(0.0f, 1.0f)
                                    .position(stop.getLocation())
                            //.title(stop.getNumber() == null ? stop.getName() : getString(R.string.stop_number) + " " + stop.getNumber())
                            //.snippet(stop.getNumber() == null ? getString(R.string.no_departures) : stop.getName())
                    );
                    displayedStops.put(stop, marker);
                    displayedMarkers.put(marker, stop);
                }
            }
            // TODO: Add buttons
        }

    }
}
