/*
 * Copyright 2012-2018 Clayton Smith
 *
 * This file is part of Ottawa Bus Follower.
 *
 * Ottawa Bus Follower is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3, or (at
 * your option) any later version.
 *
 * Ottawa Bus Follower is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ottawa Bus Follower; see the file COPYING.  If not, see
 * <https://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import java.io.IOException;

import net.argilo.busfollower.ocdata.GetRoutesOrTripsResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Util;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

class FetchRoutesTask extends AsyncTask<String, Void, GetRoutesOrTripsResult> {
    private Context activityContext;
    private final Context applicationContext;
    private final SQLiteDatabase db;
    private ProgressDialog progressDialog;
    private Stop stop;
    private String errorString;
    private OCTranspoDataFetcher dataFetcher;
    private boolean finished = false;

    FetchRoutesTask(Context context, SQLiteDatabase db) {
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
    protected GetRoutesOrTripsResult doInBackground(String... stopNumber) {
        GetRoutesOrTripsResult result = null;
        try {
            stop = new Stop(applicationContext, db, stopNumber[0]);
            dataFetcher = new OCTranspoDataFetcher(applicationContext);
            result = dataFetcher.getRouteSummaryForStop(stop.getNumber());
            errorString = Util.getErrorString(applicationContext, result.getError());
            if (errorString == null) {
                if(result.getRouteDirections().isEmpty()) {
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
    protected void onPostExecute(GetRoutesOrTripsResult result) {
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
            .setNegativeButton(applicationContext.getString(R.string.ok), (dialog, id) -> dialog.cancel());
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            Intent intent = new Intent(activityContext, RouteChooserActivity.class);
            intent.putExtra("stop", stop);
            intent.putExtra("result", result);
            activityContext.startActivity(intent);
        }
    }

    void setActivityContext(Context context) {
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
        return ProgressDialog.show(activityContext, "", applicationContext.getString(R.string.loading), true, true, dialog -> {
            if (dataFetcher != null) {
                cancel(false);
                dataFetcher.abortRequest();
            }
        });
    }
}
