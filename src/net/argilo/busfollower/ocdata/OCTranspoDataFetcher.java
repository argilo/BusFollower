/*
 * Copyright 2012 Clayton Smith
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.argilo.busfollower.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class OCTranspoDataFetcher {
    private static final int TIMEOUT_CONNECTION = 15000;
    private static final int TIMEOUT_SOCKET = 15000;
    
    private Context context;
    private SQLiteDatabase db;
    private HttpClient httpClient = null;
    
    public OCTranspoDataFetcher(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
    }
    
    public GetNextTripsForStopResult getNextTripsForStop(String stopNumber, String routeNumber)
            throws IOException, XmlPullParserException, IllegalArgumentException {
        validateStopNumber(stopNumber);
        validateRouteNumber(routeNumber);
        
        httpClient = new DefaultHttpClient(getHttpParams());
        HttpPost post = new HttpPost("https://api.octranspo1.com/v1.1/GetNextTripsForStop");
        
        List<NameValuePair> params = new ArrayList<NameValuePair>(4);
        params.add(new BasicNameValuePair("appID", context.getString(R.string.oc_transpo_application_id)));
        params.add(new BasicNameValuePair("apiKey", context.getString(R.string.oc_transpo_application_key)));
        params.add(new BasicNameValuePair("routeNo", routeNumber));
        params.add(new BasicNameValuePair("stopNo", stopNumber));
        post.setEntity(new UrlEncodedFormEntity(params));
        
        HttpResponse response = httpClient.execute(post);
        
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        InputStream in = response.getEntity().getContent();
        xpp.setInput(in, "UTF-8");
        xpp.next(); // <soap:Envelope>
        xpp.next(); //   <soap:Body>
        xpp.next(); //     <GetRouteSummaryForStopResponse>
        xpp.next(); //       <GetRouteSummaryForStopResult>
        GetNextTripsForStopResult result = new GetNextTripsForStopResult(context, db, xpp, stopNumber);
        in.close();
        return result;
    }
    
    public GetRouteSummaryForStopResult getRouteSummaryForStop(String stopNumber) throws IOException, XmlPullParserException {
        validateStopNumber(stopNumber);

        httpClient = new DefaultHttpClient(getHttpParams());
        HttpPost post = new HttpPost("https://api.octranspo1.com/v1.1/GetRouteSummaryForStop");
        
        List<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(new BasicNameValuePair("appID", context.getString(R.string.oc_transpo_application_id)));
        params.add(new BasicNameValuePair("apiKey", context.getString(R.string.oc_transpo_application_key)));
        params.add(new BasicNameValuePair("stopNo", stopNumber));
        post.setEntity(new UrlEncodedFormEntity(params));
        
        HttpResponse response = httpClient.execute(post);
        
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        InputStream in = response.getEntity().getContent();
        xpp.setInput(in, "UTF-8");
        xpp.next(); // <soap:Envelope>
        xpp.next(); //   <soap:Body>
        xpp.next(); //     <GetRouteSummaryForStopResponse>
        xpp.next(); //       <GetRouteSummaryForStopResult>
        GetRouteSummaryForStopResult result = new GetRouteSummaryForStopResult(xpp);
        in.close();
        return result;
    }
    
    public void abortRequest() {
        if (httpClient != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // This must not be done on the main thread.
                    httpClient.getConnectionManager().shutdown();
                }
            }).start();
        }
    }
    
    private HttpParams getHttpParams() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_CONNECTION);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
        return httpParams;
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
