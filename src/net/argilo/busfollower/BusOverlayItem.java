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
