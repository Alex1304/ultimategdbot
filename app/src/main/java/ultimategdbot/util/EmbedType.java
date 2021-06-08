package ultimategdbot.util;

import botrino.api.i18n.Translator;
import ultimategdbot.Strings;

import java.util.function.Function;

public enum EmbedType {
    USER_PROMOTED(tr -> tr.translate(Strings.GD, "gdevents_title_promoted"), "https://i.imgur.com/zY61GDD.png"),
    USER_DEMOTED(tr -> tr.translate(Strings.GD, "gdevents_title_demoted"), "https://i.imgur.com/X53HV7d.png"),
    USER_PROFILE(tr -> tr.translate(Strings.GD, "user_profile"), "https://i.imgur.com/ppg4HqJ.png"),
    LEVEL_SEARCH_RESULT(tr -> tr.translate(Strings.GD, "search_result"), "https://i.imgur.com/a9B6LyS.png"),
    DAILY_LEVEL(tr -> tr.translate(Strings.GD, "daily"), "https://i.imgur.com/enpYuB8.png"),
    WEEKLY_DEMON(tr -> tr.translate(Strings.GD, "weekly"), "https://i.imgur.com/kcsP5SN.png"),
    RATE(tr -> tr.translate(Strings.GD, "gdevents_title_rate"), "https://i.imgur.com/asoMj1W.png"),
    UNRATE(tr -> tr.translate(Strings.GD, "gdevents_title_unrate"), "https://i.imgur.com/fPECXUz.png"),
    MOD(tr -> tr.translate(Strings.GD, "gdevents_title_promoted"), "https://i.imgur.com/zY61GDD.png"),
    UNMOD(tr -> tr.translate(Strings.GD, "gdevents_title_demoted"), "https://i.imgur.com/X53HV7d.png");

    private final Function<Translator, String> authorName;
    private final String authorIconUrl;

    EmbedType(Function<Translator, String> authorName, String authorIconUrl) {
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
