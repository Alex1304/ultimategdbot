package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.NewsChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.exception.ManageWebhooksPrivilegeException;
import ultimategdbot.service.EmojiService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.function;

@RdiService
@ChatInputCommand(
        name = "follow",
        description = "Receive news in your server about in-game events and bot announcements."
)
public final class FollowCommand implements ChatInputInteractionListener {

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    private final Map<String, Set<NewsChannel>> channelIdsByFeed;
    private final EmojiService emojis;

    private FollowCommand(Map<String, Set<NewsChannel>> channelIdsByFeed, EmojiService emojis) {
        this.channelIdsByFeed = channelIdsByFeed;
        this.emojis = emojis;
    }

    @RdiFactory
    public static Mono<FollowCommand> create(ConfigContainer configContainer, GatewayDiscordClient gateway,
                                             EmojiService emojis) {
        final var config = configContainer.get(UltimateGDBotConfig.class);
        return Flux
                .just(
                        Tuples.of(Options.ULTIMATEGDBOT_ANNOUNCEMENTS, config.botAnnouncementsChannelIds()),
                        Tuples.of(Options.NEW_RATED_LEVELS_NON_DEMONS, config.gd().events().ratesChannelIds()),
                        Tuples.of(Options.NEW_RATED_LEVELS_DEMONS, config.gd().events().demonsChannelIds()),
                        Tuples.of(Options.NEW_DAILY_WEEKLY_LEVELS, config.gd().events().timelyChannelId()
                                .map(Set::of)
                                .orElse(Set.of())),
                        Tuples.of(Options.NEW_GD_MODERATOR_PROMOTIONS_DEMOTIONS, config.gd().events().modsChannelId()
                                .map(Set::of)
                                .orElse(Set.of())))
                .flatMap(function((k, v) -> Flux.fromIterable(v)
                        .map(Snowflake::of)
                        .flatMap(gateway::getChannelById)
                        .ofType(NewsChannel.class)
                        .collect(Collectors.toUnmodifiableSet())
                        .map(channels -> Tuples.of(k, channels))))
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .map(map -> new FollowCommand(map, emojis));
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event()).flatMap(options -> Mono
                .justOrEmpty(channelIdsByFeed.get(options.newsFeed))
                .switchIfEmpty(Mono.error(new AssertionError("Unknown newsFeed type " + options.newsFeed)))
                .flatMapMany(Flux::fromIterable)
                .switchIfEmpty(Mono.error(() -> new InteractionFailedException(
                        ctx.translate(Strings.GENERAL, "error_follow_unavailable"))))
                .flatMap(channel -> channel.follow(options.targetChannel.getId())
                        .onErrorMap(ClientException.class, e -> new InteractionFailedException(
                                ctx.translate(Strings.GENERAL, "error_follow_failed", e.getErrorResponse()
                                        .map(r -> r.getFields().get("message"))
                                        .orElse("")))))
                .then(ctx.event().createFollowup(emojis.get("success") + ' ' +
                        ctx.translate(Strings.GENERAL, "follow_success", options.newsFeed,
                                options.targetChannel.getMention()))));
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    @Override
    public Privilege privilege() {
        return ctx -> ((ChatInputInteractionContext) ctx).event().getOption("target-channel")
                .orElseThrow(AssertionError::new)
                .getValue()
                .orElseThrow(AssertionError::new)
                .asChannel()
                .ofType(GuildMessageChannel.class)
                .filterWhen(channel -> channel.getEffectivePermissions(ctx.user().getId())
                        .map(perms -> perms.contains(Permission.MANAGE_WEBHOOKS)))
                .switchIfEmpty(Mono.error(ManageWebhooksPrivilegeException::new))
                .then();
    }

    private static final class Options {

        private static final String ULTIMATEGDBOT_ANNOUNCEMENTS = "UltimateGDBot Announcements";
        private static final String NEW_RATED_LEVELS_NON_DEMONS = "New Rated Levels (non-Demons)";
        private static final String NEW_RATED_LEVELS_DEMONS = "New Rated Levels (Demons)";
        private static final String NEW_DAILY_WEEKLY_LEVELS = "New Daily/Weekly Levels";
        private static final String NEW_GD_MODERATOR_PROMOTIONS_DEMOTIONS = "New GD Moderator Promotions/Demotions";

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "news-feed",
                description = "The news feed to follow.",
                required = true,
                choices = {
                        @ChatInputCommandGrammar.Choice(
                                name = ULTIMATEGDBOT_ANNOUNCEMENTS,
                                stringValue = ULTIMATEGDBOT_ANNOUNCEMENTS),
                        @ChatInputCommandGrammar.Choice(
                                name = NEW_RATED_LEVELS_NON_DEMONS,
                                stringValue = NEW_RATED_LEVELS_NON_DEMONS),
                        @ChatInputCommandGrammar.Choice(
                                name = NEW_RATED_LEVELS_DEMONS,
                                stringValue = NEW_RATED_LEVELS_DEMONS),
                        @ChatInputCommandGrammar.Choice(
                                name = NEW_DAILY_WEEKLY_LEVELS,
                                stringValue = NEW_DAILY_WEEKLY_LEVELS),
                        @ChatInputCommandGrammar.Choice(
                                name = NEW_GD_MODERATOR_PROMOTIONS_DEMOTIONS,
                                stringValue = NEW_GD_MODERATOR_PROMOTIONS_DEMOTIONS)
                }
        )
        String newsFeed;

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.CHANNEL,
                name = "target-channel",
                description = "The channel where announcements will be sent to",
                required = true,
                channelTypes = Channel.Type.GUILD_TEXT
        )
        GuildChannel targetChannel;
    }
}
