package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class GetNextTripForStopResult {
	private static final String TAG = "GetNextTripForStopResult";

	private String stopNumber = null;
	private String stopLabel = null;
	private String error = null;
	private ArrayList<RouteDirection> routeDirections = new ArrayList<RouteDirection>();
	
	public GetNextTripForStopResult(XmlPullParser xpp) {
		try {
			while (xpp.next() == XmlPullParser.START_TAG) {
				String tagName = xpp.getName();
				if ("StopNo".equalsIgnoreCase(tagName)) {
					stopNumber = xpp.nextText();
				} else if ("StopLabel".equalsIgnoreCase(tagName)) {
					stopLabel = xpp.nextText();
				} else if ("Error".equalsIgnoreCase(tagName)) {
					error = xpp.nextText();
				} else if ("Route".equalsIgnoreCase(tagName)) {
					while (xpp.next() == XmlPullParser.START_TAG) {
						xpp.require(XmlPullParser.START_TAG, "", "RouteDirection");
						routeDirections.add(new RouteDirection(xpp));
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
