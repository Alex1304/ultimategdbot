package com.github.alex1304.ultimategdbot.api.util;

/**
 * Enumerates system units (bytes, kilobytes, etc until terabytes)
 * Provides methods for conversion and formatting.
 */
public enum SystemUnit {
	BYTE,
	KILOBYTE,
	MEGABYTE,
	GIGABYTE,
	TERABYTE;
	
	/**
	 * Converts the given value to the unit represented by the current object
	 * 
	 * @param byteValue the value to convert
	 * @return double
	 */
	public double convert(long byteValue) {
		return byteValue / Math.pow(2, this.ordinal() * 10);
	}
	
	@Override
	public String toString() {
		return super.toString().charAt(0) + (this.ordinal() > 0 ? "B" : "");
	}
	
	/**
	 * Formats the given value to a human readable format, using the appropriate
	 * unit
	 * 
	 * @param byteValue the value to format
	 * @return String
	 */
	public static String format(long byteValue) {
		SystemUnit unit = BYTE;
		double convertedValue = byteValue;
		
		while (unit.ordinal() < values().length && (convertedValue = unit.convert(byteValue)) >= 1024)
			unit = values()[unit.ordinal() + 1];
		
		convertedValue = Math.round(convertedValue * 100) / 100.0;
		
		return String.format("%.2f %s", convertedValue, unit.toString());
	}
}
