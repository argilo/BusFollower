package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RouteChooserActivity extends ListActivity {
	private SQLiteDatabase db = null;

	private Stop stop;
	private ArrayList<Route> routes;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routechooser);

        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException

        stop = (Stop) getIntent().getSerializableExtra("stop");
        GetRouteSummaryForStopResult result = (GetRouteSummaryForStopResult) getIntent().getSerializableExtra("result");
        routes = result.getRoutes();
        
        setListAdapter(new ArrayAdapter<Route>(this, android.R.layout.simple_list_item_1, routes));
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	// Here we just use RecentQuery as a convenience, since it can hold a stop and route.
    	new FetchTripsTask(this, db).execute(new RecentQuery(stop, routes.get(position)));
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
	}
}