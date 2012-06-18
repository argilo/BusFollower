/*
 * Copyright 2012 Clayton Smith
 *
 * This file is part of Ottawa Bus Follower.
 *
 * Ottawa Bus Follower is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3, or (at
 * your option) any later version.
 *
 * Ottawa Bus Follower is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ottawa Bus Follower; see the file COPYING.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

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
