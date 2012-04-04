package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class Route implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "Route";
	
	private String number = null;
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
				number = xpp.nextText();
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
	
	public String getNumber() {
		return number;
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
		if (number != null) {
			result += number;
		}
		if (heading != null) {
			result += "  " + heading;
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof Route) {
			Route otherRoute = (Route) other;
			return number.equals(otherRoute.number) && direction.equals(otherRoute.direction);
		} else {
			return false;
		}
	}
}
