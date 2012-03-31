package net.argilo.busfollower;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Trip;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BusFollowerActivity extends MapActivity {
	private static final String TAG = "BusFollowerActivity";
	
	// Values taken from stops.txt.
	private static int globalMinLatitude = 45130104; 
	private static int globalMaxLatitude = 45519650;
	private static int globalMinLongitude = -76040543;
	private static int globalMaxLongitude = -75342690;
	
	private OCTranspoDataFetcher dataFetcher;
	private SQLiteDatabase db;
	private GetNextTripsForStopResult result = null;

	private MapView mapView = null;
	private EditText stopNumberField = null;
	private EditText routeNumberField = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        dataFetcher = new OCTranspoDataFetcher(this);
        
        db = (new DatabaseHelper(this)).getReadableDatabase();
        
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        
		stopNumberField = (EditText) findViewById(R.id.stopNumber);
		routeNumberField = (EditText) findViewById(R.id.routeNumber);
		
        if (savedInstanceState != null) {
        	result = (GetNextTripsForStopResult) savedInstanceState.getSerializable("result");
        	stopNumberField.setText(savedInstanceState.getString("stopNumber"));
        	routeNumberField.setText(savedInstanceState.getString("routeNumber"));
        }
        
    	if (result != null) {
			Stop stop = null;
			try {
				stop = Stop.getStop(this, db, result.getStopNumber());
			} catch (IllegalArgumentException e) {
				// Ignore.
			}
			displayGetNextTripsForStopResult(result, stop);
    	} else {
	        // Zoom to OC Transpo service area if it's our first time.
	        MapController mapController = mapView.getController();
	        mapController.zoomToSpan((globalMaxLatitude - globalMinLatitude), (globalMaxLongitude - globalMinLongitude));
	        mapController.setCenter(new GeoPoint((globalMaxLatitude + globalMinLatitude) / 2, (globalMaxLongitude + globalMinLongitude) / 2));
    	}
        
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
                		Stop stop = null;
        				String errorString;
        				try {
        					stop = Stop.getStop(BusFollowerActivity.this, db, stopNumber);
        					result = dataFetcher.getNextTripsForStop(stopNumber, routeNumber);
            				errorString = getErrorString(result.getError());
        				} catch (IOException e) {
        					errorString = BusFollowerActivity.this.getString(R.string.server_error); 
        				} catch (XmlPullParserException e) {
        					errorString = BusFollowerActivity.this.getString(R.string.invalid_response);
        				} catch (IllegalArgumentException e) {
        					errorString = e.getMessage();
        				}
        				final String errorStringFinal = errorString;
        				if (errorString != null) {
        					BusFollowerActivity.this.runOnUiThread(new Runnable() {
        						public void run() {
                					AlertDialog.Builder builder = new AlertDialog.Builder(BusFollowerActivity.this);
                					builder.setTitle(R.string.error)
                					       .setMessage(errorStringFinal)
                					       .setNegativeButton(BusFollowerActivity.this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                					           public void onClick(DialogInterface dialog, int id) {
                					                dialog.cancel();
                					           }
                					       });
                					AlertDialog alert = builder.create();
                					alert.show();
                					updateButton.setEnabled(true);
        						}
        					});
        					return;
        				}
        				
        				BusFollowerActivity.this.displayGetNextTripsForStopResult(result, stop);
        				
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("result", result);
		outState.putString("stopNumber", stopNumberField.getText().toString());
		outState.putString("routeNumber", routeNumberField.getText().toString());
	}
	
	private void displayGetNextTripsForStopResult(GetNextTripsForStopResult result, Stop stop) {
        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.clear();
        Drawable drawable = BusFollowerActivity.this.getResources().getDrawable(R.drawable.pin_red);
        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, BusFollowerActivity.this);
        
        int minLatitude = Integer.MAX_VALUE;
        int maxLatitude = Integer.MIN_VALUE;
        int minLongitude = Integer.MAX_VALUE;
        int maxLongitude = Integer.MIN_VALUE;
        
        if (stop != null) {
        	itemizedOverlay.addOverlay(new StopOverlayItem(stop, this));
        	minLatitude = maxLatitude = stop.getLocation().getLatitudeE6();
        	minLongitude = maxLongitude = stop.getLocation().getLongitudeE6();
        }

        for (RouteDirection rd : result.getRouteDirections()) {
			for (Trip trip : rd.getTrips()) {
				GeoPoint point = trip.getGeoPoint();
				if (point != null) {
					minLatitude = Math.min(minLatitude, point.getLatitudeE6());
					maxLatitude = Math.max(maxLatitude, point.getLatitudeE6());
					minLongitude = Math.min(minLongitude, point.getLongitudeE6());
					maxLongitude = Math.max(maxLongitude, point.getLongitudeE6());
					
    		        itemizedOverlay.addOverlay(new BusOverlayItem(point, this, rd, trip));
				}
			}
		}
        if (itemizedOverlay.size() > 0) {
        	mapOverlays.add(itemizedOverlay);
        	
            MapController mapController = mapView.getController();
            mapController.zoomToSpan(Math.max(10000, maxLatitude - minLatitude) * 11 / 10, Math.max(10000, maxLongitude - minLongitude) * 11 / 10);
            mapController.animateTo(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
        }		
	}
	
	private String getErrorString(String error) {
		if ("".equals(error)) {
			return null;
		}
		
		try {
			int errorNumber = Integer.parseInt(error);
			switch (errorNumber) {
			case 1:
				return getString(R.string.invalid_api_key);
			case 2:
				return getString(R.string.unable_to_query_data_source);
			case 10:
			case 11:
				return getString(R.string.invalid_stop_number);
			case 12:
				return getString(R.string.invalid_route_number);
			default:
				Log.w(TAG, "Unknown error code: " + error);
				return null;
			}
		} catch (NumberFormatException e) {
			Log.w(TAG, "Couldn't parse error code: " + error);
			return null;
		}
	}
}