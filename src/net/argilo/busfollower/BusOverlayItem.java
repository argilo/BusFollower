package net.argilo.busfollower;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.argilo.busfollower.ocdata.BusType;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class BusOverlayItem extends OverlayItem {
	private static final String TAG = "OverlayItem";
	
	Context context;
	RouteDirection rd;
	Trip trip;
	int number;
	
	public BusOverlayItem(GeoPoint point, Context context, RouteDirection rd, Trip trip, int number) {
		super(point, rd.getRouteNumber() + " " + rd.getRouteLabel(), "");
		this.context = context;
		this.rd = rd;
		this.trip = trip;
		this.number = number;
	}

	@Override
	public String getSnippet() {
		return getBusInformationString(rd, trip);
	}
	
	@Override
	public Drawable getMarker(int stateBitset) {
		int pinId;
		try {
			pinId = R.drawable.class.getField("pin_red_" + number).getInt(null);
		} catch (Exception e) {
			Log.e(TAG, "Couldn't find numbered pin.");
			pinId = R.drawable.pin_red;
		}
		Drawable drawable = context.getResources().getDrawable(pinId);
		drawable.setBounds(-drawable.getIntrinsicWidth() / 2, -drawable.getIntrinsicHeight(), drawable.getIntrinsicWidth() / 2, 0);
		return drawable;
	}
	
	private String getBusInformationString(RouteDirection rd, Trip trip) {
		return context.getString(R.string.direction) + " " + rd.getDirection() + 
        		"\n" + context.getString(R.string.destination) + " " + trip.getDestination() + 
        		"\n" + context.getString(R.string.start_time) + " " + getHumanReadableTime(trip.getStartTime()) +
        		"\n" + context.getString(trip.isEstimated() ? R.string.estimated_arrival : R.string.scheduled_arrival) + 
        		" " + getHumanReadableTime(trip.getAdjustedScheduleTime()) +
        		(trip.getGpsSpeed() != Float.NaN ? "\n" + context.getString(R.string.bus_speed) + " " + 
        		Math.round(trip.getGpsSpeed() - 0.01) + " " + context.getString(R.string.kph) : "") +
        		"\n" + context.getString(R.string.bus_type) + " " + getBusTypeString(trip.getBusType()) +
        		(trip.isLastTrip() ? "\n" + context.getString(R.string.last_trip) : "");
	}
	
	private String getBusTypeString(BusType busType) {
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

	private String getHumanReadableTime(Date date) {
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
