package net.argilo.busfollower.ocdata;

import net.argilo.busfollower.R;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.maps.GeoPoint;

public class Stop {
	private String number;
	private String name;
	private GeoPoint location;
	
	public static Stop getStop(Context context, SQLiteDatabase db, String stopNumber) throws IllegalArgumentException {
		Cursor result = db.rawQuery("SELECT stop_code, stop_name, stop_lat, stop_lon FROM stops WHERE stop_code = ?", new String[] { stopNumber });
		if (result.getCount() == 0) {
			throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
		}
		result.moveToFirst();
		GeoPoint location = new GeoPoint(result.getInt(2), result.getInt(3));
		return new Stop(result.getInt(0), result.getString(1), location);
	}
	
	private Stop(int number, String name, GeoPoint location) {
		this.number = String.valueOf(number);
		// Zero-pad the stop number to 4 digits.
		while (this.number.length() < 4) {
			this.number = "0" + this.number;
		}
		this.name = name;
		this.location = location;
	}
	
	public String getNumber() {
		return number;
	}
	
	public String getName() {
		return name;
	}
	
	public GeoPoint getLocation() {
		return location;
	}
}
