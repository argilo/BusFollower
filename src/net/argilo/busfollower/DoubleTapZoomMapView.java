package net.argilo.busfollower;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class DoubleTapZoomMapView extends MapView {
	private static final int MAX_DOUBLE_TAP_TIME = 320;
	private static final float MAX_TOUCH_DISTANCE = 100;
	
	private long lastTouchTime = -1;
	private float lastTouchX, lastTouchY;
	
    public DoubleTapZoomMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DoubleTapZoomMapView(Context context, String apiKey) {
        super(context, apiKey);
    }

    public DoubleTapZoomMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	if (ev.getAction() == MotionEvent.ACTION_DOWN) {
    		long thisTouchTime = System.currentTimeMillis();
    		float thisTouchX = ev.getX();
    		float thisTouchY = ev.getY();
    		if (thisTouchTime - lastTouchTime <= MAX_DOUBLE_TAP_TIME
    				&& Math.abs(thisTouchX - lastTouchX) < MAX_TOUCH_DISTANCE
    				&& Math.abs(thisTouchY - lastTouchY) < MAX_TOUCH_DISTANCE) {
    			getController().zoomInFixing((int) ev.getX(), (int) ev.getY());
    			lastTouchTime = -1;
    		} else {
    			lastTouchTime = thisTouchTime;
    			lastTouchX = thisTouchX;
    			lastTouchY = thisTouchY;
    		}
    	}
    	return super.onTouchEvent(ev);
    }
}
