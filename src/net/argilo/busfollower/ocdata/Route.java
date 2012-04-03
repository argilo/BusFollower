package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class Route implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "Route";
	
	private String routeNumber = null;
	private String directionID = null;
	private String direction = null;
	private String heading = null;
	
	public Route(XmlPullParser xpp) throws XmlPullParserException, IOException {
		while (xpp.next() == XmlPullParser.START_TAG) {
			String tagName = xpp.getName();
			if ("node".equalsIgnoreCase(tagName)) {
				// Handle XML that doesn't match the published API.
				continue;
			}
			if ("RouteNo".equalsIgnoreCase(tagName)) {
				routeNumber = xpp.nextText();
			} else if ("DirectionID".equalsIgnoreCase(tagName)) {
				directionID = xpp.nextText();
			} else if ("Direction".equalsIgnoreCase(tagName)) {
				direction = xpp.nextText();
			} else if ("RouteHeading".equalsIgnoreCase(tagName)) {
				heading = xpp.nextText();
			} else {
				Log.w(TAG, "Unrecognized start tag: " + tagName);
			}
			xpp.require(XmlPullParser.END_TAG, "", tagName);
		}
	}
	
	public String getRouteNumber() {
		return routeNumber;
	}
	
	public String getDirectionID() {
		return directionID;
	}
	
	public String getDirection() {
		return direction;
	}
	
	public String getHeading() {
		return heading;
	}
	
	@Override
	public String toString() {
		String result = "";
		if (routeNumber != null) {
			result += routeNumber;
		}
		if (heading != null) {
			result += "  " + heading;
		}
		return result;
	}
}
