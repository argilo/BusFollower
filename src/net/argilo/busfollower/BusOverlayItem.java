package net.argilo.busfollower;

import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Trip;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class BusOverlayItem extends OverlayItem {
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
		return Util.getBusInformationString(context, rd, trip);
	}
	
	@Override
	public Drawable getMarker(int stateBitset) {
		Drawable drawable = Util.getNumberedPin(context, number);
		drawable.setBounds(-drawable.getIntrinsicWidth() / 2, -drawable.getIntrinsicHeight(), drawable.getIntrinsicWidth() / 2, 0);
		return drawable;
	}
}
