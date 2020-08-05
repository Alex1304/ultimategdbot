package com.github.alex1304.ultimategdbot.api.util;

import java.time.Duration;

/**
 * Utilities to manipulate Duration objects.
 */
public final class DurationUtils {
	
	/**
	 * Formats a Duration into a human readable String.
	 * 
	 * @param time the duration to format
	 * @return the formatted duration
	 */
	public static String format(Duration time) {
		var result = (time.toDaysPart() > 0 ? time.toDaysPart() + "d " : "")
				+ (time.toHoursPart() > 0 ? time.toHoursPart() + "h " : "")
				+ (time.toMinutesPart() > 0 ? time.toMinutesPart() + "min " : "")
				+ (time.toSecondsPart() > 0 ? time.toSecondsPart() + "s " : "")
				+ (time.toMillisPart() > 0 ? time.toMillisPart() + "ms " : "");
		return result.isEmpty() ? "0ms" : result.substring(0, result.length() - 1);
	}
	
	private DurationUtils() {
		throw new AssertionError();
	}
}
