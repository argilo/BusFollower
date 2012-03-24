package net.argilo.busfollower;

import java.util.List;

import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class BusFollowerActivity extends MapActivity {
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
        
        Button updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
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
        					        OverlayItem overlayItem = new OverlayItem(point, "Testing", "One two three!");
        	        		        itemizedOverlay.addOverlay(overlayItem);
        						}
        					}
        				}
        		        if (itemizedOverlay.size() > 0) {
        		        	mapOverlays.add(itemizedOverlay);
        		        }
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