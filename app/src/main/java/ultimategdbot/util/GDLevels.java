package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.entity.GDLevel;
import jdash.common.entity.GDSong;
import jdash.graphics.DifficultyRenderer;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;

import java.io.ByteArrayInputStream;
import java.util.Map;

public final class GDLevels {

    public static final Map<Integer, String> GAME_VERSIONS = gameVersions();

    private GDLevels() {
        throw new AssertionError();
    }

    public static String format(GDLevel level) {
        return "__" + level.name() + "__ by " + level.creatorName().orElse("-") + " (" + level.id() + ")";
    }

    public static Mono<ByteArrayInputStream> getDifficultyImageForLevel(GDLevel level) {
        return Misc.imageStream(DifficultyRenderer.forLevel(level).render());
    }

    public static String formatGameVersion(int v) {
        if (v < 10) {
            return "<1.6";
        }
        if (GAME_VERSIONS.containsKey(v)) {
            return GAME_VERSIONS.get(v);
        }
        var vStr = String.format("%02d", v);
        if (vStr.length() <= 1) {
            return vStr;
        }
        return vStr.substring(0, vStr.length() - 1) + "." + vStr.charAt(vStr.length() - 1);
    }

    public static String coinsToEmoji(String emoji, int n, boolean shorten) {
        final var output = new StringBuilder();
        if (shorten) {
            if (n <= 0) {
                return "";
            }
            output.append(emoji);
            output.append(" x");
            output.append(n);
        } else {
            if (n <= 0) {
                return "-";
            }
            for (int i = 1; i <= n && i <= 3; i++) {
                output.append(emoji);
                output.append(" ");
            }
        }
        return output.toString();
    }

    public static String formatSong(Translator tr, GDSong song) {
        return "__" + song.title() + "__ by " + song.artist();
    }

    public static String formatSongExtra(Translator tr, GDSong song, String emojiPlay, String emojiDownload) {
        if (song.isOfficial()) {
            return tr.translate(Strings.GD, "song_native");
        }
        final var str = tr.translate(Strings.GD, "label_song_id") + ' ' + song.id() + " - " +
                tr.translate(Strings.GD, "label_song_size") + ' ' + song.size().orElseThrow() + "MB";
        if (song.isFromMusicLibrary()) {
            return str;
        }
        return str + '\n' + emojiPlay + " [" + tr.translate(Strings.GD, "play_on_ng") +
                "](https://www.newgrounds.com/audio/listen/" + song.id() + ")  " + emojiDownload + " [" +
                tr.translate(Strings.GD, "download_mp3") + "](" + song.downloadUrl().orElseThrow() + ')';
    }

    public static Tuple2<String, String> unknownSongParts(Translator tr) {
        return Tuples.of(":warning: " + tr.translate(Strings.GD, "song_unknown"),
                tr.translate(Strings.GD, "song_info_unavailable"));
    }

    public static Tuple2<String, String> bannedSongParts(Translator tr) {
        return Tuples.of(":warning: " + tr.translate(Strings.GD, "song_banned"),
                tr.translate(Strings.GD, "song_info_unavailable"));
    }

    private static Map<Integer, String> gameVersions() {
        return Map.of(10, "1.7", 11, "1.8");
    }
}
