package net.argilo.busfollower.ocdata;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class Trip {
	private static final String TAG = "Trip";
	
	private String destination = null;
	private String startTime = null;
	private String adjustedScheduleTime = null;
	private String adjustmentAge = null;
	private boolean lastTripOfSchedule = false;
	private String busType = null;
	private float gpsSpeed = Float.NaN;
	private float longitude = Float.NaN;
	private float latitude = Float.NaN;
	
	public Trip(XmlPullParser xpp) {
		try {
			while (xpp.next() == XmlPullParser.START_TAG) {
				String tagName = xpp.getName();
				if ("TripDestination".equalsIgnoreCase(tagName)) {
					destination = xpp.nextText();
				} else if ("TripStartTime".equalsIgnoreCase(tagName)) {
					startTime = xpp.nextText();
				} else if ("AdjustedScheduleTime".equalsIgnoreCase(tagName)) {
					adjustedScheduleTime = xpp.nextText();
				} else if ("AdjustmentAge".equalsIgnoreCase(tagName)) {
					adjustmentAge = xpp.nextText();
				} else if ("LastTripOfSchedule".equalsIgnoreCase(tagName)) {
					lastTripOfSchedule = "1".equalsIgnoreCase(xpp.nextText());
				} else if ("BusType".equalsIgnoreCase(tagName)) {
					busType = xpp.nextText();
				} else if ("GPSSpeed".equalsIgnoreCase(tagName)) {
					try {
						gpsSpeed = Float.parseFloat(xpp.nextText());
					} catch (Exception e) {
						// Ignore.
					}
				} else if ("Latitude".equalsIgnoreCase(tagName)) {
					try {
						latitude = Float.parseFloat(xpp.nextText());
					} catch (Exception e) {
						// Ignore.
					}
				} else if ("Longitude".equalsIgnoreCase(tagName)) {
					try {
						longitude = Float.parseFloat(xpp.nextText());
					} catch (Exception e) {
						// Ignore.
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
