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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;

public class StopsMapView extends DoubleTapZoomMapView {
    private static final String TAG = "StopsMapView";
    private GeoPoint oldCenterPoint = null;
    private int oldZoomLevel = -1;
    private List<MapMoveListener> mapMoveListeners = new ArrayList<MapMoveListener>();
    
    public StopsMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StopsMapView(Context context, String apiKey) {
        super(context, apiKey);
    }

    public StopsMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            GeoPoint newCenterPoint = this.getMapCenter();
            if (oldCenterPoint == null || oldCenterPoint.getLatitudeE6() != newCenterPoint.getLatitudeE6()
                    || oldCenterPoint.getLongitudeE6() != newCenterPoint.getLongitudeE6()) {
                Log.d(TAG, "New center point: " + newCenterPoint.getLatitudeE6() + "," + newCenterPoint.getLongitudeE6());
                fireMapMoveEvent();
                oldCenterPoint = newCenterPoint;
            }
        }
        return super.onTouchEvent(ev);
    }
    
    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int newZoomLevel = getZoomLevel();
        if (newZoomLevel != oldZoomLevel) {
            Log.d(TAG, "New zoom level: " + newZoomLevel);
            fireMapMoveEvent();
            oldZoomLevel = newZoomLevel;
        }
    }
    
    public void addMapMoveListener(MapMoveListener listener) {
        mapMoveListeners.add(listener);
    }
    
    private void fireMapMoveEvent() {
        for (MapMoveListener listener : mapMoveListeners) {
            listener.onMapMove();
        }
    }
    
    public interface MapMoveListener {
        public void onMapMove();
    }
}
