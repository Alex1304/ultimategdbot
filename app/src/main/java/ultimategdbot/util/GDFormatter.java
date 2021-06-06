package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.AccessPolicy;
import ultimategdbot.Strings;

public final class GDFormatter {

	private GDFormatter() {
	}
	
	public static String formatPolicy(Translator tr, AccessPolicy policy) {
		return tr.translate(Strings.GD, "policy_" + policy.name().toLowerCase());
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
