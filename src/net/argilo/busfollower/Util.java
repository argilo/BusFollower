package net.argilo.busfollower;

import java.lang.reflect.Method;

import android.app.Activity;
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
}
