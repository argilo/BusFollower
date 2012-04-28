package net.argilo.busfollower.ocdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
	private static final String DATABASE_FOLDER = "/data/data/net.argilo.busfollower/databases";
	private static final String DATABASE_PATH = DATABASE_FOLDER + "/db";
	private static final String DATABASE_PREFS = "DbPrefsFile";
	private static final int DATABASE_VERSION = 7; // Increment this whenever the DB is changed

	Context context;
	
	public DatabaseHelper(Context context) {
		this.context = context;
	}
	
	public SQLiteDatabase getReadableDatabase() throws SQLiteException {
		try {
			writeDatabaseIfNecessary();
		} catch (IOException e) {
			throw new SQLiteException("Couldn't write database file.");
		}
		return SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
	}

	private void writeDatabaseIfNecessary() throws IOException {
		File folder = new File(DATABASE_FOLDER);
		if (!folder.exists()) {
			if (!folder.mkdir()) {
				throw new IOException("Couldn't create folder " + DATABASE_FOLDER);
			}
		}
		
		SharedPreferences dbPrefs = context.getSharedPreferences(DATABASE_PREFS, Context.MODE_PRIVATE);
		int dbVersion = dbPrefs.getInt("dbVersion", 0);
		
		if (dbVersion != DATABASE_VERSION) {
			Log.d(TAG, "Attempting to write database file.");
		    InputStream is = context.getAssets().open("db");

		    // Copy the database into the destination
		    OutputStream os = new FileOutputStream(new File(DATABASE_PATH));
		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = is.read(buffer)) > 0) {
		        os.write(buffer, 0, length);
		    }
		    os.flush();

		    os.close();
		    is.close();
			Log.d(TAG, "Successfully wrote database file.");
			
			SharedPreferences.Editor editor = dbPrefs.edit();
			editor.putInt("dbVersion", DATABASE_VERSION);
			editor.commit();
			Log.d(TAG, "Wrote new database version number to preferences file.");
		}
	}
}