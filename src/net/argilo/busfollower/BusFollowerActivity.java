package net.argilo.busfollower;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class BusFollowerActivity extends MapActivity {
	// Values taken from stops.txt.
	private static int minLatitude = 45130104; 
	private static int maxLatitude = 45519650;
	private static int minLongitude = -76040543;
	private static int maxLongitude = -75342690;
	
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
        mapController.zoomToSpan((maxLatitude - minLatitude), (maxLongitude - minLongitude));
        mapController.setCenter(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
        
        final Button updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		updateButton.setEnabled(false);
        		
        		EditText routeNumberField = (EditText) findViewById(R.id.routeNumber);
        		EditText stopNumberField = (EditText) findViewById(R.id.stopNumber);
        		
        		final String routeNumber = routeNumberField.getText().toString();
        		final String stopNumber = stopNumberField.getText().toString();
        		
        		new Thread(new Runnable() {
        			public void run() {
        				GetNextTripsForStopResult result = dataFetcher.getNextTripsForStop(routeNumber, stopNumber);
        				
        		        List<Overlay> mapOverlays = mapView.getOverlays();
        		        mapOverlays.clear();
        		        Drawable drawable = BusFollowerActivity.this.getResources().getDrawable(R.drawable.ic_launcher);
        		        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, BusFollowerActivity.this);

        		        for (RouteDirection rd : result.getRouteDirections()) {
        					for (Trip trip : rd.getTrips()) {
        						GeoPoint point = trip.getGeoPoint();
        						if (point != null) {
        							DateFormat formatter = new SimpleDateFormat("HH:mm");
        					        OverlayItem overlayItem = new OverlayItem(point,
        					        		rd.getRouteNumber() + " " + rd.getRouteLabel(), 
        					        		"Direction: " + rd.getDirection() + 
        					        		"\nDestination: " + trip.getDestination() + 
        					        		"\nStart time: " + trip.getStartTime() +
        					        		"\nEstimated arrival: " + formatter.format(trip.getAdjustedScheduleTime()) +
        					        		"\nBus type: " + trip.getBusType() +
        					        		(trip.isLastTrip() ? "\nThis is the last trip." : ""));
        	        		        itemizedOverlay.addOverlay(overlayItem);
        						}
        					}
        				}
        		        if (itemizedOverlay.size() > 0) {
        		        	mapOverlays.add(itemizedOverlay);
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
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}