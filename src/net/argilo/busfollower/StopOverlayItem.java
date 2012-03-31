package net.argilo.busfollower;

import net.argilo.busfollower.ocdata.Stop;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.OverlayItem;

public class StopOverlayItem extends OverlayItem {
	private Context context;

	public StopOverlayItem(Stop stop, Context context) {
		super(stop.getLocation(), context.getString(R.string.stop_number) + " " + stop.getNumber(), stop.getName());
		this.context = context;
	}

	@Override
	public Drawable getMarker(int stateBitset) {
		Drawable drawable = context.getResources().getDrawable(R.drawable.stop);
		drawable.setBounds(-drawable.getIntrinsicWidth() / 2, -drawable.getIntrinsicHeight(), drawable.getIntrinsicWidth() / 2, 0);
		return drawable;
	}
}