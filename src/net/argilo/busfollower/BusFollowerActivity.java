package net.argilo.busfollower;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BusFollowerActivity extends MapActivity {
	private static final String TAG = "BusFollowerActivity";
	// The zoom level to use when there's only one point to display.
	private static final int MIN_ZOOM = 10000;
	
	private SQLiteDatabase db;
	private GetNextTripsForStopResult result = null;
	private Route route;

	private MapView mapView = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.busfollower);
        
        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
        
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        
        result = (GetNextTripsForStopResult) getIntent().getSerializableExtra("result");
        route = (Route) getIntent().getSerializableExtra("route");
        if (savedInstanceState != null) {
        	result = (GetNextTripsForStopResult) savedInstanceState.getSerializable("result");
        	route = (Route) savedInstanceState.getSerializable("route");

        	if (result != null) {
    			displayGetNextTripsForStopResult(result);
        	} else {
    	        // Zoom to OC Transpo service area if it's our first time.
    	        MapController mapController = mapView.getController();
    	        mapController.zoomToSpan(MIN_ZOOM, MIN_ZOOM);
    	        mapController.setCenter(result.getStop().getLocation());
        	}
        } else {
        	RecentQueryList.addOrUpdateRecent(this, result.getStop(), route);
        	displayGetNextTripsForStopResult(result);
        }
        
        setTitle(getString(R.string.stop_number) + " " + result.getStop().getNumber() +
        		", " + getString(R.string.route_number) + " " + route.getNumber() + " " + route.getHeading());
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("result", result);
		outState.putSerializable("route", route);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
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
			case R.id.menu_refresh:
				new FetchTripsTask(this, db).execute(new RecentQuery(result.getStop(), route));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	void displayGetNextTripsForStopResult(final GetNextTripsForStopResult result) {
        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.clear();
        Drawable drawable = BusFollowerActivity.this.getResources().getDrawable(R.drawable.pin_red);
        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, BusFollowerActivity.this, db);
        
        int minLatitude = Integer.MAX_VALUE;
        int maxLatitude = Integer.MIN_VALUE;
        int minLongitude = Integer.MAX_VALUE;
        int maxLongitude = Integer.MIN_VALUE;
        
        GeoPoint stopLocation = result.getStop().getLocation();
        if (stopLocation != null) {
        	itemizedOverlay.addOverlay(new StopOverlayItem(result.getStop(), BusFollowerActivity.this));
        	minLatitude = maxLatitude = stopLocation.getLatitudeE6();
        	minLongitude = maxLongitude = stopLocation.getLongitudeE6();
        }

        for (RouteDirection rd : result.getRouteDirections()) {
        	if (rd.getDirection().equals(route.getDirection())) {
                ListView tripList = (ListView) findViewById(R.id.tripList);
                tripList.setAdapter(new TripAdapter(BusFollowerActivity.this, R.layout.tripitem, rd.getTrips()));

                int number = 0;
                for (Trip trip : rd.getTrips()) {
                	number++;
					GeoPoint point = trip.getGeoPoint();
					if (point != null) {
						minLatitude = Math.min(minLatitude, point.getLatitudeE6());
						maxLatitude = Math.max(maxLatitude, point.getLatitudeE6());
						minLongitude = Math.min(minLongitude, point.getLongitudeE6());
						maxLongitude = Math.max(maxLongitude, point.getLongitudeE6());
						
	    		        itemizedOverlay.addOverlay(new BusOverlayItem(point, BusFollowerActivity.this, rd, trip, number));
					}
				}
        	}
		}
        if (itemizedOverlay.size() > 0) {
        	mapOverlays.add(itemizedOverlay);
        	
            MapController mapController = mapView.getController();
            mapController.zoomToSpan(Math.max(MIN_ZOOM, (maxLatitude - minLatitude) * 110 / 100), Math.max(MIN_ZOOM, (maxLongitude - minLongitude) * 110 / 100));
            mapController.setCenter(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
        }
        mapView.invalidate();
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
    		Trip trip = trips.get(position);
    		if (trip != null) {
    			TextView text1 = (TextView) v.findViewById(android.R.id.text1);
    			TextView text2 = (TextView) v.findViewById(android.R.id.text2);
    			ImageView busPin = (ImageView) v.findViewById(R.id.busPin);
    			text1.setText(getHumanReadableTime(trip.getAdjustedScheduleTime()) + (trip.isEstimated() ? " (estimated)" : " (scheduled)"));
    			text2.setText("Destination: " + trip.getDestination());
    			if (trip.getGeoPoint() == null) {
    				busPin.setImageDrawable(null);
    			} else {
    				busPin.setImageDrawable(Util.getNumberedPin(BusFollowerActivity.this, position + 1));
    			}
    		}
    		return v;
    	}

    	private String getHumanReadableTime(Date date) {
    		StringBuffer result = new StringBuffer();
    		
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