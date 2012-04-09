package net.argilo.busfollower;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
}
