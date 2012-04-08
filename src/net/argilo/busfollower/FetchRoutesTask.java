package net.argilo.busfollower;

import java.io.IOException;

import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
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

public class FetchRoutesTask extends AsyncTask<String, Void, GetRouteSummaryForStopResult> {
	Context context = null;
	SQLiteDatabase db = null;
	ProgressDialog progressDialog = null;
	private Stop stop = null;
	private String errorString = null;
	
	public FetchRoutesTask(Context context, SQLiteDatabase db) {
		super();
		this.context = context;
		this.db = db;
	}

	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading));
	}
	
	@Override
	protected GetRouteSummaryForStopResult doInBackground(String... stopNumber) {
		GetRouteSummaryForStopResult result = null;
		try {
			stop = new Stop(context, db, stopNumber[0]);
			result = OCTranspoDataFetcher.getRouteSummaryForStop(context, stop.getNumber());
			errorString = Util.getErrorString(context, result.getError());
			if (errorString == null) {
				if(result.getRoutes().isEmpty()) {
					errorString = context.getString(R.string.no_routes);
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
	protected void onPostExecute(GetRouteSummaryForStopResult result) {
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
			Intent intent = new Intent(context, RouteChooserActivity.class);
			intent.putExtra("stop", stop);
			intent.putExtra("result", result);
			context.startActivity(intent);
		}
	}
}