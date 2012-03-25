package net.argilo.busfollower;

import java.text.DateFormat;
import java.util.List;

import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BusFollowerActivity extends MapActivity {
	// Values taken from stops.txt.
	private static int globalMinLatitude = 45130104; 
	private static int globalMaxLatitude = 45519650;
	private static int globalMinLongitude = -76040543;
	private static int globalMaxLongitude = -75342690;
	
	private OCTranspoDataFetcher dataFetcher;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        dataFetcher = new OCTranspoDataFetcher(
        		getString(R.string.oc_transpo_application_id),
        		getString(R.string.oc_transpo_application_key));
        
        final MapView mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        
        // Zoom to OC Transpo service area at start.
        MapController mapController = mapView.getController();
        mapController.zoomToSpan((globalMaxLatitude - globalMinLatitude), (globalMaxLongitude - globalMinLongitude));
        mapController.setCenter(new GeoPoint((globalMaxLatitude + globalMinLatitude) / 2, (globalMaxLongitude + globalMinLongitude) / 2));
        
		final EditText stopNumberField = (EditText) findViewById(R.id.stopNumber);
		final EditText routeNumberField = (EditText) findViewById(R.id.routeNumber);
		
        final Button updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		updateButton.setEnabled(false);
        		
        		// Hide the on-screen keyboard when the user presses the Update button.
        		InputMethodManager imm = (InputMethodManager) BusFollowerActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        		imm.hideSoftInputFromWindow(updateButton.getWindowToken(), 0);
        		
        		final String stopNumber = stopNumberField.getText().toString();
        		final String routeNumber = routeNumberField.getText().toString();
        		
        		new Thread(new Runnable() {
        			public void run() {
        				GetNextTripsForStopResult result = dataFetcher.getNextTripsForStop(routeNumber, stopNumber);
        				
        		        List<Overlay> mapOverlays = mapView.getOverlays();
        		        mapOverlays.clear();
        		        Drawable drawable = BusFollowerActivity.this.getResources().getDrawable(R.drawable.ic_launcher);
        		        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, BusFollowerActivity.this);

        		        int minLatitude = 81000000;
        		        int maxLatitude = -81000000;
        		        int minLongitude = 181000000;
        		        int maxLongitude = -181000000;
        		        
        		        for (RouteDirection rd : result.getRouteDirections()) {
        					for (Trip trip : rd.getTrips()) {
        						GeoPoint point = trip.getGeoPoint();
        						if (point != null) {
        							minLatitude = Math.min(minLatitude, point.getLatitudeE6());
        							maxLatitude = Math.max(maxLatitude, point.getLatitudeE6());
        							minLongitude = Math.min(minLongitude, point.getLongitudeE6());
        							maxLongitude = Math.max(maxLongitude, point.getLongitudeE6());
        							
        							DateFormat formatter = android.text.format.DateFormat.getTimeFormat(BusFollowerActivity.this);
        					        OverlayItem overlayItem = new OverlayItem(point,
        					        		rd.getRouteNumber() + " " + rd.getRouteLabel(), 
        					        		"Direction: " + rd.getDirection() + 
        					        		"\nDestination: " + trip.getDestination() + 
        					        		"\nStart time: " + formatter.format(trip.getStartTime()) +
        					        		(trip.isEstimated() ? "\nEstimated arrival: " : "\nScheduled arrival: ") + 
        					        		formatter.format(trip.getAdjustedScheduleTime()) +
        					        		"\nBus type: " + trip.getBusType() +
        					        		(trip.isLastTrip() ? "\nThis is the last trip." : ""));
        	        		        itemizedOverlay.addOverlay(overlayItem);
        						}
        					}
        				}
        		        if (itemizedOverlay.size() > 0) {
        		        	mapOverlays.add(itemizedOverlay);
        		        	
        		            MapController mapController = mapView.getController();
        		            mapController.zoomToSpan(Math.max(10000, maxLatitude - minLatitude), Math.max(10000, maxLongitude - minLongitude));
        		            mapController.animateTo(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
        		        }
        		        mapView.post(new Runnable() {
        		        	public void run() {
                		        mapView.invalidate();
                		        updateButton.setEnabled(true);
        		        	}
        		        });
        			}
        		}).start();
        	}
        });
        
        routeNumberField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					updateButton.performClick();
					return true;
				}
				return false;
			}
		});
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}