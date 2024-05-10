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

    public Mono<MessageCreateSpec> buildProfile(Translator tr, GDUserProfile profile, EmbedType type) {
        final var user = profile.user();
        final var stats = profile.stats();
        return Mono.zip(db.gdLinkedUserDao()
                        .getDiscordAccountsForGDUser(user.accountId())
                        .flatMap(id -> gateway.withRetrievalStrategy(STORE_FALLBACK_REST).getUserById(Snowflake.of(id)))
                        .collectList(), makeIconSet(profile))
                .map(function((linkedAccounts, iconSet) -> {
                    final var role = user.role().orElse(Role.USER);
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
                            .addField(infoEntry("global_rank", tr.translate(Strings.GD, "label_global_rank"),
                                            profile.globalRank() == 0 ? italic("unranked") :
                                                    profile.globalRank()) , displayRole(role) +
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
