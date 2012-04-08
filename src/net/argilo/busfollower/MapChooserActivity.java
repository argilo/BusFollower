package net.argilo.busfollower;

import java.util.List;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.Stop;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

public class MapChooserActivity extends MapActivity {
	private static final String TAG = "MapChooserActivity";
	private SQLiteDatabase db;
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
        
        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
        
        mapView = (StopsMapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.addMapMoveListener(new StopsMapView.MapMoveListener() {
			@Override
			public void onMapMove() {
				new DisplayStopsTask().execute();
			}
        });
        
        MapController mapController = mapView.getController();
        mapController.zoomToSpan((globalMaxLatitude - globalMinLatitude), (globalMaxLongitude - globalMinLongitude));
        mapController.setCenter(new GeoPoint((globalMaxLatitude + globalMinLatitude) / 2, (globalMaxLongitude + globalMinLongitude) / 2));
        
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocationOverlay);

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
    }
        
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	private class DisplayStopsTask extends AsyncTask<Void, Void, BusFollowerItemizedOverlay> {
		@Override
		protected BusFollowerItemizedOverlay doInBackground(Void... params) {
			if (mapView.getZoomLevel() < 17) {
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
}
