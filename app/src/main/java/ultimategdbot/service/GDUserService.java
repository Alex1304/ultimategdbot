package ultimategdbot.service;

import botrino.api.i18n.Translator;
import botrino.interaction.InteractionFailedException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields.File;
import discord4j.core.spec.MessageCreateSpec;
import jdash.client.GDClient;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import jdash.graphics.IconSetFactory;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.util.EmbedType;
import ultimategdbot.util.Misc;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static botrino.api.util.Markdown.italic;
import static discord4j.core.retriever.EntityRetrievalStrategy.STORE_FALLBACK_REST;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.GDFormatter.formatCode;
import static ultimategdbot.util.GDFormatter.formatPolicy;

@RdiService
public final class GDUserService {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDClient gdClient;
    private final GatewayDiscordClient gateway;

    @RdiFactory
    public GDUserService(DatabaseService db, EmojiService emoji, GDClient gdClient,
                         GatewayDiscordClient gateway) {
        this.db = db;
        this.emoji = emoji;
        this.gdClient = gdClient;
        this.gateway = gateway;
    }

    /**
     * Generates a random String made of alphanumeric characters. The length of the generated String is specified as an
     * argument.
     * <p>
     * The following characters are excluded to avoid confusion between l and 1, O and 0, etc: <code>l, I, 1, 0,
     * O</code>
     *
     * @param n the length of the generated String
     * @return the generated random String
     */
    public static String generateAlphanumericToken(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n is negative");
        }
        var rand = new Random();
        var alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        var result = new char[n];
        for (int i = 0; i < result.length; i++) {
            result[i] = alphabet.charAt(rand.nextInt(alphabet.length()));
        }
        return new String(result);
    }

    public Mono<MessageCreateSpec> buildProfile(Translator tr, GDUserProfile profile, EmbedType type,
                                                boolean showFull) {
        final var user = profile.user();
        final var stats = profile.stats();
        return Mono.zip(db.gdLinkedUserDao()
                        .getDiscordAccountsForGDUser(user.accountId())
                        .flatMap(id -> gateway.withRetrievalStrategy(STORE_FALLBACK_REST).getUserById(Snowflake.of(id)))
                        .collectList(), makeIconSet(profile))
                .map(function((linkedAccounts, iconSet) -> {
                    final var role = user.role().orElse(Role.USER);
                    final var rankEmoji = getRankEmoji(profile.globalRank());
                    final var embed = EmbedCreateSpec.builder()
                            .author(type.getAuthorName(tr), null, "attachment://author.png")
                            .addField(":chart_with_upwards_trend:  " +
                                            tr.translate(Strings.GD, "player_stats", user.name()),
                                    statEntry("star", stats.stars()) +
                                            statEntry("moon", stats.moons()) +
                                            statEntry("diamond", stats.diamonds()) +
                                            statEntry("user_coin", stats.userCoins()) +
                                            statEntry("secret_coin", stats.secretCoins()) +
                                            statEntry("demon", stats.demons()) +
                                            statEntry("creator_points", stats.creatorPoints()), false)
                            .addField(infoEntry(rankEmoji, tr.translate(Strings.GD, "label_global_rank"),
                                            profile.globalRank() == 0 ? italic("unranked") :
                                                    profile.globalRank()), displayRole(role) +
                                            "\n" +
                                            infoEntry("youtube", "YouTube:", profile.youtube().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided"))
                                                    : '[' + tr.translate(Strings.GD, "open_link") + "]" +
                                                    "(https://www.youtube.com/channel/" + profile.youtube() + ')') +
                                            infoEntry("twitch", "Twitch:", profile.twitch().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided")) :
                                                    "[@" + profile.twitch() + "](https://www.twitch.tv/" + profile.twitch() +
                                                            ')') +
                                            infoEntry("twitter", "Twitter:", profile.twitter().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided"))
                                                    : "[@" + profile.twitter() + "](https://www.twitter.com/" +
                                                    profile.twitter() + ')') +
                                            infoEntry("discord", "Discord:", linkedAccounts.isEmpty()
                                                    ? italic(tr.translate(Strings.GENERAL, "unknown"))
                                                    : linkedAccounts.stream().map(User::getTag)
                                                    .collect(Collectors.joining(", "))) +
                                            "\n" +
                                            infoEntry("friends", tr.translate(Strings.GD, "label_friend_requests"),
                                                    (profile.hasFriendRequestsEnabled()
                                                            ? tr.translate(Strings.GD, "enabled")
                                                            : tr.translate(Strings.GD, "disabled"))) +
                                            infoEntry("messages", tr.translate(Strings.GD, "label_private_messages"),
                                                    formatPolicy(tr, profile.privateMessagePrivacy())) +
                                            infoEntry("comment_history", tr.translate(Strings.GD,
                                                            "label_comment_history"),
                                                    formatPolicy(tr, profile.commentHistoryPrivacy())),
                                    false);
                    if (showFull) {
                        appendCompletedLevels(tr, embed, profile.completedClassicLevels().orElse(null),
                                profile.completedPlatformerLevels().orElse(null));
                        appendCompletedDemons(tr, embed, profile.completedDemons().orElse(null));
                    }
                    embed.footer(tr.translate(Strings.GD, "label_player_id") + ' ' + user.playerId() + " | "
                            + tr.translate(Strings.GD, "label_account_id") + ' ' + user.accountId(), null);
                    var message = MessageCreateSpec.builder();
                    message.addFile(File.of("author.png", type.iconInputStream()));
                    if (iconSet.inputStream == null) {
                        Objects.requireNonNull(iconSet.error);
                        embed.addField(":warning: " + tr.translate(Strings.GD, "error_icon_set_failed"),
                                iconSet.error, false);
                    } else {
                        embed.image("attachment://icons.png");
                        message.addFile(File.of("icons.png", iconSet.inputStream));
                    }
                    return message.addEmbed(embed.build()).build();
                }));
    }

    private void appendCompletedLevels(Translator tr, EmbedCreateSpec.Builder embed,
                                       @Nullable GDUserProfile.CompletedClassicLevels classic,
                                       @Nullable GDUserProfile.CompletedPlatformerLevels platformer) {
        if (classic == null && platformer == null) return;
        final var sb = new StringBuilder();
        completedLevelEntry(sb, "icon_auto",
                classic, GDUserProfile.CompletedClassicLevels::auto,
                platformer, GDUserProfile.CompletedPlatformerLevels::auto);
        completedLevelEntry(sb, "icon_easy",
                classic, GDUserProfile.CompletedClassicLevels::easy,
                platformer, GDUserProfile.CompletedPlatformerLevels::easy);
        completedLevelEntry(sb, "icon_normal",
                classic, GDUserProfile.CompletedClassicLevels::normal,
                platformer, GDUserProfile.CompletedPlatformerLevels::normal);
        completedLevelEntry(sb, "icon_hard",
                classic, GDUserProfile.CompletedClassicLevels::hard,
                platformer, GDUserProfile.CompletedPlatformerLevels::hard);
        completedLevelEntry(sb, "icon_harder",
                classic, GDUserProfile.CompletedClassicLevels::harder,
                platformer, GDUserProfile.CompletedPlatformerLevels::harder);
        completedLevelEntry(sb, "icon_insane",
                classic, GDUserProfile.CompletedClassicLevels::insane,
                platformer, GDUserProfile.CompletedPlatformerLevels::insane);
        completedLevelEntry(sb, "daily",
                classic, GDUserProfile.CompletedClassicLevels::daily,
                null, __ -> 0);
        completedLevelEntry(sb, "gauntlet",
                classic, GDUserProfile.CompletedClassicLevels::gauntlet,
                platformer, GDUserProfile.CompletedPlatformerLevels::gauntlet);
        final var totalClassic = classic == null ? 0 : classic.total();
        final var totalPlatformer = platformer == null ? 0 : platformer.total();
        embed.addField(tr.translate(Strings.GD, "completed_levels", totalClassic, totalPlatformer), sb.toString(),
                false);
    }

    private void appendCompletedDemons(Translator tr, EmbedCreateSpec.Builder embed,
                                       @Nullable GDUserProfile.CompletedDemons demons) {
        if (demons == null) return;
        final var sb = new StringBuilder();
        completedLevelEntry(sb, "icon_demon_easy",
                demons, GDUserProfile.CompletedDemons::easyClassic,
                demons, GDUserProfile.CompletedDemons::easyPlatformer);
        completedLevelEntry(sb, "icon_demon_medium",
                demons, GDUserProfile.CompletedDemons::mediumClassic,
                demons, GDUserProfile.CompletedDemons::mediumPlatformer);
        completedLevelEntry(sb, "icon_demon_hard",
                demons, GDUserProfile.CompletedDemons::hardClassic,
                demons, GDUserProfile.CompletedDemons::hardPlatformer);
        completedLevelEntry(sb, "icon_demon_insane",
                demons, GDUserProfile.CompletedDemons::insaneClassic,
                demons, GDUserProfile.CompletedDemons::insanePlatformer);
        completedLevelEntry(sb, "icon_demon_extreme",
                demons, GDUserProfile.CompletedDemons::extremeClassic,
                demons, GDUserProfile.CompletedDemons::extremePlatformer);
        completedLevelEntry(sb, "weekly",
                demons, GDUserProfile.CompletedDemons::weekly,
                null, __ -> 0);
        completedLevelEntry(sb, "gauntlet",
                demons, GDUserProfile.CompletedDemons::gauntlet,
                demons, GDUserProfile.CompletedDemons::gauntlet);
        embed.addField(tr.translate(Strings.GD, "completed_demons", demons.totalClassic(), demons.totalPlatformer()),
                sb.toString(), false);
    }

    private String getRankEmoji(int rank) {
        if (rank <= 0) return "global_rank";
        if (rank == 1) return "top_1";
        if (rank <= 10) return "top_10";
        if (rank <= 50) return "top_50";
        if (rank <= 100) return "top_100";
        if (rank <= 200) return "top_200";
        if (rank <= 500) return "top_500";
        if (rank <= 1000) return "top_1000";
        return "global_rank";
    }

    private Mono<GeneratedIconSet> makeIconSet(GDUserProfile user) {
        return Mono.defer(() -> {
            final var iconSet = IconSetFactory.forUser(user).createIconSet();
            return Misc.imageStream(iconSet).map(inputStream -> new GeneratedIconSet(inputStream, null));
        }).onErrorResume(e -> Mono.just(new GeneratedIconSet(null, e.getMessage())));
    }

    public Mono<GDUserProfile> stringToUser(Translator tr, String str) {
        final var gdClient = this.gdClient.withWriteOnlyCache();
        if (!str.matches("[a-zA-Z0-9 _-]+")) {
            return Mono.error(new InteractionFailedException(tr.translate(Strings.GD, "error_invalid_characters")));
        }
        return gdClient.searchUsers(str, 0).next()
                .filter(stats -> stats.user().accountId() > 0)
                .flatMap(stats -> gdClient.getUserProfile(stats.user().accountId()));
    }

    private String statEntry(String emojiName, int stat) {
        return emoji.get(emojiName) + "  " + formatCode(stat, 9) + '\n';
    }

    private String infoEntry(String emojiName, String label, Object value) {
        return emoji.get(emojiName) + "  **" + label + "** " + value + '\n';
    }

    private <T, U> void completedLevelEntry(StringBuilder sb, String emojiName,
                                            @Nullable T classic, ToIntFunction<T> classicV,
                                            @Nullable U platformer, ToIntFunction<U> platformerV) {
        sb.append(emoji.get(emojiName)).append("  ");
        if (classic != null) {
            sb.append("C: ").append(formatCode(classicV.applyAsInt(classic), 6));
            if (platformer != null) {
                sb.append("  ");
            }
        }
        if (platformer != null) {
            sb.append("P: ").append(formatCode(platformerV.applyAsInt(platformer), 6));
        }
        sb.append('\n');
    }

    private String displayRole(Role role) {
        return switch (role) {
            case USER -> "";
            case MODERATOR -> emoji.get("mod") + " **MODERATOR**\n";
            case ELDER_MODERATOR -> emoji.get("elder_mod") + " **ELDER MODERATOR**\n";
            case LEADERBOARD_MODERATOR -> emoji.get("leaderboard_mod") + " **LEADERBOARD MODERATOR**\n";
        };
    }

    private record GeneratedIconSet(@Nullable ByteArrayInputStream inputStream, @Nullable String error) {}
}
