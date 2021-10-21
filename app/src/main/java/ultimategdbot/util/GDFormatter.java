package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.AccessPolicy;
import ultimategdbot.Strings;

public final class GDFormatter {

	private GDFormatter() {
	}
	
	public static String formatPolicy(Translator tr, AccessPolicy policy) {
        String str;
        switch (policy) {
            case NONE:
                str = "policy_none";
                break;
            case FRIENDS_ONLY:
                str = "policy_friends_only";
                break;
            case ALL:
                str = "policy_all";
                break;
            default:
                throw new AssertionError();
        }
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
