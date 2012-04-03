package net.argilo.busfollower;

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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routechooser);

        stop = (Stop) getIntent().getSerializableExtra("stop");
        GetRouteSummaryForStopResult routes = (GetRouteSummaryForStopResult) getIntent().getSerializableExtra("routes");
        
        setListAdapter(new ArrayAdapter<Route>(this, android.R.layout.simple_list_item_1, routes.getRoutes()));
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
		Intent intent = new Intent(this, BusFollowerActivity.class);
		intent.putExtra("stop", stop);
		startActivity(intent);
    }
}
