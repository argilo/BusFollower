package net.argilo.busfollower.ocdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
	private static final String DATABASE_NAME = "db";

	Context context;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 1);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Begin database import.");

		db.execSQL("CREATE TABLE stops (stop_id TEXT PRIMARY KEY, " +
				"stop_code INT, stop_name TEXT, stop_lat INT, stop_lon INT);");
		
		try {
		    BufferedReader in = new BufferedReader(new InputStreamReader(context.getAssets().open("stops.txt")));
		    String line = in.readLine();
		    String[] columns = csvColumns(line);
		    int stopIdCol = -1, stopCodeCol = -1, stopNameCol = -1, stopLatCol = -1, stopLonCol = -1;
		    for (int i = 0; i < columns.length; i++) {
                if ("stop_id".equals(columns[i])) {
                    stopIdCol = i;
                } else if ("stop_code".equals(columns[i])) {
		            stopCodeCol = i;
		        } else if ("stop_name".equals(columns[i])) {
		            stopNameCol = i;
		        } else if ("stop_lat".equals(columns[i])) {
		            stopLatCol = i;
		        } else if ("stop_lon".equals(columns[i])) {
		            stopLonCol = i;
		        }
		    }
		    ContentValues cv = new ContentValues();
		    while ((line = in.readLine()) != null) {
		        columns = csvColumns(line);

                cv.put("stop_id", columns[stopIdCol]);
		        try {
	                cv.put("stop_code", Integer.parseInt(columns[stopCodeCol]));
		        } catch (NumberFormatException e) {
		        	cv.putNull("stop_code");
		        }
                cv.put("stop_name", columns[stopNameCol]);
		        try {
		        	cv.put("stop_lat", Util.latStringToMicroDegrees(columns[stopLatCol]));
		        } catch (NumberFormatException e) {
		        	cv.putNull("stop_name");
		        }
		        try {
		        	cv.put("stop_lon", Util.lonStringToMicroDegrees(columns[stopLonCol]));
		        } catch (NumberFormatException e) {
		        	cv.putNull("stop_name");
		        }

                db.insert("stops", "stop_code", cv);
		    }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
        Log.d(TAG, "End database import.");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	private String[] csvColumns(String line) {
		String[] columns = line.split(",");
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].length() >= 2 && columns[i].startsWith("\"") && columns[i].endsWith("\"")) {
				// Strip off surrounding quotation marks.
				columns[i] = columns[i].substring(1, columns[i].length() - 1);
				// Unescape quotation marks within the string.
				columns[i] = columns[i].replace("\"\"", "\"");
			}
		}
		return columns;
	}
}
