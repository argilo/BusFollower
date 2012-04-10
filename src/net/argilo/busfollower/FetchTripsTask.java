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
	Context context = null;
	SQLiteDatabase db = null;
	ProgressDialog progressDialog = null;
	private Route route = null;
	private String errorString = null;
	
	public FetchTripsTask(Context context, SQLiteDatabase db) {
		super();
		this.context = context;
		this.db = db;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading));
	}

	@Override
	protected GetNextTripsForStopResult doInBackground(RecentQuery... query) {
		Stop stop = query[0].getStop();
		route = query[0].getRoute();
		GetNextTripsForStopResult result = null;
		try {
			result = OCTranspoDataFetcher.getNextTripsForStop(context, db, stop.getNumber(), route.getNumber());
			errorString = Util.getErrorString(context, result.getError());
			if (errorString == null) {
				// Check whether there are any trips to display, since there's no
				// point going to the map screen if there aren't.
		        for (RouteDirection rd : result.getRouteDirections()) {
		        	if (rd.getDirection().equals(route.getDirection())) {
		        		if (rd.getTrips().isEmpty()) {
							errorString = context.getString(R.string.no_trips);
		        		}
		        	}
		        }
			}
		} catch (IOException e) {
			errorString = context.getString(R.string.server_error); 
		} catch (XmlPullParserException e) {
			errorString = context.getString(R.string.invalid_response);
		} catch (IllegalArgumentException e) {
			errorString = e.getMessage();
		}
		return result;
	}
	
	@Override
	protected void onPostExecute(GetNextTripsForStopResult result) {
		progressDialog.dismiss();
		if (errorString != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.error)
			.setMessage(errorString)
			.setNegativeButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			if (context instanceof BusFollowerActivity) {
				// Don't launch the BusFollowerActivity if it's the one that
				// requested the update. Just display the update.
				((BusFollowerActivity) context).displayGetNextTripsForStopResult(result);
			} else {
				Intent intent = new Intent(context, BusFollowerActivity.class);
				intent.putExtra("result", result);
				intent.putExtra("route", route);
				context.startActivity(intent);
			}
		}
	}
}