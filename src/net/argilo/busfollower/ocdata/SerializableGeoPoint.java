package net.argilo.busfollower.ocdata;

import java.io.Serializable;

import com.google.android.maps.GeoPoint;

public class SerializableGeoPoint extends GeoPoint implements Serializable {
	private static final long serialVersionUID = 1L;

	public SerializableGeoPoint(int latitudeE6, int longitudeE6) {
		super(latitudeE6, longitudeE6);
	}
}
