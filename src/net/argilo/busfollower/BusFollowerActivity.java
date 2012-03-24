package net.argilo.busfollower;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

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
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}