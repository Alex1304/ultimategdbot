package ultimategdbot.service;

import botrino.api.i18n.Translator;
import botrino.api.util.DurationUtils;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.util.MessagePaginator;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields.File;
import discord4j.core.spec.MessageCreateSpec;
import jdash.client.GDClient;
import jdash.client.exception.ActionFailedException;
import jdash.client.exception.GDClientException;
import jdash.client.request.GDRequests;
import jdash.common.entity.GDDailyInfo;
import jdash.common.entity.GDLevel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.util.EmbedType;
import ultimategdbot.util.GDLevels;
import ultimategdbot.util.Interactions;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static botrino.api.util.Markdown.*;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.GDFormatter.formatCode;
import static ultimategdbot.util.GDLevels.*;

@RdiService
public final class GDLevelService {

    private final EmojiService emoji;
    private final GDClient gdClient;

    @RdiFactory
    public GDLevelService(EmojiService emoji, GDClient gdClient) {
        this.emoji = emoji;
        this.gdClient = gdClient;
    }

    public EmbedCreateSpec searchResultsEmbed(InteractionContext ctx, Iterable<? extends GDLevel> results, String title,
                                              int page) {
        final var embed = EmbedCreateSpec.builder();
        embed.title(title);
        var i = 1;
        for (final var level : results) {
            final var coins = coinsToEmoji(emoji.get(level.hasCoinsVerified()
                    ? "user_coin" : "user_coin_unverified"), level.coinCount(), true);
            final var song = level.song().map(s -> formatSong(ctx, s))
                    .orElse(":warning: " + ctx.translate(Strings.GD, "song_unknown"));
            embed.addField(String.format("`%02d` - %s %s | __**%s**__ by **%s** %s%s",
                            i,
                            emoji.get("play") + (level.rewards() > 0 ?
                                    " " + emoji.get("star") + " x" + level.rewards() : ""),
                            coins.equals("None") ? "" : ' ' + coins,
                            level.name(),
                            level.creatorName().orElse("-"),
                            level.originalLevelId().orElse(0L) > 0 ? emoji.get("copy") : "",
                            level.objectCount() > 40_000 ? emoji.get("object_overflow") : ""),
                    String.format("%s %d \t\t %s %d \t\t %s %s\n:musical_note:  **%s**\n _ _",
                            emoji.get("downloads"),
                            level.downloads(),
                            emoji.get(level.likes() >= 0 ? "like" : "dislike"),
                            level.likes(),
                            emoji.get("length"),
                            level.length(),
                            song), false);
            i++;
        }
        if (i == 1) {
            embed.description(italic(ctx.translate(Strings.GD, "no_results")));
        }
        embed.addField(ctx.translate(Strings.GENERAL, "page_number", page + 1),
                ctx.translate(Strings.GENERAL, "page_instructions"), false);
        return embed.build();
    }

    private Mono<Tuple2<EmbedCreateSpec, InputStream>> detailedEmbed(InteractionContext ctx, long levelId,
                                                                     String creatorName, EmbedType type,
                                                                     @Nullable GDDailyInfo timelyInfo) {
        return gdClient.downloadLevel(levelId)
                .zipWhen(dl -> extractSongParts(ctx, dl.level()))
                .map(function((dl, songParts) -> {
                    final var level = dl.level();
                    final var embed = EmbedCreateSpec.builder();
                    final var suffix = timelyInfo != null ? " #" + timelyInfo.number() : "";
                    embed.author(type.getAuthorName(ctx) + suffix, null, type.getAuthorIconUrl());
                    embed.thumbnail("attachment://difficulty.png");
                    final var title = emoji.get("play") + "  __" + level.name() + "__ by " +
                            level.creatorName().orElse(creatorName);
                    final var desc = bold(ctx.translate(Strings.GD, "label_description")) + ' ' +
                            (level.description().isEmpty()
                                    ? italic('(' + ctx.translate(Strings.GD, "no_description") + ')')
                                    : escape(level.description()));
                    final var coins = formatCoins(ctx, level);
                    final var downloadLikesLength = formatDownloadsLikesLength(level);
                    var objCount = bold(ctx.translate(Strings.GD, "label_object_count")) + ' ';
                    if (level.objectCount() > 0 || level.levelVersion() >= 21) {
                        if (level.objectCount() == 65535) {
                            objCount += ">";
                        }
                        objCount += level.objectCount();
                    } else {
                        objCount += italic(ctx.translate(Strings.GENERAL, "unknown"));
                    }
                    objCount += '\n';
                    final var extraInfo = new StringBuilder();
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_level_id"))).append(' ')
                            .append(level.id()).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_level_version"))).append(' ')
                            .append(level.levelVersion()).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_game_version"))).append(' ')
                            .append(formatGameVersion(level.gameVersion())).append('\n');
                    extraInfo.append(objCount);
                    var pass = "";
                    if (dl.copyPasscode().isEmpty() && dl.isCopyable()) {
                        pass = ctx.translate(Strings.GD, "free_to_copy");
                    } else if (dl.copyPasscode().isEmpty()) {
                        pass = ctx.translate(Strings.GENERAL, "no");
                    } else {
                        pass = ctx.translate(Strings.GD, "protected_copyable", emoji.get("lock"),
                                String.format("%06d", dl.copyPasscode().orElseThrow()));
                    }
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_copyable"))).append(' ')
                            .append(pass).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_uploaded"))).append(' ')
                            .append(ctx.translate(Strings.GENERAL, "ago", dl.uploadedAgo())).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_last_updated"))).append(' ')
                            .append(ctx.translate(Strings.GENERAL, "ago", dl.updatedAgo())).append('\n');
                    if (level.originalLevelId().orElse(0L) > 0) {
                        extraInfo.append(emoji.get("copy")).append(' ')
                                .append(bold(ctx.translate(Strings.GD, "label_original"))).append(' ')
                                .append(level.originalLevelId().orElseThrow()).append('\n');
                    }
                    if (level.objectCount() > 40_000) {
                        extraInfo.append(emoji.get("object_overflow")).append(' ')
                                .append(bold(ctx.translate(Strings.GD, "lag_notice"))).append('\n');
                    }
                    embed.addField(title, desc, false);
                    embed.addField(coins, downloadLikesLength + "\n_ _", false);
                    embed.addField(":musical_note:   " + songParts.getT1(),
                            songParts.getT2() + "\n_ _\n" + extraInfo, false);
                    return Tuples.of(dl.level(), embed.build());
                }))
                .flatMap(function((level, embed) -> getDifficultyImageForLevel(level)
                        .map(inputStream -> Tuples.of(embed, inputStream))));
    }

    public Mono<Tuple2<EmbedCreateSpec, InputStream>> compactEmbed(Translator tr, GDLevel level, EmbedType type,
                                                                   @Nullable GDDailyInfo timelyInfo) {
        return Mono.zip(extractSongParts(tr, level).map(Tuple2::getT1), getDifficultyImageForLevel(level))
                .map(function((songInfo, inputStream) -> {
                    final var embed = EmbedCreateSpec.builder();
                    final var suffix = timelyInfo != null ? " #" + timelyInfo.number() : "";
                    embed.author(type.getAuthorName(tr) + suffix, null, type.getAuthorIconUrl());
                    embed.thumbnail("attachment://difficulty.png");
                    final var title =
                            emoji.get("play") + "  __" + level.name() + "__ by " + level.creatorName().orElse("-") +
                                    (level.originalLevelId().orElse(0L) > 0 ? ' ' + emoji.get("copy") : "") +
                                    (level.objectCount() > 40_000 ? ' ' + emoji.get("object_overflow") : "");
                    final var coins = formatCoins(tr, level);
                    final var downloadLikesLength = formatDownloadsLikesLength(level);
                    embed.addField(title, downloadLikesLength, false);
                    embed.addField(coins, ":musical_note:   " + songInfo, false);
                    embed.footer(tr.translate(Strings.GD, "label_level_id") + ' ' + level.id(), null);
                    return Tuples.of(embed.build(), inputStream);
                }));
    }

    public Mono<Void> interactiveSearch(InteractionContext ctx, String title,
                                        IntFunction<? extends Flux<? extends GDLevel>> searchFunction) {
        final var resultsOfCurrentPage = new AtomicReference<List<? extends GDLevel>>();
        final var selectionMessageId = new AtomicReference<Snowflake>();
        final var selectMenuId = UUID.randomUUID().toString();
        return searchFunction.apply(0).collectList()
                .doOnNext(resultsOfCurrentPage::set)
                .flatMap(results -> results.size() == 1 ? sendSelectedSearchResult(ctx, results.get(0), null).then()
                        : Mono.firstWithSignal(
                        MessagePaginator.paginate(ctx, Integer.MAX_VALUE, state -> searchFunction
                                .apply(state.getPage())
                                .collectList()
                                .doOnNext(resultsOfCurrentPage::set)
                                .map(newResults -> MessageCreateSpec.create()
                                        .withEmbeds(searchResultsEmbed(ctx, newResults, title, state.getPage()))
                                        .withComponents(Interactions.paginationButtons(ctx, state), ActionRow.of(
                                                SelectMenu.of(selectMenuId, newResults.stream()
                                                                .map(level -> SelectMenu.Option.of(
                                                                        GDLevels.format(level).replaceAll("_", ""),
                                                                        level.id() + ""))
                                                                .toList())
                                                        .disabled(!state.isActive())
                                        ))
                                )),
                        ctx.awaitSelectMenuItems(selectMenuId)
                                .map(items -> resultsOfCurrentPage.get().stream()
                                        .filter(level -> level.id() == Long.parseLong(items.get(0)))
                                        .findAny()
                                        .orElseThrow())
                                .flatMap(level -> sendSelectedSearchResult(ctx, level,
                                        selectionMessageId.get()).doOnNext(selectionMessageId::set))
                                .repeat()
                                .then()
                ));
    }

    private Mono<Snowflake> sendSelectedSearchResult(InteractionContext ctx, GDLevel level,
                                                     @Nullable Snowflake selectionMessageId) {
        return detailedEmbed(ctx, level.id(), level.creatorName().orElse("-"), EmbedType.LEVEL_SEARCH_RESULT, null)
                .flatMap(function((embed, inputStream) -> selectionMessageId == null ?
                        ctx.event().createFollowup()
                                .withEmbeds(embed)
                                .withFiles(File.of("difficulty.png", inputStream)) :
                        ctx.event().editFollowup(selectionMessageId)
                                .withEmbeds(embed)
                                .withFiles(File.of("difficulty.png", inputStream))))
                .map(Message::getId);
    }

    public Mono<Message> sendTimelyInfo(InteractionContext ctx, boolean isWeekly) {
        final var gdClient = this.gdClient.withCacheDisabled();
        final var timelyMono = isWeekly ? gdClient.getWeeklyDemonInfo() : gdClient.getDailyLevelInfo();
        final var downloadId = isWeekly ? -2 : -1;
        final var type = isWeekly ? EmbedType.WEEKLY_DEMON : EmbedType.DAILY_LEVEL;
        return timelyMono
                .flatMap(timely -> detailedEmbed(ctx, downloadId, "-", type, timely)
                        .flatMap(function((embed, inputStream) -> ctx.event()
                                .createFollowup(ctx.translate(Strings.GD, "timely_of_today",
                                        type.getAuthorName(ctx), DurationUtils.format(timely.nextIn())))
                                .withEmbeds(embed)
                                .withFiles(File.of("difficulty.png", inputStream)))))
                .onErrorMap(e -> e instanceof GDClientException
                                && ((GDClientException) e).getRequest().getUri().equals(GDRequests.GET_GJ_DAILY_LEVEL)
                                && e.getCause() instanceof ActionFailedException,
                        e -> new InteractionFailedException(
                                ctx.translate(Strings.GD, "error_no_timely_set", type.getAuthorName(ctx))));
    }

    private Mono<Tuple2<String, String>> extractSongParts(Translator tr, GDLevel level) {
        return level.song().map(Mono::just)
                .or(() -> level.songId().map(gdClient::getSongInfo))
                .map(songMono -> songMono.map(s -> Tuples.of(formatSong(tr, s),
                        formatSongExtra(tr, s, emoji.get("play"), emoji.get("download_song")))))
                .orElseGet(() -> Mono.just(unknownSongParts(tr)))
                .onErrorReturn(e -> e instanceof GDClientException
                        && ((GDClientException) e).getRequest().getUri().equals(GDRequests.GET_GJ_SONG_INFO)
                        && e.getCause() instanceof ActionFailedException
                        && e.getCause().getMessage().equals("-2"), bannedSongParts(tr))
                .onErrorReturn(unknownSongParts(tr));
    }

    private String formatCoins(Translator tr, GDLevel level) {
        return tr.translate(Strings.GD, "label_coins") + ' ' +
                coinsToEmoji(emoji.get(level.hasCoinsVerified()
                        ? "user_coin" : "user_coin_unverified"), level.coinCount(), false);
    }

    private String formatDownloadsLikesLength(GDLevel level) {
        final var width = 9;
        return emoji.get("downloads") + ' ' +
                formatCode(level.downloads(), width) + '\n' +
                emoji.get(level.likes() >= 0 ? "like" : "dislike") + ' ' +
                formatCode(level.likes(), width) + '\n' + emoji.get("length") + ' ' +
                formatCode(level.length(), width);
    }

}
