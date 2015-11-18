/*
 * Copyright 2012-2015 Clayton Smith
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
 * <http://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower;

import java.io.IOException;
import java.util.HashSet;

import net.argilo.busfollower.ocdata.GetRoutesOrTripsResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Util;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

class FetchTripsTask extends AsyncTask<TripsQuery, Void, GetRoutesOrTripsResult> {
    private Context activityContext = null;
    private Context applicationContext = null;
    private SQLiteDatabase db = null;
    private ProgressDialog progressDialog = null;
    private TripsQuery query = null;
    private String errorString = null;
    private OCTranspoDataFetcher dataFetcher = null;
    private boolean finished = false;

    public FetchTripsTask(Context context, SQLiteDatabase db) {
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
    protected GetRoutesOrTripsResult doInBackground(TripsQuery... query) {
        this.query = query[0];
        String stopNumber = query[0].getStopNumber();
        HashSet<RouteDirection> routeDirections = query[0].getRouteDirections();
        GetRoutesOrTripsResult result = null;
        try {
            dataFetcher = new OCTranspoDataFetcher(applicationContext, db);
            if (routeDirections.size() == 1) {
                String routeNumber = routeDirections.iterator().next().getRouteNumber();
                result = dataFetcher.getNextTripsForStop(stopNumber, routeNumber);
            } else {
                result = dataFetcher.getNextTripsForStopAllRoutes(stopNumber);
            }
            errorString = Util.getErrorString(applicationContext, result.getError());
            if (errorString == null) {
                // Check whether there are any trips to display, since there's no
                // point going to the map screen if there aren't.
                int totalTrips = 0;
                for (RouteDirection rd : result.getFilteredRouteDirections(routeDirections)) {
                    totalTrips += rd.getTrips().size();
                }
                if (totalTrips == 0) {
                    errorString = applicationContext.getString(routeDirections.size() == 1 ?
                            R.string.no_trips_one_route : R.string.no_trips_many_routes);
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
                ((BusFollowerActivity) activityContext).setResult(result);
            } else {
                Intent intent = new Intent(activityContext, BusFollowerActivity.class);
                intent.putExtra("query", query);
                intent.putExtra("result", result);
                activityContext.startActivity(intent);
            }
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
        return ProgressDialog.show(activityContext, "", applicationContext.getString(R.string.loading), true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (dataFetcher != null) {
                    cancel(false);
                    dataFetcher.abortRequest();
                }
            }
        });
    }
}
