package net.argilo.busfollower;

import net.argilo.busfollower.ocdata.Stop;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.OverlayItem;

public class StopOverlayItem extends OverlayItem {
	private Context context;
	private Stop stop;

	public StopOverlayItem(Stop stop, Context context) {
		super(stop.getLocation(), context.getString(R.string.stop_number) + " " + stop.getNumber(), stop.getName());
		this.context = context;
		this.stop = stop;
	}

	@Override
	public Drawable getMarker(int stateBitset) {
		Drawable drawable = context.getResources().getDrawable(R.drawable.stop);
		drawable.setBounds(0, -drawable.getIntrinsicHeight(), drawable.getIntrinsicWidth(), 0);
		return drawable;
	}
	
	public Stop getStop() {
		return stop;
	}
}