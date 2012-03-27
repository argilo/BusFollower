package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.maps.GeoPoint;

import android.util.Log;

public class Trip implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "Trip";
	
	private String destination = null;
	private String startTime = null;
	private String adjustedScheduleTime = null;
	private float adjustmentAge = Float.NaN;
	private boolean lastTripOfSchedule = false;
	private BusType busType = new BusType("");
	private float gpsSpeed = Float.NaN;
	private float latitude = Float.NaN;
	private float longitude = Float.NaN;
	
	// Needed to get the request processing time.
	private RouteDirection routeDirection;
	
	public Trip(XmlPullParser xpp, RouteDirection routeDirection) throws XmlPullParserException, IOException {
		this.routeDirection = routeDirection;
		
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
				busType = new BusType(xpp.nextText());
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
	}
	
	public String getDestination() {
		return destination;
	}
	
	public Date getStartTime() {
		Calendar calendar = Calendar.getInstance();
		if (routeDirection.getRequestProcessingTime() != null) {
			calendar.setTime(routeDirection.getRequestProcessingTime());
		}
		
		int colonIndex = startTime.indexOf(":");
		int hour = Integer.parseInt(startTime.substring(0, colonIndex)) % 24;
		int minute = Integer.parseInt(startTime.substring(colonIndex + 1));
		
		// Since we're only given the time and not the date, search within
		// an eight hour window to ensure we get the date right.
		
		calendar.add(Calendar.HOUR, -8);
		for (int i = -8; i <= 8; i++) {
			if (calendar.get(Calendar.HOUR_OF_DAY) == hour) break;
			calendar.add(Calendar.HOUR, 1);
		}
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		return calendar.getTime();
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
	
	public BusType getBusType() {
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
