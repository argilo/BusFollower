package net.argilo.busfollower;

import java.io.Serializable;
import java.util.Date;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

public class RecentQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private Stop stop = null;
	private Route route = null;
	private int timesQueried = -1;
	private Date lastQueried = null;
	
	public RecentQuery(Stop stop) {
		this.stop = stop;
	}

	public RecentQuery(Stop stop, Route route) {
		this.stop = stop;
		this.route = route;
		this.timesQueried = 1;
		this.lastQueried = new Date();
	}
	
	public Stop getStop() {
		return stop;
	}
	
	public Route getRoute() {
		return route;
	}
	
	public int getTimesQueried() {
		return timesQueried;
	}
	
	public Date getLastQueried() {
		return lastQueried;
	}
	
	public void queriedAgain() {
		timesQueried++;
		lastQueried = new Date();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof RecentQuery) {
			RecentQuery otherRecentQuery = (RecentQuery) other;
			return stop.equals(otherRecentQuery.stop) && route.equals(otherRecentQuery.route);
		} else {
			return false;
		}
	}
}
