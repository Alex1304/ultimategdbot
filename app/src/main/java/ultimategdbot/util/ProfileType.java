package ultimategdbot.util;

import botrino.api.i18n.Translator;
import ultimategdbot.Strings;

import java.util.function.Function;

public enum ProfileType {
    USER_PROMOTED(tr -> tr.translate(Strings.GD, "gdevproc_title_promoted"), "https://i.imgur.com/zY61GDD.png"),
    USER_DEMOTED(tr -> tr.translate(Strings.GD, "gdevproc_title_demoted"), "https://i.imgur.com/X53HV7d.png"),
    STANDARD(tr -> tr.translate(Strings.GD, "user_profile"), "https://i.imgur.com/ppg4HqJ.png");

    private final Function<Translator, String> authorName;
    private final String authorIconUrl;

    ProfileType(Function<Translator, String> authorName, String authorIconUrl) {
        this.authorName = authorName;
        this.authorIconUrl = authorIconUrl;
    }

    public String getAuthorName(Translator tr) {
        return authorName.apply(tr);
    }

    public String getAuthorIconUrl() {
        return authorIconUrl;
    }
}
