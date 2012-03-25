package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.maps.GeoPoint;

import android.util.Log;

public class Trip {
	private static final String TAG = "Trip";
	
	private String destination = null;
	private String startTime = null;
	private String adjustedScheduleTime = null;
	private float adjustmentAge = Float.NaN;
	private boolean lastTripOfSchedule = false;
	private String busType = null;
	private float gpsSpeed = Float.NaN;
	private float latitude = Float.NaN;
	private float longitude = Float.NaN;
	
	// Needed to get the request processing time.
	private RouteDirection routeDirection;
	
	public Trip(XmlPullParser xpp, RouteDirection routeDirection) {
		this.routeDirection = routeDirection;
		
		try {
			while (xpp.next() == XmlPullParser.START_TAG) {
				String tagName = xpp.getName();
				if ("node".equalsIgnoreCase(tagName)) {
					// Handle XML that doesn't match the published API.
					continue;
				}
				if ("TripDestination".equalsIgnoreCase(tagName)) {
					destination = xpp.nextText();
				} else if ("TripStartTime".equalsIgnoreCase(tagName)) {
					startTime = xpp.nextText();
				} else if ("AdjustedScheduleTime".equalsIgnoreCase(tagName)) {
					adjustedScheduleTime = xpp.nextText();
				} else if ("AdjustmentAge".equalsIgnoreCase(tagName)) {
					try {
						adjustmentAge = Float.parseFloat(xpp.nextText());
					} catch (Exception e) {
						Log.w(TAG, "Couldn't parse AdjustmentAge.");
					}
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
	
	public String getDestination() {
		return destination;
	}
	
	public String getStartTime() {
		return startTime;
	}
	
	public Date getAdjustedScheduleTime() {
		try {
			Calendar calendar = Calendar.getInstance();
			if (routeDirection.getRequestProcessingTime() != null) {
				calendar.setTime(routeDirection.getRequestProcessingTime());
			}
			calendar.add(Calendar.MINUTE, Integer.parseInt(adjustedScheduleTime));
			return calendar.getTime();
		} catch (NumberFormatException e) {
			Log.w(TAG, "Couldn't parse AdjustedScheduleTime: " + adjustedScheduleTime);
			return null;
		}
	}
	
	public float getAdjustmentAge() {
		return adjustmentAge;
	}
	
	public boolean isEstimated() {
		return (adjustmentAge >= 0);
	}
	
	public boolean isLastTrip() {
		return lastTripOfSchedule;
	}
	
	public String getBusType() {
		return busType;
	}
	
	public float getGpsSpeed() {
		return gpsSpeed;
	}
	
	public GeoPoint getGeoPoint() {
		if (Float.isNaN(latitude) || Float.isNaN(longitude)) {
			return null;
		} else {
			return new GeoPoint((int)(latitude * 1000000), (int)(longitude * 1000000));
		}
	}
}
