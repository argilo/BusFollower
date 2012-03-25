package net.argilo.busfollower.ocdata;

public class BusType {
	private String busType;
	
	public BusType(String busType) {
		this.busType = busType;
	}
	
	public int getLength() {
		if (busType.contains("4") && !busType.contains("6")) {
			return 40;
		} else if (busType.contains("6") && !busType.contains("4")) {
			return 60;
		} else {
			return -1;
		}
	}

	public boolean hasBikeRack() {
		return busType.contains("B");
	}
	
	public boolean isHybrid() {
		return busType.contains("DEH");
	}
	
	public boolean isDoubleDecker() {
		return busType.contains("DD");
	}
}
