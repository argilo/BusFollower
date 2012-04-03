package net.argilo.busfollower;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Trip;
import net.argilo.busfollower.ocdata.Util;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
	private Stop stop;
	private Route route;

	private MapView mapView = null;
	private EditText stopNumberField = null;
	private EditText routeNumberField = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.busfollower);
        
        dataFetcher = new OCTranspoDataFetcher(this);
        
        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
        
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        
		stopNumberField = (EditText) findViewById(R.id.stopNumber);
		routeNumberField = (EditText) findViewById(R.id.routeNumber);
		
        stop = (Stop) getIntent().getSerializableExtra("stop");
        route = (Route) getIntent().getSerializableExtra("route");
        if (savedInstanceState != null) {
        	result = (GetNextTripsForStopResult) savedInstanceState.getSerializable("result");
        	stop = (Stop) savedInstanceState.getSerializable("stop");
        	route = (Route) savedInstanceState.getSerializable("route");
        }
    	stopNumberField.setText(stop.getNumber());
    	routeNumberField.setText(route.getNumber());
        
    	if (result != null) {
			displayGetNextTripsForStopResult(result);
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
        		
        		new Thread(new Runnable() {
        			public void run() {
        				String errorString;
        				try {
        					result = dataFetcher.getNextTripsForStop(BusFollowerActivity.this, db, stop.getNumber(), route.getNumber());
            				errorString = Util.getErrorString(BusFollowerActivity.this, result.getError());
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
        				
        				BusFollowerActivity.this.displayGetNextTripsForStopResult(result);
        				
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
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("result", result);
		outState.putSerializable("stop", stop);
		outState.putSerializable("route", route);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
	}
	
	private void displayGetNextTripsForStopResult(GetNextTripsForStopResult result) {
        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.clear();
        Drawable drawable = BusFollowerActivity.this.getResources().getDrawable(R.drawable.pin_red);
        BusFollowerItemizedOverlay itemizedOverlay = new BusFollowerItemizedOverlay(drawable, BusFollowerActivity.this);
        
        int minLatitude = Integer.MAX_VALUE;
        int maxLatitude = Integer.MIN_VALUE;
        int minLongitude = Integer.MAX_VALUE;
        int maxLongitude = Integer.MIN_VALUE;
        
        GeoPoint stopLocation = result.getStop().getLocation();
        if (stopLocation != null) {
        	itemizedOverlay.addOverlay(new StopOverlayItem(result.getStop(), this));
        	minLatitude = maxLatitude = stopLocation.getLatitudeE6();
        	minLongitude = maxLongitude = stopLocation.getLongitudeE6();
        }

        for (final RouteDirection rd : result.getRouteDirections()) {
        	if (rd.getDirection().equals(route.getDirection())) {
                final ListView tripList = (ListView) findViewById(R.id.tripList);
                runOnUiThread(new Runnable() {
                	public void run() {
                        tripList.setAdapter(new TripAdapter(BusFollowerActivity.this, android.R.layout.simple_list_item_2, rd.getTrips()));
                	}
                });

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
		}
        if (itemizedOverlay.size() > 0) {
        	mapOverlays.add(itemizedOverlay);
        	
            MapController mapController = mapView.getController();
            mapController.zoomToSpan(Math.max(10000, maxLatitude - minLatitude) * 11 / 10, Math.max(10000, maxLongitude - minLongitude) * 11 / 10);
            mapController.animateTo(new GeoPoint((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
        }		
	}

    private class TripAdapter extends ArrayAdapter<Trip> {
    	private int resourceId;
    	private ArrayList<Trip> trips;
    	
    	public TripAdapter(Context context, int resourceId, ArrayList<Trip> trips) {
    		super(context, resourceId, trips);
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
    			text1.setText(trip.getDestination());
    			text2.setText("Bleargh!!!");
    		}
    		return v;
    	}
    }
}