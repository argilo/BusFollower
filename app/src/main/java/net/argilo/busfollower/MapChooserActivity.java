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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.argilo.busfollower.ocdata.Stop;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapChooserActivity extends Activity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, LocationListener {
    private static final String TAG = "MapChooserActivity";
    private static final double MAX_AREA = 0.04 * 0.04; // The maximum area for which stops will be displayed.
    private static final double MIN_LAT_SPAN = 0.01;
    private static final double MIN_LON_SPAN = 0.01;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private SQLiteDatabase db;
    private static FetchRoutesTask task;
    private GoogleMap map;
    private CameraPosition startingPosition;
    private final Map<Stop, Marker> displayedStops = new HashMap<>();
    private final Map<Marker, Stop> displayedMarkers = new HashMap<>();

    // Values taken from stops.txt.
    private static final double GLOBAL_MIN_LATITUDE = 45.130104;
    private static final double GLOBAL_MAX_LATITUDE = 45.519650;
    private static final double GLOBAL_MIN_LONGITUDE = -76.040543;
    private static final double GLOBAL_MAX_LONGITUDE = -75.342690;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Util.useAndroidTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapchooser);

        db = ((BusFollowerApplication) getApplication()).getDatabase();

        Util.setDisplayHomeAsUpEnabled(this, true);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (savedInstanceState != null) {
            if (task != null) {
                // Let the AsyncTask know we're back.
                task.setActivityContext(this);
                Log.d(TAG, "set task activity in onCreate");
            }
            float mapZoom = savedInstanceState.getFloat("mapZoomV2", -1);
            if (mapZoom != -1) {
                startingPosition = new CameraPosition(
                        new LatLng(
                                savedInstanceState.getDouble("mapTargetLatitude"),
                                savedInstanceState.getDouble("mapTargetLongitude")
                        ),
                        savedInstanceState.getFloat("mapZoomV2"),
                        savedInstanceState.getFloat("mapTilt"),
                        savedInstanceState.getFloat("mapBearing")
                );
            }
        } else {
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            float mapZoom = settings.getFloat("mapZoomV2", -1);
            if (mapZoom != -1) {
                startingPosition = new CameraPosition(
                        new LatLng(
                                settings.getFloat("mapTargetLatitude", 0),
                                settings.getFloat("mapTargetLongitude", 0)
                        ),
                        settings.getFloat("mapZoomV2", 0),
                        settings.getFloat("mapTilt", 0),
                        settings.getFloat("mapBearing", 0)
                );
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map != null) {
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            CameraPosition pos = map.getCameraPosition();
            editor.putFloat("mapTargetLatitude", (float) pos.target.latitude);
            editor.putFloat("mapTargetLongitude", (float) pos.target.longitude);
            editor.putFloat("mapZoomV2", pos.zoom);
            editor.putFloat("mapTilt", pos.tilt);
            editor.putFloat("mapBearing", pos.bearing);
            editor.apply();
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (map != null) {
            map.setMyLocationEnabled(true);

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null) {
                Criteria locationCriteria = new Criteria();
                locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
                String bestProvider = locationManager.getBestProvider(locationCriteria, true);
                if (bestProvider != null) {
                    locationManager.requestSingleUpdate(bestProvider, this, null);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation();
                }
            }
        }
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
            Log.d(TAG, "cleared task activity");
        }

        if (map != null) {
            CameraPosition pos = map.getCameraPosition();
            outState.putDouble("mapTargetLatitude", pos.target.latitude);
            outState.putDouble("mapTargetLongitude", pos.target.longitude);
            outState.putFloat("mapZoomV2", pos.zoom);
            outState.putFloat("mapTilt", pos.tilt);
            outState.putFloat("mapBearing", pos.bearing);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        enableMyLocation();

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                new DisplayStopsTask().execute(map.getProjection().getVisibleRegion().latLngBounds);
            }
        });

        map.setOnMarkerClickListener(this);
        final CameraUpdate cameraUpdate;
        if (startingPosition == null) {
            // If it's our first time running, initially show OC Transpo's service area.
            final LatLngBounds bounds = new LatLngBounds(
                    new LatLng(GLOBAL_MIN_LATITUDE, GLOBAL_MIN_LONGITUDE),
                    new LatLng(GLOBAL_MAX_LATITUDE, GLOBAL_MAX_LONGITUDE)
            );
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 30);
        } else {
            cameraUpdate = CameraUpdateFactory.newCameraPosition(startingPosition);
        }
        final View layout = findViewById(R.id.layout);

        layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                map.moveCamera(cameraUpdate);
            }
        });
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

    @Override
    public void onLocationChanged(Location location) {
        double minLatitude = location.getLatitude() - MIN_LAT_SPAN / 2;
        double maxLatitude = location.getLatitude() + MIN_LAT_SPAN / 2;
        double minLongitude = location.getLongitude() - MIN_LON_SPAN / 2;
        double maxLongitude = location.getLongitude() + MIN_LON_SPAN / 2;

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, maxLongitude)), 0));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private class DisplayStopsTask extends AsyncTask<LatLngBounds, Void, Collection<Stop>> {
        @Override
        protected Collection<Stop> doInBackground(LatLngBounds... params) {
            LatLngBounds mapBounds = params[0]; /* map.getProjection().getVisibleRegion().latLngBounds; */

            double latitudeSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude;
            double longitudeSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude;

            HashSet<Stop> stops = new HashSet<>();
            if (latitudeSpan * longitudeSpan > MAX_AREA) {
                return stops;
            }

            double minLatitude = mapBounds.getCenter().latitude - latitudeSpan;
            double maxLatitude = mapBounds.getCenter().latitude + latitudeSpan;
            double minLongitude = mapBounds.getCenter().longitude - longitudeSpan;
            double maxLongitude = mapBounds.getCenter().longitude + longitudeSpan;

            Log.d(TAG, "Before rawQuery");
            long startTime = System.currentTimeMillis();
            Cursor cursor = db.rawQuery("SELECT stop_code, stop_name, stop_lat, stop_lon FROM stops " +
                            "WHERE stop_lat > " + minLatitude + " AND stop_lat < " + maxLatitude + " " +
                            "AND stop_lon > " + minLongitude + " AND stop_lon < " + maxLongitude + " " +
                            "ORDER BY total_departures DESC", null);
            Log.d(TAG, "After rawQuery " + (System.currentTimeMillis() - startTime));
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String stopCode = cursor.getString(0);
                    String stopName = cursor.getString(1);
                    double stopLat = cursor.getDouble(2);
                    double stopLon = cursor.getDouble(3);

                    Stop stop = new Stop(stopCode, stopName, stopLat, stopLon);
                    if (stop.getLocation() != null) {
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
                    Marker marker = map.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop))
                                    .anchor(0.0f, 1.0f)
                                    .position(stop.getLocation())
                    );
                    displayedStops.put(stop, marker);
                    displayedMarkers.put(marker, stop);
                }
            }
        }

    }
}
