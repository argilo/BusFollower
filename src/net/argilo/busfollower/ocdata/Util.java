package net.argilo.busfollower.ocdata;

public class Util {
	public static int latStringToMicroDegrees(String degreesString) throws NumberFormatException {
		int result = stringToMicroDegrees(degreesString);
		if (result < -90000000 || result > 90000000) {
			throw new NumberFormatException();
		} else {
			return result;
		}
	}
	
	public static int lonStringToMicroDegrees(String degreesString) throws NumberFormatException {
		int result = stringToMicroDegrees(degreesString);
		if (result < -180000000 || result > 180000000) {
			throw new NumberFormatException();
		} else {
			return result;
		}
	}

	private static int stringToMicroDegrees(String degreesString) throws NumberFormatException {
		if (degreesString == null) {
			throw new NumberFormatException();
		}
		degreesString = degreesString.trim();

		return Math.round(1000000 * Float.parseFloat(degreesString));
	}
}
