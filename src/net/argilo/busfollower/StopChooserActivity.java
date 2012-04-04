package net.argilo.busfollower;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;
import net.argilo.busfollower.ocdata.GetRouteSummaryForStopResult;
import net.argilo.busfollower.ocdata.OCTranspoDataFetcher;
import net.argilo.busfollower.ocdata.Route;
import net.argilo.busfollower.ocdata.RouteDirection;
import net.argilo.busfollower.ocdata.Stop;
import net.argilo.busfollower.ocdata.Util;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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
	private RecentQueryList recentQueryList = null;
	
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
				if (actionId == EditorInfo.IME_ACTION_GO) {
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

    	recentQueryList = ((BusFollowerApplication) getApplicationContext()).getRecentQueryList();
        setListAdapter(new ArrayAdapter<RecentQuery>(this, android.R.layout.simple_list_item_1, recentQueryList.getRecents()));
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	new FetchTripsTask().execute(recentQueryList.getRecents().get(position));
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
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

	private class FetchTripsTask extends AsyncTask<RecentQuery, Void, GetNextTripsForStopResult> {
		ProgressDialog progressDialog = null;
		private Route route = null;
		private String errorString = null;
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(StopChooserActivity.this, "", StopChooserActivity.this.getString(R.string.loading));
		}

		@Override
		protected GetNextTripsForStopResult doInBackground(RecentQuery... query) {
			Stop stop = query[0].getStop();
			route = query[0].getRoute();
			GetNextTripsForStopResult result = null;
			try {
				result = OCTranspoDataFetcher.getNextTripsForStop(StopChooserActivity.this, db, stop.getNumber(), route.getNumber());
				errorString = Util.getErrorString(StopChooserActivity.this, result.getError());
				if (errorString == null) {
					// Check whether there are any trips to display, since there's no
					// point going to the map screen if there aren't.
			        for (RouteDirection rd : result.getRouteDirections()) {
			        	if (rd.getDirection().equals(route.getDirection())) {
			        		if (rd.getTrips().isEmpty()) {
								errorString = StopChooserActivity.this.getString(R.string.no_trips);
			        		}
			        	}
			        }
				}
			} catch (IOException e) {
				errorString = StopChooserActivity.this.getString(R.string.server_error); 
			} catch (XmlPullParserException e) {
				errorString = StopChooserActivity.this.getString(R.string.invalid_response);
			} catch (IllegalArgumentException e) {
				errorString = e.getMessage();
			}
			return result;
		}
    	
		@Override
		protected void onPostExecute(GetNextTripsForStopResult result) {
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
				Intent intent = new Intent(StopChooserActivity.this, BusFollowerActivity.class);
				intent.putExtra("result", result);
				intent.putExtra("route", route);
				startActivity(intent);
			}
		}
    }
}
