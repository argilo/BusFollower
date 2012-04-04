package net.argilo.busfollower;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Util;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RouteChooserActivity extends ListActivity {
	private SQLiteDatabase db = null;

	private Stop stop;
	private ArrayList<Route> routes;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routechooser);

        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException

        stop = (Stop) getIntent().getSerializableExtra("stop");
        GetRouteSummaryForStopResult result = (GetRouteSummaryForStopResult) getIntent().getSerializableExtra("result");
        routes = result.getRoutes();
        
        setListAdapter(new ArrayAdapter<Route>(this, android.R.layout.simple_list_item_1, routes));
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	new FetchTripsTask().execute(routes.get(position));
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
	}

	private class FetchTripsTask extends AsyncTask<Route, Void, GetNextTripsForStopResult> {
		ProgressDialog progressDialog = null;
		private Route route = null;
		private String errorString = null;
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(RouteChooserActivity.this, "", RouteChooserActivity.this.getString(R.string.loading));
		}

		@Override
		protected GetNextTripsForStopResult doInBackground(Route... params) {
			route = params[0];
			GetNextTripsForStopResult result = null;
			try {
				result = OCTranspoDataFetcher.getNextTripsForStop(RouteChooserActivity.this, db, stop.getNumber(), route.getNumber());
				errorString = Util.getErrorString(RouteChooserActivity.this, result.getError());
				if (errorString == null) {
					// Check whether there are any trips to display, since there's no
					// point going to the map screen if there aren't.
			        for (RouteDirection rd : result.getRouteDirections()) {
			        	if (rd.getDirection().equals(route.getDirection())) {
			        		if (rd.getTrips().isEmpty()) {
								errorString = RouteChooserActivity.this.getString(R.string.no_trips);
			        		}
			        	}
			        }
				}
			} catch (IOException e) {
				errorString = RouteChooserActivity.this.getString(R.string.server_error); 
			} catch (XmlPullParserException e) {
				errorString = RouteChooserActivity.this.getString(R.string.invalid_response);
			} catch (IllegalArgumentException e) {
				errorString = e.getMessage();
			}
			return result;
		}
    	
		@Override
		protected void onPostExecute(GetNextTripsForStopResult result) {
			progressDialog.dismiss();
			if (errorString != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(RouteChooserActivity.this);
				builder.setTitle(R.string.error)
				.setMessage(errorString)
				.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				Intent intent = new Intent(RouteChooserActivity.this, BusFollowerActivity.class);
				intent.putExtra("result", result);
				intent.putExtra("route", route);
				startActivity(intent);
			}
		}
    }
}
