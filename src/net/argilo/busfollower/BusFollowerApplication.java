package net.argilo.busfollower;

import net.argilo.busfollower.ocdata.DatabaseHelper;
import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

public class BusFollowerApplication extends Application {
	private SQLiteDatabase db = null;
	
	@Override
	public void onCreate() {
        db = (new DatabaseHelper(this)).getReadableDatabase();
        // TODO: Catch & handle SQLiteException
	}
	
	public SQLiteDatabase getDatabase() {
		return db;
	}
}
