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

		int sign = 1;
		if (degreesString.startsWith("-")) {
			sign = -1;
			degreesString = degreesString.substring(1);
		}
		
		int decimalOffset = degreesString.indexOf(".");
		if (decimalOffset == -1) {
			degreesString = degreesString + ".000000";
			decimalOffset = degreesString.indexOf(".");
		}
		String beforeDecimal = degreesString.substring(0, decimalOffset);
		String afterDecimal = degreesString.substring(decimalOffset + 1);
		while (afterDecimal.length() < 6) {
			afterDecimal = afterDecimal + "0";
		}
		afterDecimal = afterDecimal.substring(0, 6);
		return sign * (1000000 * Integer.parseInt(beforeDecimal) + Integer.parseInt(afterDecimal));
	}
}
