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