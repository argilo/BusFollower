package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class RouteDirection implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "RouteDirection";

	private String routeNumber = null;
	private String routeLabel = null;
	private String direction = null;
	private String error = null;
	private String requestProcessingTime = null;
	private ArrayList<Trip> trips = new ArrayList<Trip>();

	public RouteDirection(XmlPullParser xpp) throws XmlPullParserException, IOException {
		while (xpp.next() == XmlPullParser.START_TAG) {
			String tagName = xpp.getName();
			if ("RouteNo".equalsIgnoreCase(tagName)) {
				routeNumber = xpp.nextText();
			} else if ("RouteLabel".equalsIgnoreCase(tagName)) {
				routeLabel = xpp.nextText();
			} else if ("Direction".equalsIgnoreCase(tagName)) {
				direction = xpp.nextText();
			} else if ("Error".equalsIgnoreCase(tagName)) {
				error = xpp.nextText();
			} else if ("RequestProcessingTime".equalsIgnoreCase(tagName)) {
				requestProcessingTime = xpp.nextText();
			} else if ("Trips".equalsIgnoreCase(tagName)) {
				while (xpp.next() == XmlPullParser.START_TAG) {
					trips.add(new Trip(xpp, this));
				}
			} else {
				Log.w(TAG, "Unrecognized start tag: " + tagName);
			}
			xpp.require(XmlPullParser.END_TAG, null, tagName);
		}
	}
	
	public String getRouteNumber() {
		return routeNumber;
	}
	
	public String getRouteLabel() {
		return routeLabel;
	}
	
	public String getDirection() {
		return direction;
	}
	
	public String getError() {
		return error;
	}
	
	public Date getRequestProcessingTime() {
		try {
			DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			return format.parse(requestProcessingTime);
		} catch (ParseException e) {
			Log.w(TAG, "Couldn't parse RequestProcessingTime: " + requestProcessingTime);
			return null;
		}
	}
	
	public ArrayList<Trip> getTrips() {
		return trips;
	}
}
