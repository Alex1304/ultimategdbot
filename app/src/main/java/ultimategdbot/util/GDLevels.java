package ultimategdbot.util;

import botrino.api.i18n.Translator;
import jdash.common.Length;
import jdash.common.entity.GDLevel;
import jdash.common.entity.GDSong;
import jdash.graphics.DifficultyRenderer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.service.EmojiService;

import java.io.ByteArrayInputStream;
import java.util.function.IntFunction;

import static ultimategdbot.util.GDFormatter.formatCode;

public final class GDLevels {

    private GDLevels() {
        throw new AssertionError();
    }

    public static String format(GDLevel level) {
        return "__" + level.name() + "__ by " + level.creatorName().orElse("-") + " (" + level.id() + ")";
    }

    public static Mono<ByteArrayInputStream> getDifficultyImageForLevel(GDLevel level, boolean forComponentsV2) {
        var image = DifficultyRenderer.forLevel(level).render();
        if (forComponentsV2) {
            image = ImageUtils.makeSquare(image);
        } else {
            image = image.getSubimage(0, 5, DifficultyRenderer.WIDTH, DifficultyRenderer.HEIGHT - 35);
        }
        return ImageUtils.imageStream(image);
    }

    public static String difficultySignatureForLevel(GDLevel level) {
        return (level.isDemon() ? level.demonDifficulty().name() : level.difficulty().name()) + "_"
                + level.rewards() + "_" + level.qualityRating().name();
    }

    public static String formatLevelHeader(EmojiService emoji, GDLevel level) {
        return emoji.get("play") + "  __" + level.name() + "__ by " + level.creatorName().orElse("-") +
                (level.originalLevelId().orElse(0L) > 0 ? ' ' + emoji.get("copy") : "") +
                (level.objectCount() > 40_000 ? ' ' + emoji.get("object_overflow") : "");
    }

    public static String formatCoins(EmojiService emoji, Translator tr, GDLevel level) {
        return tr.translate(Strings.GD, "label_coins") + ' ' +
                coinsToEmoji(emoji.get(level.hasCoinsVerified()
                        ? "user_coin" : "user_coin_unverified"), level.coinCount(), false);
    }

    public static String formatGameVersion(int v) {
        return switch (v) {
            case 1 -> "1.0";
            case 2 -> "1.1";
            case 3 -> "1.2";
            case 4 -> "1.3";
            case 5 -> "1.4";
            case 6 -> "1.5";
            case 7 -> "1.6";
            case 10 -> "1.7";
            case 11 -> "1.8";
            default -> {
                var vStr = String.format("%02d", v);
                if (vStr.length() <= 1) {
                    yield vStr;
                }
                yield vStr.substring(0, vStr.length() - 1) + "." + vStr.charAt(vStr.length() - 1);
            }
        };
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

    public static String formatSong(Translator tr, GDSong song, String emojiNcs) {
        return (song.isNCS() ? emojiNcs + ' ' : "") + "__" + song.title() + "__ by " + song.artist() +
                (song.otherArtistIds().isEmpty() ? "" : " +" + song.otherArtistIds().size());
    }

    public static String formatSongExtra(Translator tr, GDSong song, String emojiPlay, String emojiDownload) {
        if (song.isOfficial()) {
            return tr.translate(Strings.GD, "song_native");
        }
        final var str = tr.translate(Strings.GD, "label_song_id") + ' ' + song.id() + " - " +
                tr.translate(Strings.GD, "label_song_size") + ' ' + song.size().orElseThrow() + "MB";
        if (song.isFromMusicLibrary()) {
            return str + '\n' + tr.translate(Strings.GD, "song_from_music_library");
        }
        if (song.isFromNewgrounds() && song.downloadUrl().isPresent()) {
            return str + '\n' + emojiPlay + " [" + tr.translate(Strings.GD, "play_on_ng") +
                    "](https://www.newgrounds.com/audio/listen/" + song.id() + ")  " + emojiDownload + " [" +
                    tr.translate(Strings.GD, "download_mp3") + "](" + song.downloadUrl().orElseThrow() + ')';
        }
        return str;
    }

    public static Tuple2<String, String> unknownSongParts(Translator tr) {
        return Tuples.of(":warning: " + tr.translate(Strings.GD, "song_unknown"),
                tr.translate(Strings.GD, "song_info_unavailable"));
    }

    public static Tuple2<String, String> bannedSongParts(Translator tr) {
        return Tuples.of(":warning: " + tr.translate(Strings.GD, "song_banned"),
                tr.translate(Strings.GD, "song_info_unavailable"));
    }

    public static String formatDownloadsLikesLength(EmojiService emoji, GDLevel level) {
        final var width = 9;
        return emoji.get("downloads") + ' ' +
                formatCode(String.format("%,d", level.downloads()), width) + '\n' +
                emoji.get(level.likes() >= 0 ? "like" : "dislike") + ' ' +
                formatCode(String.format("%,d", level.likes()), width) + '\n' + emoji.get("length") + ' ' +
                formatCode(level.length() == Length.PLATFORMER ? "PLAT." : level.length().name(), width);
    }

    public static IntFunction<Flux<? extends GDLevel>> splittingSearchFunction(IntFunction<? extends Flux<?
            extends GDLevel>> original, int splitFactor, int limitPerPage) {
        return page -> original.apply(page / splitFactor)
                .collectList()
                .flatMapMany(list -> {
                    final var newPageLimit = Math.max(limitPerPage, list.size() / splitFactor);
                    return Flux.fromIterable(list)
                            .skip((long) (page % splitFactor) * newPageLimit)
                            .take(newPageLimit);
                });
    }

}
