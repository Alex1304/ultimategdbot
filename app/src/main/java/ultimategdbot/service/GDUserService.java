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
import jdash.common.IconType;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import jdash.graphics.GDUserIconSet;
import jdash.graphics.SpriteFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.util.EmbedType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
    private final SpriteFactory spriteFactory;
    private final GatewayDiscordClient gateway;

    @RdiFactory
    public GDUserService(DatabaseService db, EmojiService emoji, GDClient gdClient, SpriteFactory spriteFactory,
                         GatewayDiscordClient gateway) {
        this.db = db;
        this.emoji = emoji;
        this.gdClient = gdClient;
        this.spriteFactory = spriteFactory;
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

    private static Mono<ByteArrayInputStream> imageStream(BufferedImage img) {
        return Mono.fromCallable(() -> {
            final var os = new ByteArrayOutputStream(100_000);
            ImageIO.write(img, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<MessageCreateSpec> buildProfile(Translator tr, GDUserProfile gdUser, EmbedType type) {
        return Mono.zip(db.gdLinkedUserDao()
                        .getDiscordAccountsForGDUser(gdUser.accountId())
                        .flatMap(id -> gateway.withRetrievalStrategy(STORE_FALLBACK_REST).getUserById(Snowflake.of(id)))
                        .collectList(), makeIconSet(tr, gdUser))
                .map(function((linkedAccounts, iconSet) -> {
                    final var role = gdUser.role().orElse(Role.USER);
                    final var embed = EmbedCreateSpec.builder()
                            .author(type.getAuthorName(tr), null, type.getAuthorIconUrl())
                            .addField(":chart_with_upwards_trend:  " +
                                            tr.translate(Strings.GD, "player_stats", gdUser.name()),
                                    statEntry("star", gdUser.stars()) +
                                            statEntry("diamond", gdUser.diamonds()) +
                                            statEntry("user_coin", gdUser.userCoins()) +
                                            statEntry("secret_coin", gdUser.secretCoins()) +
                                            statEntry("demon", gdUser.demons()) +
                                            statEntry("creator_points", gdUser.creatorPoints()), false)
                            .addField("───────────", displayRole(role) +
                                            infoEntry("global_rank", tr.translate(Strings.GD, "label_global_rank"),
                                                    gdUser.globalRank() == 0 ? italic("unranked") :
                                                            gdUser.globalRank()) +
                                            infoEntry("youtube", "YouTube:", gdUser.youtube().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided"))
                                                    : '[' + tr.translate(Strings.GD, "open_link") + "]" +
                                                    "(https://www.youtube.com/channel/" + gdUser.youtube() + ')') +
                                            infoEntry("twitch", "Twitch:", gdUser.twitch().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided")) :
                                                    "[@" + gdUser.twitch() + "](https://www.twitch.tv/" + gdUser.twitch() +
                                                            ')') +
                                            infoEntry("twitter", "Twitter:", gdUser.twitter().isEmpty()
                                                    ? italic(tr.translate(Strings.GD, "not_provided"))
                                                    : "[@" + gdUser.twitter() + "](https://www.twitter.com/" +
                                                    gdUser.twitter() + ')') +
                                            infoEntry("discord", "Discord:", linkedAccounts.isEmpty()
                                                    ? italic(tr.translate(Strings.GENERAL, "unknown"))
                                                    : linkedAccounts.stream().map(User::getTag)
                                                    .collect(Collectors.joining(", "))) +
                                            "\n───────────\n" +
                                            infoEntry("friends", tr.translate(Strings.GD, "label_friend_requests"),
                                                    (gdUser.hasFriendRequestsEnabled()
                                                            ? tr.translate(Strings.GD, "enabled")
                                                            : tr.translate(Strings.GD, "disabled"))) +
                                            infoEntry("messages", tr.translate(Strings.GD, "label_private_messages"),
                                                    formatPolicy(tr, gdUser.privateMessagePolicy())) +
                                            infoEntry("comment_history", tr.translate(Strings.GD,
                                                            "label_comment_history"),
                                                    formatPolicy(tr, gdUser.commentHistoryPolicy())),
                                    false);
                    embed.footer(tr.translate(Strings.GD, "label_player_id") + ' ' + gdUser.playerId() + " | "
                            + tr.translate(Strings.GD, "label_account_id") + ' ' + gdUser.accountId(), null);
                    var spec = MessageCreateSpec.create();
                    if (iconSet.inputStream == null) {
                        Objects.requireNonNull(iconSet.error);
                        embed.addField(":warning: " + tr.translate(Strings.GD, "error_icon_set_failed"),
                                iconSet.error, false);
                    } else {
                        embed.image("attachment://icons.png");
                        spec = spec.withFiles(File.of("icons.png", iconSet.inputStream));
                    }
                    return spec.withEmbeds(embed.build());
                }));
    }

    public Mono<GeneratedIconSet> makeIconSet(Translator tr, GDUserProfile user) {
        return Mono.defer(() -> {
            final var iconSet = GDUserIconSet.create(user, spriteFactory);
            final var icons = new ArrayList<BufferedImage>();
            try {
                for (var iconType : IconType.values()) {
                    icons.add(iconSet.generateIcon(iconType));
                }
            } catch (IllegalArgumentException e) {
                return Mono.just(new GeneratedIconSet(null, e.getMessage()));
            }
            final var iconSetImg = new BufferedImage(icons.stream().mapToInt(BufferedImage::getWidth).sum(),
                    icons.get(0).getHeight(), icons.get(0).getType());
            final var g = iconSetImg.createGraphics();
            var offset = 0;
            for (var icon : icons) {
                g.drawImage(icon, offset, 0, null);
                offset += icon.getWidth();
            }
            g.dispose();
            return imageStream(iconSetImg).map(inputStream -> new GeneratedIconSet(inputStream, null));
        });
    }

    public Mono<GDUserProfile> stringToUser(Translator tr, String str) {
        final var gdClient = this.gdClient.withWriteOnlyCache();
        if (!str.matches("[a-zA-Z0-9 _-]+")) {
            return Mono.error(new InteractionFailedException(tr.translate(Strings.GD, "error_invalid_characters")));
        }
        return gdClient.searchUsers(str, 0).next()
                .filter(user -> user.accountId() > 0)
                .flatMap(user -> gdClient.getUserProfile(user.accountId()));
    }

    private String statEntry(String emojiName, int stat) {
        return emoji.get(emojiName) + "  " + formatCode(stat, 9) + '\n';
    }

    private String infoEntry(String emojiName, String label, Object value) {
        return emoji.get(emojiName) + "  **" + label + "** " + value + '\n';
    }

    private String displayRole(Role role) {
        return role == Role.USER ? "" : role == Role.MODERATOR
                ? emoji.get("mod") + " **MODERATOR**\n"
                : emoji.get("elder_mod") + " **ELDER MODERATOR**\n";
    }

    private static final class GeneratedIconSet {

        private final ByteArrayInputStream inputStream;
        private final String error;

        private GeneratedIconSet(@Nullable ByteArrayInputStream inputStream, @Nullable String error) {
            this.inputStream = inputStream;
            this.error = error;
        }
    }
}
