package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.PrivacySetting;
import ultimategdbot.Strings;

public final class GDFormatter {

	private GDFormatter() {
	}
	
	public static String formatPolicy(Translator tr, PrivacySetting policy) {
        String str = switch (policy) {
            case NONE -> "policy_none";
            case FRIENDS_ONLY -> "policy_friends_only";
            case ALL -> "policy_all";
        };
        return tr.translate(Strings.GD, str);
	}
	
	public static String formatCode(Object val, int n) {
		var sb = new StringBuilder("" + val);
		for (var i = sb.length() ; i <= n ; i++) {
			sb.insert(0, " ‌‌");
		}
		sb.insert(0, '`').append('`');
		return sb.toString();
	}

    public static String formatHumanReadable(long number) {
        final var units = new char[] {'K', 'M', 'B', 'T'};

        if (Math.abs(number) < 1000) {
            return String.valueOf(number);
        }

        var value = (double) number;
        var unitIndex = -1;

        while (Math.abs(value) >= 1000 && unitIndex < units.length - 1) {
            value /= 1000;
            unitIndex++;
        }

        final var intPart = (long) value;
        final var hasDecimal = Math.abs(intPart) < 10;

        if (hasDecimal) {
            return String.format("%.1f%c", Math.floor(value * 10) / 10, units[unitIndex]);
        } else {
            return String.format("%.0f%c", Math.floor(value), units[unitIndex]);
        }
    }
}
