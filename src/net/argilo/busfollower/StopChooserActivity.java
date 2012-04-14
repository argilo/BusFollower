package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.Stop;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class StopChooserActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "StopChooserActivity";
	
	private SQLiteDatabase db = null;
	private RecentQueryAdapter recentQueryAdapter = null;
	private SimpleCursorAdapter adapter = null;
	private AutoCompleteTextView stopSearchField = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopchooser);

        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
                
        stopSearchField = (AutoCompleteTextView) findViewById(R.id.stopSearch);
        final Button chooseMapButton = (Button) findViewById(R.id.chooseMap);

		adapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_dropdown_item_1line, null, 
				new String[] { "stop_desc" }, new int[] { android.R.id.text1 }, 0);
		stopSearchField.setAdapter(adapter);
		
		adapter.setCursorToStringConverter(new CursorToStringConverter() {
			@Override
			public String convertToString(Cursor cursor) {
				return cursor.getString(cursor.getColumnIndexOrThrow("stop_code"));
			}
		});
		
		stopSearchField.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				// Do nothing.
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Do nothing.
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (stopSearchField.enoughToFilter()) {
					getSupportLoaderManager().restartLoader(0, null, StopChooserActivity.this);
				}
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

    	ListView recentList = (ListView) findViewById(R.id.recentList);
        recentQueryAdapter = new RecentQueryAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<RecentQuery>());
    	recentList.setAdapter(recentQueryAdapter);

    	recentList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                RecentQuery query = recentQueryAdapter.getItem(position);
                if (query.getRoute() == null) {
                    new FetchRoutesTask(StopChooserActivity.this, db).execute(query.getStop().getNumber());
                } else {
                    new FetchTripsTask(StopChooserActivity.this, db).execute(query);
                }
            }
    	});
    	
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getSupportLoaderManager().initLoader(0, null, this);
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
				text1.setTypeface(null, Typeface.BOLD);
    			text1.setText(context.getString(R.string.stop_number) + " " + 
    					query.getStop().getNumber() + " " + query.getStop().getName());
			} else {
				// Route number
				text1.setTypeface(null, Typeface.NORMAL);
				text1.setText(" \u00BB " + context.getString(R.string.route_number) + " " + 
						query.getRoute().getNumber() + " " + query.getRoute().getHeading());
			}
			
    		return v;
    	}
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        return new CursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
            	Log.d(TAG, "Loading cursor in background.");
            	String constraintStr = stopSearchField.getText().toString();
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
                    cursor.moveToFirst();
                	Log.d(TAG, "Done loading cursor in background.");
                    return cursor;
                }

                Log.d(TAG, "Cursor was null.");
                return null;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        adapter.changeCursor(null);       
    }
}