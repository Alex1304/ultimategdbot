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
}
