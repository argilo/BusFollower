package net.argilo.busfollower;

import java.util.ArrayList;

import net.argilo.busfollower.ocdata.DatabaseHelper;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

public class StopChooserActivity extends Activity {
	private SQLiteDatabase db = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stopchooser);

        db = (new DatabaseHelper(this)).getReadableDatabase();
        AutoCompleteTextView stopSearchField = (AutoCompleteTextView) findViewById(R.id.stopSearch);

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
				
				String query = "SELECT stop_id AS _id, stop_code, stop_code || \": \" || stop_name AS stop_desc FROM stops WHERE 1";
				ArrayList<String> params = new ArrayList<String>();
				for (String piece : pieces) {
					if (piece.length() > 0) {
						query += " AND stop_name LIKE ?";
						params.add("%" + piece + "%");
					}
				}
				Cursor cursor = db.rawQuery(query, params.toArray(new String[params.size()]));
				if (cursor != null) {
					StopChooserActivity.this.startManagingCursor(cursor);
					cursor.moveToFirst();
					return cursor;
				}
				
				return null;
			}
		});
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		db.close();
	}
}
