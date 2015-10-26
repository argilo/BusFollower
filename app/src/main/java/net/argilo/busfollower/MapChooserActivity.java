/*
 * Copyright 2012 Clayton Smith
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

import java.util.List;

import net.argilo.busfollower.ocdata.Stop;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

public class MapChooserActivity extends MapActivity {
    private static final String TAG = "MapChooserActivity";
    private static final int MIN_ZOOM_LEVEL = 17; // The minimum zoom level at which stops will be displayed.
    
    private SQLiteDatabase db;
    private static FetchRoutesTask task = null;
    private StopsMapView mapView = null;
    private MyLocationOverlay myLocationOverlay = null;
    
    // Values taken from stops.txt.
    private static int globalMinLatitude = 45130104;
    private static int globalMaxLatitude = 45519650;
    private static int globalMinLongitude = -76040543;
    private static int globalMaxLongitude = -75342690;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapchooser);
        
        db = ((BusFollowerApplication) getApplication()).getDatabase();
        
        Util.setDisplayHomeAsUpEnabled(this, true);

        mapView = (StopsMapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.addMapMoveListener(new StopsMapView.MapMoveListener() {
            @Override
            public void onMapMove() {
                new DisplayStopsTask().execute();
            }
        });
        
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocationOverlay);

        final MapController mapController = mapView.getController();
        if (savedInstanceState != null) {
            if (task != null) {
                // Let the AsyncTask know we're back.
                task.setActivityContext(this);
            }
            mapController.setZoom(savedInstanceState.getInt("mapZoom"));
            mapController.setCenter(new GeoPoint(savedInstanceState.getInt("mapCenterLatitude"), savedInstanceState.getInt("mapCenterLongitude")));
        } else {
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            int mapZoom = settings.getInt("mapZoom", -1);
            if (mapZoom != -1) {
                mapController.setZoom(mapZoom);
                mapController.setCenter(new GeoPoint(settings.getInt("mapCenterLatitude", 0), settings.getInt("mapCenterLongitude", 0)));
            } else {
                // If it's our first time running, initially show OC Transpo's service area.
                mapController.zoomToSpan((globalMaxLatitude - globalMinLatitude), (globalMaxLongitude - globalMinLongitude));
                mapController.setCenter(new GeoPoint((globalMaxLatitude + globalMinLatitude) / 2, (globalMaxLongitude + globalMinLongitude) / 2));
            }
            myLocationOverlay.runOnFirstFix(new Runnable() {
                @Override
                public void run() {
                    mapController.setZoom(MIN_ZOOM_LEVEL);
                    mapController.setCenter(myLocationOverlay.getMyLocation());
                    new DisplayStopsTask().execute();
                }
            });
        }
        new DisplayStopsTask().execute();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        myLocationOverlay.enableMyLocation();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        myLocationOverlay.disableMyLocation();
        
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        GeoPoint mapCenter = mapView.getMapCenter();
        editor.putInt("mapCenterLatitude", mapCenter.getLatitudeE6());
        editor.putInt("mapCenterLongitude", mapCenter.getLongitudeE6());
        editor.putInt("mapZoom", mapView.getZoomLevel());
        editor.commit();
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
        }

        GeoPoint mapCenter = mapView.getMapCenter();
        outState.putInt("mapCenterLatitude", mapCenter.getLatitudeE6());
        outState.putInt("mapCenterLongitude", mapCenter.getLongitudeE6());
        outState.putInt("mapZoom", mapView.getZoomLevel());
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    private class DisplayStopsTask extends AsyncTask<Void, Void, BusFollowerItemizedOverlay> {
        @Override
        protected BusFollowerItemizedOverlay doInBackground(Void... params) {
            if (mapView.getZoomLevel() < MIN_ZOOM_LEVEL) {
                return null;
            }
            Drawable drawable = getResources().getDrawable(R.drawable.stop);
            BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, MapChooserActivity.this, db);
            
            int centerLatitude = mapView.getMapCenter().getLatitudeE6();
            int centerLongitude = mapView.getMapCenter().getLongitudeE6();
            
            int latitudeSpan = mapView.getLatitudeSpan() * 20 / 10;
            int longitudeSpan = mapView.getLongitudeSpan() * 20 / 10;
            
            String minLatitude = String.valueOf(centerLatitude - latitudeSpan / 2);
            String maxLatitude = String.valueOf(centerLatitude + latitudeSpan / 2);
            String minLongitude = String.valueOf(centerLongitude - longitudeSpan / 2);
            String maxLongitude = String.valueOf(centerLongitude + longitudeSpan / 2);
            
            Log.d(TAG, "Before rawQuery");
            long startTime = System.currentTimeMillis();
            Cursor cursor = db.rawQuery("SELECT stop_code, stop_name, stop_lat, stop_lon FROM stops " +
                    "WHERE stop_lat > ? AND stop_lat < ? AND stop_lon > ? AND stop_lon < ? " +
                    "ORDER BY total_departures DESC",
                    new String[] { minLatitude, maxLatitude, minLongitude, maxLongitude });
            Log.d(TAG, "After rawQuery " + (System.currentTimeMillis() - startTime));
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String stopCode = cursor.getString(0);
                    String stopName = cursor.getString(1);
                    int stopLat = cursor.getInt(2);
                    int stopLon = cursor.getInt(3);
                    
                    GeoPoint point = new GeoPoint(stopLat, stopLon);
                    if (point != null) {
                        itemizedOverlay.addOverlay(new StopOverlayItem(new Stop(stopCode, stopName, stopLat, stopLon), MapChooserActivity.this));
                    }
                    
                    cursor.moveToNext();
                }
                cursor.close();
                Log.d(TAG, "After cursor.close() " + (System.currentTimeMillis() - startTime));
            }
            return itemizedOverlay;
        }
        
        @Override
        protected void onPostExecute(BusFollowerItemizedOverlay itemizedOverlay) {
            List<Overlay> mapOverlays = mapView.getOverlays();
            // Remove the existing BusFollowerItemizedOverlay, if any.
            for (Overlay overlay : mapOverlays) {
                if (overlay instanceof BusFollowerItemizedOverlay) {
                    mapOverlays.remove(overlay);
                }
            }
            if ((itemizedOverlay != null) && (itemizedOverlay.size() > 0)) {
                mapOverlays.add(itemizedOverlay);
            }
            mapView.invalidate();
        }
    }
    
    public void setFetchRoutesTask(FetchRoutesTask task) {
        MapChooserActivity.task = task;
    }
}
