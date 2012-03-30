package net.argilo.busfollower.ocdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
		    String line;
		    while ((line = in.readLine()) != null) {
		        Log.d(TAG, line);
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
