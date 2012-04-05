package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class GetRouteSummaryForStopResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "GetRouteSummaryForStopResult";

	private String stopNumber = null;
	private String stopLabel = null;
	private String error = null;
	private ArrayList<Route> routes = new ArrayList<Route>();
	
	public GetRouteSummaryForStopResult(XmlPullParser xpp) throws XmlPullParserException, IOException {
		while (xpp.next() == XmlPullParser.START_TAG) {
			String tagName = xpp.getName();
			if ("StopNo".equalsIgnoreCase(tagName)) {
				stopNumber = xpp.nextText();
			} else if ("StopDescription".equalsIgnoreCase(tagName)) {
				stopLabel = xpp.nextText();
			} else if ("Error".equalsIgnoreCase(tagName)) {
				error = xpp.nextText();
			} else if ("Routes".equalsIgnoreCase(tagName)) {
				while (xpp.next() == XmlPullParser.START_TAG) {
					routes.add(new Route(xpp));
				}
				if ("Route".equals(xpp.getName())) {
					// Handle XML that doesn't match the published API.
					xpp.next();
				}
			} else {
				Log.w(TAG, "Unrecognized start tag: " + tagName);
			}
			xpp.require(XmlPullParser.END_TAG, null, tagName);
		}
	}
	
	public String getStopNumber() {
		return stopNumber;
	}
	
	public String getStopLabel() {
		return stopLabel;
	}
	
	public String getError() {
		return error;
	}
	
	public ArrayList<Route> getRoutes() {
		return routes;
	}
}
