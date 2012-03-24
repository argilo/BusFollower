package net.argilo.busfollower;

import java.util.List;

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
        
        MapView mapView = (MapView) findViewById(R.id.mapView);
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
        				dataFetcher.getNextTripsForStop(routeNumber, stopNumber);
        			}
        		}).start();
        	}
        });
        
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.ic_launcher);
        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, this);
        
        GeoPoint point = new GeoPoint(45348518,-75938460);
        OverlayItem overlayItem = new OverlayItem(point, "Testing", "One two three!");
        itemizedOverlay.addOverlay(overlayItem);
        mapOverlays.add(itemizedOverlay);
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}