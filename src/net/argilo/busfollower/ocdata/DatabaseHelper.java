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
		db.execSQL("CREATE TABLE stops (stop_id TEXT PRIMARY KEY, " +
				"stop_code INT, stop_name TEXT, stop_lat INT, stop_lon INT);");
		
		try {
		    BufferedReader in = new BufferedReader(new InputStreamReader(context.getAssets().open("stops.txt")));
		    String line = in.readLine();
		    String[] columns = line.split(",");
		    int stopCodeCol = -1, stopNameCol = -1, stopLatCol = -1, stopLonCol = -1;
		    for (int i = 0; i < columns.length; i++) {
		        if ("stop_code".equals(columns[i])) {
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
		        columns = line.split(",");
		        cv.put("stop_code", columns[stopCodeCol]);
		        cv.put("stop_name", columns[stopNameCol]);
                cv.put("stop_lat", columns[stopLatCol]);
                cv.put("stop_lon", columns[stopLonCol]);
                db.insert("stops", "stop_code", cv);
		    }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
