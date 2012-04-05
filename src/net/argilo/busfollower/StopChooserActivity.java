package net.argilo.busfollower;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Util;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
				query += " ORDER BY stop_code";
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
					new FetchRoutesTask().execute(stopSearchField.getText().toString());
					return true;
				}
				return false;
			}
		});
		
		stopSearchField.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				new FetchRoutesTask().execute(stopSearchField.getText().toString());
			}
		});

    	recentQueryAdapter = new RecentQueryAdapter(this, android.R.layout.simple_list_item_2, RecentQueryList.loadRecents(this)); 
        setListAdapter(recentQueryAdapter);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	recentQueryAdapter.clear();

    	ArrayList<RecentQuery> recentQueryList = RecentQueryList.loadRecents(this);
    	for (RecentQuery recentQuery : recentQueryList) {
    		recentQueryAdapter.add(recentQuery);
    	}
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	new FetchTripsTask(this, db).execute(recentQueryAdapter.getItem(position));
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
    		if (v == null) {
    			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = li.inflate(resourceId, null);
    		}
    		RecentQuery query = queries.get(position);
    		if (query != null) {
    			TextView text1 = (TextView) v.findViewById(android.R.id.text1);
    			TextView text2 = (TextView) v.findViewById(android.R.id.text2);
    			text1.setText(query.getStop().getNumber() + " " + query.getStop().getName());
    			text2.setText(context.getString(R.string.route_number) + " " + query.getRoute().getNumber() + " " + query.getRoute().getHeading());
    		}
    		return v;
    	}
    }
	
	private class FetchRoutesTask extends AsyncTask<String, Void, GetRouteSummaryForStopResult> {
		ProgressDialog progressDialog = null;
		private Stop stop = null;
		private String errorString = null;
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(StopChooserActivity.this, "", StopChooserActivity.this.getString(R.string.loading));
		}
		
		@Override
		protected GetRouteSummaryForStopResult doInBackground(String... stopNumber) {
			GetRouteSummaryForStopResult result = null;
			try {
				stop = new Stop(StopChooserActivity.this, db, stopNumber[0]);
				result = OCTranspoDataFetcher.getRouteSummaryForStop(StopChooserActivity.this, stop.getNumber());
				errorString = Util.getErrorString(StopChooserActivity.this, result.getError());
			} catch (IOException e) {
				errorString = getString(R.string.server_error); 
			} catch (XmlPullParserException e) {
				errorString = getString(R.string.invalid_response);
			} catch (IllegalArgumentException e) {
				errorString = e.getMessage();
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(GetRouteSummaryForStopResult result) {
			progressDialog.dismiss();
			if (errorString != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(StopChooserActivity.this);
				builder.setTitle(R.string.error)
				.setMessage(errorString)
				.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				Intent intent = new Intent(StopChooserActivity.this, RouteChooserActivity.class);
				intent.putExtra("stop", stop);
				intent.putExtra("result", result);
				startActivity(intent);
			}
		}
	}
}