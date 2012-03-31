package net.argilo.busfollower;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.argilo.busfollower.ocdata.BusType;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class BusOverlayItem extends OverlayItem {
	Context context;
	RouteDirection rd;
	Trip trip;
	
	public BusOverlayItem(GeoPoint point, Context context, RouteDirection rd, Trip trip) {
		super(point, rd.getRouteNumber() + " " + rd.getRouteLabel(), "");
		this.context = context;
		this.rd = rd;
		this.trip = trip;
	}

	@Override
	public String getSnippet() {
		return getBusInformationString(rd, trip);
	}
	
	private String getBusInformationString(RouteDirection rd, Trip trip) {
		return context.getString(R.string.direction) + " " + rd.getDirection() + 
        		"\n" + context.getString(R.string.destination) + " " + trip.getDestination() + 
        		"\n" + context.getString(R.string.start_time) + " " + getHumanReadableTime(trip.getStartTime()) +
        		"\n" + context.getString(trip.isEstimated() ? R.string.estimated_arrival : R.string.scheduled_arrival) + 
        		" " + getHumanReadableTime(trip.getAdjustedScheduleTime()) +
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
