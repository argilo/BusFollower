package net.argilo.busfollower;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class StopOverlayItem extends OverlayItem {
	private Context context;

	public StopOverlayItem(GeoPoint point, Context context, String title, String snippet) {
		super(point, title, snippet);
		this.context = context;
	}

	@Override
	public Drawable getMarker(int stateBitset) {
		Drawable drawable = context.getResources().getDrawable(R.drawable.stop);
		drawable.setBounds(-drawable.getIntrinsicWidth() / 2, -drawable.getIntrinsicHeight(), drawable.getIntrinsicWidth() / 2, 0);
		return drawable;
	}
}