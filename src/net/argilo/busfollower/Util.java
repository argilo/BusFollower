package net.argilo.busfollower;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.argilo.busfollower.ocdata.BusType;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

public class Util {
	private static final String TAG = "Util";
	
	public static Drawable getNumberedPin(Context context, int number) {
		int pinId;
		try {
			pinId = R.drawable.class.getField("pin_red_" + number).getInt(null);
		} catch (Exception e) {
			Log.e(TAG, "Couldn't find numbered pin.");
			pinId = R.drawable.pin_red;
		}
		return context.getResources().getDrawable(pinId);
	}
	
	public static void setDisplayHomeAsUpEnabled(Context context, boolean bool) {
        try {
        	Class<?> actionBarClass = Class.forName("android.app.ActionBar");
        	Method getActionBarMethod = Activity.class.getMethod("getActionBar");
        	Object actionBar = getActionBarMethod.invoke(context);
        	Method setDisplayHomeAsUpEnabledMethod = actionBarClass.getMethod("setDisplayHomeAsUpEnabled", Boolean.TYPE);
        	setDisplayHomeAsUpEnabledMethod.invoke(actionBar, bool);
        } catch (Exception e) {
        	// We're not running honeycomb or later.
        }
	}
	
	public static String getBusInformationString(Context context, RouteDirection rd, Trip trip) {
		return context.getString(R.string.direction) + " " + rd.getDirection() + 
        		"\n" + context.getString(R.string.destination) + " " + trip.getDestination() + 
        		"\n" + context.getString(R.string.start_time) + " " + getHumanReadableTime(context, trip.getStartTime()) +
        		"\n" + context.getString(trip.isEstimated() ? R.string.estimated_arrival : R.string.scheduled_arrival) + 
        		" " + getHumanReadableTime(context, trip.getAdjustedScheduleTime()) +
        		(Float.isNaN(trip.getGpsSpeed()) ? "" : "\n" + context.getString(R.string.bus_speed) + " " + 
        		Math.round(trip.getGpsSpeed() - 0.01) + " " + context.getString(R.string.kph)) +
        		"\n" + context.getString(R.string.bus_type) + " " + getBusTypeString(context, trip.getBusType()) +
        		(trip.isLastTrip() ? "\n" + context.getString(R.string.last_trip) : "");
	}
	
	private static String getBusTypeString(Context context, BusType busType) {
		ArrayList<String> pieces = new ArrayList<String>();
		
		switch (busType.getLength()) {
		case 40:
			pieces.add(context.getString(R.string.length_40));
			break;
		case 60:
			pieces.add(context.getString(R.string.length_60));
			break;
		}
		
		if (busType.hasBikeRack()) {
			pieces.add(context.getString(R.string.bike_rack));
		}
		
		if (busType.isDoubleDecker()) {
			pieces.add(context.getString(R.string.double_decker));
		}

		if (busType.isHybrid()) {
			pieces.add(context.getString(R.string.hybrid));
		}

		return TextUtils.join(", ", pieces);
	}

	private static String getHumanReadableTime(Context context, Date date) {
		StringBuffer result = new StringBuffer();
		DateFormat formatter = android.text.format.DateFormat.getTimeFormat(context);
		result.append(formatter.format(date));
		result.append(" (");
		
		// Relative time
		long difference = date.getTime() - Calendar.getInstance().getTimeInMillis();
		if (difference >= 0) {
			int differenceMinutes = (int) ((difference + 30000) / 60000);
			result.append(context.getResources().getQuantityString(R.plurals.minutes, differenceMinutes, differenceMinutes));
		} else {
			int differenceMinutes = (int) ((-difference + 30000) / 60000);
			result.append(context.getResources().getQuantityString(R.plurals.minutesAgo, differenceMinutes, differenceMinutes));
		}
		
		result.append(")");
		return result.toString();
	}
}
