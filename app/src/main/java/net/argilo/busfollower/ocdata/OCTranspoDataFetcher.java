/*
 * Copyright 2012-2017 Clayton Smith
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

package net.argilo.busfollower.ocdata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import net.argilo.busfollower.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

public class OCTranspoDataFetcher {
    private static final int TIMEOUT_CONNECTION = 15000;
    private static final int TIMEOUT_SOCKET = 15000;

    private final Context context;

    public OCTranspoDataFetcher(Context context) {
        this.context = context;
    }

    public GetRoutesOrTripsResult getRouteSummaryForStop(String stopNumber) throws IOException, XmlPullParserException {
        return getRoutesOrTripsResult("GetRouteSummaryForStop", stopNumber, null);
    }

    public GetRoutesOrTripsResult getNextTripsForStop(String stopNumber, String routeNumber) throws IOException, XmlPullParserException {
        return getRoutesOrTripsResult("GetNextTripsForStop", stopNumber, routeNumber);
    }

    public GetRoutesOrTripsResult getNextTripsForStopAllRoutes(String stopNumber) throws IOException, XmlPullParserException {
        return getRoutesOrTripsResult("GetNextTripsForStopAllRoutes", stopNumber, null);
    }

    public void abortRequest() {
        // TODO: Re-implement
    }

    private GetRoutesOrTripsResult getRoutesOrTripsResult(String command, String stopNumber, String routeNumber)
            throws IOException, XmlPullParserException {
        validateStopNumber(stopNumber);
        validateRouteNumber(routeNumber);

        URL url = new URL("http://api.octranspo1.com/v1.2/" + command);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            String params = "appID=" + context.getString(R.string.oc_transpo_application_id) +
                    "&apiKey=" + context.getString(R.string.oc_transpo_application_key) +
                    "&stopNo=" + stopNumber;
            if (routeNumber != null) {
                params += "&routeNo=" + routeNumber;
            }
            sendPost(conn, params);

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            InputStream in = conn.getInputStream();
            xpp.setInput(in, "UTF-8");
            xpp.next(); // <soap:Envelope>
            xpp.next(); //   <soap:Body>
            xpp.next(); //     <Get*Response>
            xpp.next(); //       <Get*Result>
            GetRoutesOrTripsResult result = new GetRoutesOrTripsResult(xpp);
            in.close();
            return result;
        } finally {
            conn.disconnect();
        }
    }

    private void sendPost(HttpURLConnection conn, String params) throws IOException {
        conn.setConnectTimeout(TIMEOUT_CONNECTION);
        conn.setReadTimeout(TIMEOUT_SOCKET);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(params);
        writer.close();
    }

    private void validateStopNumber(String stopNumber) {
        if (stopNumber.length() < 3 || stopNumber.length() > 4) {
            throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
        }
        for (int i = 0; i < stopNumber.length(); i++) {
            if (!Character.isDigit(stopNumber.charAt(i))) {
                throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
            }
        }
    }

    private void validateRouteNumber(String routeNumber) {
        if (routeNumber == null) {
            return;
        }
        if (routeNumber.length() < 1 || routeNumber.length() > 3) {
            throw new IllegalArgumentException(context.getString(R.string.invalid_route_number));
        }
        for (int i = 0; i < routeNumber.length(); i++) {
            if (!Character.isDigit(routeNumber.charAt(i))) {
                throw new IllegalArgumentException(context.getString(R.string.invalid_route_number));
            }
        }
    }
}
