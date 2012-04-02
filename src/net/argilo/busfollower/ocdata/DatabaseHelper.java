package net.argilo.busfollower.ocdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
	private static final String DATABASE_FOLDER = "/data/data/net.argilo.busfollower/databases";
	private static final String DATABASE_PATH = DATABASE_FOLDER + "/db";

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
		
		File file = new File(DATABASE_PATH);
		if (!file.exists()) {
			Log.d(TAG, "Attempting to write database file.");
		    InputStream is = context.getAssets().open("db");

		    // Copy the database into the destination
		    OutputStream os = new FileOutputStream(file);
		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = is.read(buffer)) > 0){
		        os.write(buffer, 0, length);
		    }
		    os.flush();

		    os.close();
		    is.close();
			Log.d(TAG, "Successfully wrote database file.");
		}
	}
}