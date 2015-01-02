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

import net.argilo.busfollower.ocdata.Stop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class BusFollowerItemizedOverlay extends ItemizedOverlay<OverlayItem> {
    private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
    private Context context;
    private SQLiteDatabase db;

    public BusFollowerItemizedOverlay(Drawable defaultMarker, Context context, SQLiteDatabase db) {
        super(boundCenterBottom(defaultMarker));
        this.context = context;
        this.db = db;
    }
    
    public void addOverlay(OverlayItem overlay) {
        overlays.add(overlay);
        populate();
    }
    
    @Override
    protected boolean onTap(int index) {
        OverlayItem item = overlays.get(index);
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(item.getTitle());
        dialog.setMessage(item.getSnippet());
        if ((item instanceof StopOverlayItem) && (context instanceof MapChooserActivity)) {
            final Stop stop = ((StopOverlayItem)item).getStop();
            if (stop.getNumber() != null) {
                dialog.setPositiveButton(context.getString(R.string.open), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        FetchRoutesTask task = new FetchRoutesTask(context, db);
                        ((MapChooserActivity) context).setFetchRoutesTask(task);
                        task.execute(stop.getNumber());
                    }
                });
            }
            dialog.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        } else {
            dialog.setNegativeButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        }
        dialog.show();
        return true;
    }

    @Override
    protected OverlayItem createItem(int i) {
        return overlays.get(i);
    }

    @Override
    public int size() {
        return overlays.size();
    }

}
