package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.Stop;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class StopChooserActivity extends ListActivity {
	private SQLiteDatabase db = null;
	private RecentQueryAdapter recentQueryAdapter = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopchooser);

        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException

        final AutoCompleteTextView stopSearchField = (AutoCompleteTextView) findViewById(R.id.stopSearch);
        final Button chooseMapButton = (Button) findViewById(R.id.chooseMap);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_dropdown_item_1line, null, 
				new String[] { "stop_desc" }, new int[] { android.R.id.text1 });
		stopSearchField.setAdapter(adapter);
		
		adapter.setCursorToStringConverter(new CursorToStringConverter() {
			@Override
			public String convertToString(Cursor cursor) {
				return cursor.getString(cursor.getColumnIndexOrThrow("stop_code"));
			}
		});
		
		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				String constraintStr = (constraint != null ? constraint.toString() : "");
				String[] pieces = constraintStr.split(" ");
				
				String query = "SELECT stop_id AS _id, stop_code, stop_code || \"  \" || stop_name AS stop_desc FROM stops WHERE stop_code IS NOT NULL";
				ArrayList<String> params = new ArrayList<String>();
				for (String piece : pieces) {
					if (piece.length() > 0) {
						query += " AND (stop_name LIKE ?";
						params.add("%" + piece + "%");
						if (piece.matches("\\d\\d\\d?\\d?")) {
							query += " OR stop_code LIKE ?";
							params.add(piece + "%");
						}
						query += ")";
					}
				}
				query += " ORDER BY total_departures DESC";
				Cursor cursor = db.rawQuery(query, params.toArray(new String[params.size()]));
				if (cursor != null) {
					// TODO: Handle Cursor with CursorLoader / LoaderManager
					cursor.moveToFirst();
					return cursor;
				}
				
				return null;
			}
		});
		
		stopSearchField.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO || 
						(actionId == EditorInfo.IME_NULL && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
					new FetchRoutesTask(StopChooserActivity.this, db).execute(stopSearchField.getText().toString());
					return true;
				}
				return false;
			}
		});
		
		stopSearchField.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				new FetchRoutesTask(StopChooserActivity.this, db).execute(stopSearchField.getText().toString());
			}
		});
		
		chooseMapButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(StopChooserActivity.this, MapChooserActivity.class);
				startActivity(intent);
			}
		});

    	recentQueryAdapter = new RecentQueryAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<RecentQuery>());
        setListAdapter(recentQueryAdapter);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	recentQueryAdapter.clear();

    	ArrayList<RecentQuery> recentQueryList = RecentQueryList.loadRecents(this);
    	Stop lastStop = null;
    	for (RecentQuery recentQuery : recentQueryList) {
    		Stop thisStop = recentQuery.getStop();
    		if (!thisStop.equals(lastStop)) {
    			// Add a stop heading that will group all routes departing from this stop.
    			recentQueryAdapter.add(new RecentQuery(thisStop));
    			lastStop = thisStop;
    		}
    		recentQueryAdapter.add(recentQuery);
    	}
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	RecentQuery query = recentQueryAdapter.getItem(position);
    	if (query.getRoute() == null) {
			new FetchRoutesTask(StopChooserActivity.this, db).execute(query.getStop().getNumber());
    	} else {
    		new FetchTripsTask(this, db).execute(query);
    	}
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
	}
	
    private class RecentQueryAdapter extends ArrayAdapter<RecentQuery> {
    	private Context context;
    	private int resourceId;
    	private ArrayList<RecentQuery> queries;
    	
    	public RecentQueryAdapter(Context context, int resourceId, ArrayList<RecentQuery> queries) {
    		super(context, resourceId, queries);
    		this.context = context;
    		this.resourceId = resourceId;
    		this.queries = queries;
    	}
    	
    	@Override
    	public View getView(int position, View v, ViewGroup parent) {
    		RecentQuery query = queries.get(position);

    		if (v == null) {
    			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = li.inflate(resourceId, null);
    		}

			TextView text1 = (TextView) v.findViewById(android.R.id.text1);
			text1.setSingleLine();
			text1.setEllipsize(TextUtils.TruncateAt.END);
			if (query.getRoute() == null) {
				// Stop number heading
    			text1.setBackgroundColor(Color.DKGRAY);
    			text1.setText(context.getString(R.string.stop_number) + " " + 
    					query.getStop().getNumber() + " " + query.getStop().getName());
			} else {
				// Route number
    			text1.setBackgroundColor(Color.BLACK);
				text1.setText("    " + context.getString(R.string.route_number) + " " + 
						query.getRoute().getNumber() + " " + query.getRoute().getHeading());
			}
			
    		return v;
    	}
    }
}