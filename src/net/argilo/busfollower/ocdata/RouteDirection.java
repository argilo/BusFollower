package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class RouteDirection {
	private static final String TAG = "RouteDirection";

	private String routeNumber = null;
	private String routeLabel = null;
	private String direction = null;
	private String error = null;
	private String requestProcessingTime = null;
	private ArrayList<Trip> trips = new ArrayList<Trip>();

	public RouteDirection(XmlPullParser xpp) {
		try {
			while (xpp.next() == XmlPullParser.START_TAG) {
				String tagName = xpp.getName();
				if ("node".equalsIgnoreCase(tagName)) {
					// Handle XML that doesn't match the published API.
					continue;
				}
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
						trips.add(new Trip(xpp));
					}
					if ("Trip".equals(xpp.getName())) {
						// Handle XML that doesn't match the published API.
						xpp.next();
					}
				} else {
					Log.w(TAG, "Unrecognized start tag: " + tagName);
				}
				xpp.require(XmlPullParser.END_TAG, "", tagName);
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
