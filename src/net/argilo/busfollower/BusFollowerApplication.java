package net.argilo.busfollower;

import android.app.Application;

public class BusFollowerApplication extends Application {
	RecentQueryList recentQueryList = null;
	
	@Override
	public void onCreate() {
		recentQueryList = new RecentQueryList(this);
	}
	
	public RecentQueryList getRecentQueryList() {
		return recentQueryList;
	}
}
