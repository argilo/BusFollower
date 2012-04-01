package net.argilo.busfollower.ocdata;

import java.io.Serializable;

import net.argilo.busfollower.R;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.maps.GeoPoint;

public class Stop implements Serializable {
	private static final long serialVersionUID = 1L;

	private String number = null;
	private String name = null;
	private int latitude;
	private int longitude;
	
	public Stop(Context context, SQLiteDatabase db, String number) {
		// Zero-pad the stop number to 4 digits.
		while (number.length() < 4) {
			number = "0" + number;
		}

		Cursor result = db.rawQuery("SELECT stop_name, stop_lat, stop_lon FROM stops WHERE stop_code = ?", new String[] { number });
		if (result.getCount() == 0) {
			throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
		}
		
		result.moveToFirst();
		if (result.getCount() == 1) {
			// Only use the name from the stop database if there's a single location
			// associated with the stop number. Otherwise we'll get it from the API later.
			name = result.getString(0);
		}

		// Average out the stop locations in case there are multiple entries 
		// (e.g. different platforms at a Transitway station)
		int avgLatitude = 0;
		int avgLongitude = 0;
		while (!result.isAfterLast()) {
			avgLatitude += result.getInt(1);
			avgLongitude += result.getInt(2);
			result.moveToNext();
		}
		latitude = avgLatitude / result.getCount();
		longitude = avgLongitude / result.getCount();

		this.number = number;
	}
	
	public String getNumber() {
		return number;
	}
	
	public String getName() {
		return name;
	}
	
	void setName(String name) {
		this.name = name;
	}
	
	public GeoPoint getLocation() {
		return new GeoPoint(latitude, longitude);
	}
}
