package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.entity.GDLevel;
import jdash.common.entity.GDSong;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GDLevels {

    public static final Map<String, String> DIFFICULTY_IMAGES = difficultyImages();
    public static final Map<Integer, String> GAME_VERSIONS = gameVersions();

    private GDLevels() {
        throw new AssertionError();
    }

    public static String format(GDLevel level) {
        return "__" + level.name() + "__ by " + level.creatorName().orElse("-") + " (" + level.id() + ")";
    }

    public static String getDifficultyImageForLevel(GDLevel level) {
        var difficulty = new StringBuilder();
        difficulty.append(level.stars()).append("-");
        if (level.isDemon()) {
            difficulty.append("demon-").append(level.demonDifficulty().toString().toLowerCase());
        } else if (level.isAuto()) {
            difficulty.append("auto");
        } else {
            difficulty.append(level.difficulty().toString().toLowerCase());
        }
        if (level.isEpic()) {
            difficulty.append("-epic");
        } else if (level.featuredScore() > 0) {
            difficulty.append("-featured");
        }
        return DIFFICULTY_IMAGES.getOrDefault(difficulty.toString(), "https://i.imgur.com/T3YfK5d.png");
    }

    public static String getDifficultyEmojiForLevel(GDLevel level) {
        var difficulty = new StringBuilder("icon_");
        if (level.isDemon()) {
            difficulty.append("demon_").append(level.demonDifficulty().toString().toLowerCase());
        } else if (level.isAuto()) {
            difficulty.append("auto");
        } else {
            difficulty.append(level.difficulty().toString().toLowerCase());
        }
        if (level.isEpic()) {
            difficulty.append("_epic");
        } else if (level.featuredScore() > 0) {
            difficulty.append("_featured");
        }
        return difficulty.toString();
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
            for (int i = 1 ; i <= n && i <= 3 ; i++) {
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
        return song.isCustom()
                ? tr.translate(Strings.GD, "label_song_id") + ' ' + song.id() + " - " +
                tr.translate(Strings.GD, "label_song_size") + ' ' + song.size().orElseThrow() + "MB\n" +
                emojiPlay + " [" + tr.translate(Strings.GD, "play_on_ng") +
                "](https://www.newgrounds.com/audio/listen/" + song.id() + ")  " + emojiDownload + " [" +
                tr.translate(Strings.GD, "download_mp3") + "](" + song.downloadUrl().orElseThrow() + ')'
                : tr.translate(Strings.GD, "song_native");
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

    private static Map<String, String> difficultyImages() {
        var map = new HashMap<String, String>();
        map.put("6-harder-featured", "https://i.imgur.com/b7J4AXi.png");
        map.put("0-insane-epic", "https://i.imgur.com/GdS2f8f.png");
        map.put("0-harder", "https://i.imgur.com/5lT74Xj.png");
        map.put("4-hard-epic", "https://i.imgur.com/toyo1Cd.png");
        map.put("4-hard", "https://i.imgur.com/XnUynAa.png");
        map.put("6-harder", "https://i.imgur.com/e499HCB.png");
        map.put("5-hard-epic", "https://i.imgur.com/W11eyJ9.png");
        map.put("6-harder-epic", "https://i.imgur.com/9x1ddvD.png");
        map.put("5-hard", "https://i.imgur.com/Odx0nAT.png");
        map.put("1-auto-featured", "https://i.imgur.com/DplWGja.png");
        map.put("5-hard-featured", "https://i.imgur.com/HiyX5DD.png");
        map.put("8-insane-featured", "https://i.imgur.com/PYJ5T0x.png");
        map.put("0-auto-featured", "https://i.imgur.com/eMwuWmx.png");
        map.put("8-insane", "https://i.imgur.com/RDVJDaO.png");
        map.put("7-harder-epic", "https://i.imgur.com/X3N5sm1.png");
        map.put("0-normal-epic", "https://i.imgur.com/VyV8II6.png");
        map.put("0-demon-hard-featured", "https://i.imgur.com/lVdup3A.png");
        map.put("8-insane-epic", "https://i.imgur.com/N2pjW2W.png");
        map.put("3-normal-epic", "https://i.imgur.com/S3PhlDs.png");
        map.put("0-normal-featured", "https://i.imgur.com/Q1MYgu4.png");
        map.put("2-easy", "https://i.imgur.com/yG1U6RP.png");
        map.put("0-hard-featured", "https://i.imgur.com/8DeaxfL.png");
        map.put("0-demon-hard-epic", "https://i.imgur.com/xLFubIn.png");
        map.put("1-auto", "https://i.imgur.com/Fws2s3b.png");
        map.put("0-demon-hard", "https://i.imgur.com/WhrTo7w.png");
        map.put("0-easy", "https://i.imgur.com/kWHZa5d.png");
        map.put("2-easy-featured", "https://i.imgur.com/Kyjevk1.png");
        map.put("0-insane-featured", "https://i.imgur.com/t8JmuIw.png");
        map.put("0-hard", "https://i.imgur.com/YV4Afz2.png");
        map.put("0-na", "https://i.imgur.com/T3YfK5d.png");
        map.put("7-harder", "https://i.imgur.com/dJoUDUk.png");
        map.put("0-na-featured", "https://i.imgur.com/C4oMYGU.png");
        map.put("3-normal", "https://i.imgur.com/cx8tv98.png");
        map.put("0-harder-featured", "https://i.imgur.com/n5kA2Tv.png");
        map.put("0-harder-epic", "https://i.imgur.com/Y7bgUu9.png");
        map.put("0-na-epic", "https://i.imgur.com/hDBDGzX.png");
        map.put("1-auto-epic", "https://i.imgur.com/uzYx91v.png");
        map.put("0-easy-featured", "https://i.imgur.com/5p9eTaR.png");
        map.put("0-easy-epic", "https://i.imgur.com/k2lJftM.png");
        map.put("0-hard-epic", "https://i.imgur.com/SqnA9kJ.png");
        map.put("3-normal-featured", "https://i.imgur.com/1v3p1A8.png");
        map.put("0-normal", "https://i.imgur.com/zURUazz.png");
        map.put("2-easy-epic", "https://i.imgur.com/wl575nH.png");
        map.put("7-harder-featured", "https://i.imgur.com/v50cZBZ.png");
        map.put("0-auto", "https://i.imgur.com/7xI8EOp.png");
        map.put("0-insane", "https://i.imgur.com/PeOvWuq.png");
        map.put("4-hard-featured", "https://i.imgur.com/VW4yufj.png");
        map.put("0-auto-epic", "https://i.imgur.com/QuRBnpB.png");
        map.put("10-demon-hard", "https://i.imgur.com/jLBD7cO.png");
        map.put("9-insane-featured", "https://i.imgur.com/byhPbgR.png");
        map.put("10-demon-hard-featured", "https://i.imgur.com/7deDmTQ.png");
        map.put("10-demon-hard-epic", "https://i.imgur.com/xtrTl4r.png");
        map.put("9-insane", "https://i.imgur.com/5VA2qDb.png");
        map.put("9-insane-epic", "https://i.imgur.com/qmfey5L.png");
        // Demon difficulties
        map.put("0-demon-medium-epic", "https://i.imgur.com/eEEzM6I.png");
        map.put("10-demon-medium-epic", "https://i.imgur.com/ghco42q.png");
        map.put("10-demon-insane", "https://i.imgur.com/nLZqoyQ.png");
        map.put("0-demon-extreme-epic", "https://i.imgur.com/p250YUh.png");
        map.put("0-demon-easy-featured", "https://i.imgur.com/r2WNVw0.png");
        map.put("10-demon-easy", "https://i.imgur.com/0zM0VuT.png");
        map.put("10-demon-medium", "https://i.imgur.com/lvpPepA.png");
        map.put("10-demon-insane-epic", "https://i.imgur.com/2BWY8pO.png");
        map.put("10-demon-medium-featured", "https://i.imgur.com/kkAZv5O.png");
        map.put("0-demon-extreme-featured", "https://i.imgur.com/4MMF8uE.png");
        map.put("0-demon-extreme", "https://i.imgur.com/v74cX5I.png");
        map.put("0-demon-medium", "https://i.imgur.com/H3Swqhy.png");
        map.put("0-demon-medium-featured", "https://i.imgur.com/IaeyGY4.png");
        map.put("0-demon-insane", "https://i.imgur.com/fNC1iFH.png");
        map.put("0-demon-easy-epic", "https://i.imgur.com/idesUcS.png");
        map.put("10-demon-easy-epic", "https://i.imgur.com/wUGOGJ7.png");
        map.put("10-demon-insane-featured", "https://i.imgur.com/RWqIpYL.png");
        map.put("10-demon-easy-featured", "https://i.imgur.com/fFq5lbN.png");
        map.put("0-demon-insane-featured", "https://i.imgur.com/1MpbSRR.png");
        map.put("0-demon-insane-epic", "https://i.imgur.com/ArGfdeh.png");
        map.put("10-demon-extreme", "https://i.imgur.com/DEr1HoM.png");
        map.put("0-demon-easy", "https://i.imgur.com/45GaxRN.png");
        map.put("10-demon-extreme-epic", "https://i.imgur.com/gFndlkZ.png");
        map.put("10-demon-extreme-featured", "https://i.imgur.com/xat5en2.png");
        //noinspection Java9CollectionFactory
        return Collections.unmodifiableMap(map);
    }
}
