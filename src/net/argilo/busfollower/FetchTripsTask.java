package net.argilo.busfollower;

import java.io.IOException;

import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Util;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class FetchTripsTask extends AsyncTask<RecentQuery, Void, GetNextTripsForStopResult> {
	Context activityContext = null;
	Context applicationContext = null;
	SQLiteDatabase db = null;
	ProgressDialog progressDialog = null;
	private Route route = null;
	private String errorString = null;
	OCTranspoDataFetcher dataFetcher = null;
	
	public FetchTripsTask(Context context, SQLiteDatabase db) {
		super();
		activityContext = context;
		applicationContext = context.getApplicationContext(); 
		this.db = db;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(activityContext, "", applicationContext.getString(R.string.loading), true, true, new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (dataFetcher != null) {
					cancel(false);
					dataFetcher.abortRequest();
				}
			}
		});
	}

	@Override
	protected GetNextTripsForStopResult doInBackground(RecentQuery... query) {
		Stop stop = query[0].getStop();
		route = query[0].getRoute();
		GetNextTripsForStopResult result = null;
		try {
			dataFetcher = new OCTranspoDataFetcher(applicationContext, db); 
			result = dataFetcher.getNextTripsForStop(stop.getNumber(), route.getNumber());
			errorString = Util.getErrorString(applicationContext, result.getError());
			if (errorString == null) {
				// Check whether there are any trips to display, since there's no
				// point going to the map screen if there aren't.
		        for (RouteDirection rd : result.getRouteDirections()) {
		        	if (rd.getDirection().equals(route.getDirection())) {
		        		if (rd.getTrips().isEmpty()) {
							errorString = applicationContext.getString(R.string.no_trips);
		        		}
		        	}
		        }
			}
		} catch (IOException e) {
			errorString = applicationContext.getString(R.string.server_error); 
		} catch (XmlPullParserException e) {
			errorString = applicationContext.getString(R.string.invalid_response);
		} catch (IllegalArgumentException e) {
			errorString = e.getMessage();
		} catch (IllegalStateException e) {
			// The user cancelled the request by pressing the back button.
		}
		return result;
	}
	
	@Override
	protected void onPostExecute(GetNextTripsForStopResult result) {
		progressDialog.dismiss();
		if (isCancelled()) {
			return;
		}
		if (errorString != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
			builder.setTitle(R.string.error)
			.setMessage(errorString)
			.setNegativeButton(applicationContext.getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			if (activityContext instanceof BusFollowerActivity) {
				// Don't launch the BusFollowerActivity if it's the one that
				// requested the update. Just display the update.
				((BusFollowerActivity) activityContext).displayGetNextTripsForStopResult(result);
			} else {
				Intent intent = new Intent(activityContext, BusFollowerActivity.class);
				intent.putExtra("result", result);
				intent.putExtra("route", route);
				activityContext.startActivity(intent);
			}
		}
	}
}