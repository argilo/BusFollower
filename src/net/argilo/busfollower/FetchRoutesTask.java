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
	Context activityContext = null;
	Context applicationContext = null;
	SQLiteDatabase db = null;
	ProgressDialog progressDialog = null;
	private Stop stop = null;
	private String errorString = null;
	OCTranspoDataFetcher dataFetcher = null;
	private boolean finished = false;
	
	public FetchRoutesTask(Context context, SQLiteDatabase db) {
		super();
		activityContext = context;
		applicationContext = context.getApplicationContext(); 
		this.db = db;
	}

	@Override
	protected void onPreExecute() {
		progressDialog = createProgressDialog();
	}
	
	@Override
	protected GetRouteSummaryForStopResult doInBackground(String... stopNumber) {
		GetRouteSummaryForStopResult result = null;
		try {
			stop = new Stop(applicationContext, db, stopNumber[0]);
			dataFetcher = new OCTranspoDataFetcher(applicationContext, db);
			result = dataFetcher.getRouteSummaryForStop(stop.getNumber());
			errorString = Util.getErrorString(applicationContext, result.getError());
			if (errorString == null) {
				if(result.getRoutes().isEmpty()) {
					errorString = applicationContext.getString(R.string.no_routes);
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
	protected void onPostExecute(GetRouteSummaryForStopResult result) {
		finished = true;
		if (activityContext == null) {
			return;
		}
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
			Intent intent = new Intent(activityContext, RouteChooserActivity.class);
			intent.putExtra("stop", stop);
			intent.putExtra("result", result);
			activityContext.startActivity(intent);
		}
	}

	public void setActivityContext(Context context) {
		activityContext = context;
		if (context == null) {
			progressDialog = null;
		} else {
			if (!finished && !isCancelled()) {
				progressDialog = createProgressDialog();
			}
		}
	}

	private ProgressDialog createProgressDialog() {
		ProgressDialog progressDialog = ProgressDialog.show(activityContext, "", applicationContext.getString(R.string.loading), true, true, new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (dataFetcher != null) {
					cancel(false);
					dataFetcher.abortRequest();
				}
			}
		});
		return progressDialog;
	}
}