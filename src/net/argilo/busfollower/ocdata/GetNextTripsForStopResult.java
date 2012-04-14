package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GetNextTripsForStopResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "GetNextTripForStopResult";

	private Stop stop = null;
	private String error = null;
	private ArrayList<RouteDirection> routeDirections = new ArrayList<RouteDirection>();
	
	public GetNextTripsForStopResult(Context context, SQLiteDatabase db, XmlPullParser xpp, String stopNumber)
			throws XmlPullParserException, IOException, IllegalArgumentException {
		stop = new Stop(context, db, stopNumber);
		String stopLabel = null;
		
		while (xpp.next() == XmlPullParser.START_TAG) {
			String tagName = xpp.getName();
			if ("StopNo".equalsIgnoreCase(tagName)) {
				// We already know the stop number, so discard the result.
				xpp.nextText();
			} else if ("StopLabel".equalsIgnoreCase(tagName)) {
				stopLabel = xpp.nextText();
			} else if ("Error".equalsIgnoreCase(tagName)) {
				error = xpp.nextText();
			} else if ("Route".equalsIgnoreCase(tagName)) {
				while (xpp.next() == XmlPullParser.START_TAG) {
					xpp.require(XmlPullParser.START_TAG, null, "RouteDirection");
					routeDirections.add(new RouteDirection(xpp));
					xpp.require(XmlPullParser.END_TAG, null, "RouteDirection");
				}
			} else {
				Log.w(TAG, "Unrecognized start tag: " + tagName);
			}
			xpp.require(XmlPullParser.END_TAG, null, tagName);
		}
	}
	
	public Stop getStop() {
		return stop;
	}
	
	public String getError() {
		return error;
	}
	
	public ArrayList<RouteDirection> getRouteDirections() {
		return routeDirections;
	}
}
