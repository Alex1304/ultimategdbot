package ultimategdbot.util;

import botrino.api.i18n.Translator;
import ultimategdbot.Strings;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

public enum EmbedType {
    USER_PROFILE(tr -> tr.translate(Strings.GD, "user_profile")),
    LEVEL_SEARCH_RESULT(tr -> tr.translate(Strings.GD, "search_result")),
    DAILY_LEVEL(tr -> tr.translate(Strings.GD, "daily")),
    WEEKLY_DEMON(tr -> tr.translate(Strings.GD, "weekly")),
    RATE(tr -> tr.translate(Strings.GD, "gdevents_title_rate")),
    UNRATE(tr -> tr.translate(Strings.GD, "gdevents_title_unrate")),
    MOD(tr -> tr.translate(Strings.GD, "gdevents_title_promoted")),
    UNMOD(tr -> tr.translate(Strings.GD, "gdevents_title_demoted"));

    private final Function<Translator, String> authorName;

    EmbedType(Function<Translator, String> authorName) {
        this.authorName = authorName;
    }

    public String getAuthorName(Translator tr) {
        return authorName.apply(tr);
    }

    public InputStream iconInputStream() {
        return Objects.requireNonNull(EmbedType.class
                .getResourceAsStream("/authorIcons/" + name().toLowerCase() + ".png"));
    }
}
