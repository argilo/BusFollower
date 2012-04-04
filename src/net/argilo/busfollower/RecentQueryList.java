package net.argilo.busfollower;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

import android.content.Context;

public class RecentQueryList {
	private static final String FILENAME = "recent_queries";
	private static final int MAX_RECENT_QUERIES = 10;
	
	Context context;
	private ArrayList<RecentQuery> recents = null;

	@SuppressWarnings("unchecked")
	public RecentQueryList(Context context) {
		this.context = context;
		try {
			ObjectInputStream in = new ObjectInputStream(context.openFileInput(FILENAME));
			recents = (ArrayList<RecentQuery>) in.readObject();
			in.close();
		} catch (Exception e) {
			// Start a new recent list.
			recents = new ArrayList<RecentQuery>();
		}
	}
	
	public synchronized void addOrUpdateRecent(Stop stop, Route route) {
		RecentQuery query = new RecentQuery(stop, route);
		
		boolean foundQuery = false;
		for (RecentQuery recent : recents) {
			if (recent.equals(query)) {
				foundQuery = true;
				recent.queriedAgain();
				break;
			}
		}
		
		if (!foundQuery) {
			recents.add(query);
			if (recents.size() > MAX_RECENT_QUERIES) {
				// Boot the least recently used query.
				Collections.sort(recents, new QueryDateComparator());
				recents.remove(0);
			}
			// Sort by stop and route number for use.
			Collections.sort(recents, new QueryStopRouteComparator());
		}
		
		try {
			ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(FILENAME, Context.MODE_PRIVATE));
			out.writeObject(recents);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<RecentQuery> getRecents() {
		return recents;
	}
	
	private class QueryDateComparator implements Comparator<RecentQuery> {
		@Override
		public int compare(RecentQuery lhs, RecentQuery rhs) {
			return lhs.getLastQueried().compareTo(rhs.getLastQueried());
		}
	}

	private class QueryStopRouteComparator implements Comparator<RecentQuery> {
		@Override
		public int compare(RecentQuery lhs, RecentQuery rhs) {
			int lhsStopNumber = Integer.parseInt(lhs.getStop().getNumber());
			int rhsStopNumber = Integer.parseInt(rhs.getStop().getNumber());
			int lhsRouteNumber = Integer.parseInt(lhs.getRoute().getNumber());
			int rhsRouteNumber = Integer.parseInt(rhs.getRoute().getNumber());
			return (lhsStopNumber - rhsStopNumber) * 10000 + (lhsRouteNumber - rhsRouteNumber);
		}
	}
}
