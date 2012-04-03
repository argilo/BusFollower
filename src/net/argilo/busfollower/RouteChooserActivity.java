package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.Stop;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RouteChooserActivity extends ListActivity {
	private Stop stop;
	private ArrayList<Route> routes;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routechooser);

        stop = (Stop) getIntent().getSerializableExtra("stop");
        GetRouteSummaryForStopResult result = (GetRouteSummaryForStopResult) getIntent().getSerializableExtra("routes");
        routes = result.getRoutes();
        
        setListAdapter(new ArrayAdapter<Route>(this, android.R.layout.simple_list_item_1, routes));
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
		Intent intent = new Intent(this, BusFollowerActivity.class);
		intent.putExtra("stop", stop);
		intent.putExtra("route", routes.get(position));
		startActivity(intent);
    }
}
