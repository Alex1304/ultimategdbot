package ultimategdbot.event;

import botrino.api.config.ConfigContainer;
import botrino.api.i18n.Translator;
import botrino.api.util.MatcherFunction;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.entity.RestChannel;
import jdash.client.GDClient;
import jdash.common.entity.GDLevel;
import jdash.common.entity.GDUser;
import jdash.events.GDEventLoop;
import jdash.events.object.*;
import jdash.events.producer.GDEventProducer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.DefaultTranslator;
import ultimategdbot.service.GDLevelService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.EmbedType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static botrino.api.util.MessageUtils.toMessageEditSpec;

@RdiService
public final class GDEventService {

    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Loggers.getLogger(GDEventService.class);

    private final GDClient gdClient;
    private final GDLevelService levelService;
    private final GDUserService userService;
    private final DatabaseService db;
    private final GatewayDiscordClient gateway;
    private final Translator tr;

    private final BroadcastResultCache broadcastResultCache = new BroadcastResultCache();
    private final CrosspostQueue crosspostQueue;

    private final List<RestChannel> ratesChannels;
    private final List<RestChannel> demonsChannels;
    private final RestChannel timelyChannel;
    private final RestChannel modsChannel;
    private final UltimateGDBotConfig.GD.Events.RandomMessages publicRandomMessages;
    private final UltimateGDBotConfig.GD.Events.RandomMessages dmRandomMessages;

    private long ratesChannelRotator;
    private long demonsChannelRotator;

    private final ClassValue<Optional<GDEvent<?>>> events = new ClassValue<>() {
        @Override
        protected Optional<GDEvent<?>> computeValue(Class<?> type) {
            return MatcherFunction.<GDEvent<?>>create()
                    .matchType(Class.class, AwardedAdd.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<AwardedAdd>builder()
                            .channel(event -> event.addedLevel().isDemon()
                                    ? demonsChannels.isEmpty() ? null :
                                    demonsChannels.get((int) (demonsChannelRotator++ % demonsChannels.size()))
                                    : ratesChannels.isEmpty() ? null :
                                    ratesChannels.get((int) (ratesChannelRotator++ % ratesChannels.size())))
                            .levelIdGetter(event -> Optional.of(event.addedLevel().id()))
                            .recipientAccountId(event -> db.gdAwardedLevelDao().saveEvent(event).then(gdClient
                                    .searchUsers("" + event.addedLevel().creatorPlayerId(), 0)
                                    .next()
                                    .map(GDUser::accountId)))
                            .messageTemplateFactory(event -> levelService
                                    .compactEmbed(tr, event.addedLevel(), EmbedType.RATE, null)
                                    .map(embed -> MessageCreateSpec.create()
                                            .withContent(randomString(publicRandomMessages.rates()))
                                            .withEmbeds(embed)))
                            .congratMessage(event -> randomString(dmRandomMessages.rates()))
                            .isUpdate(false)
                            .build())
                    .matchType(Class.class, AwardedRemove.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<AwardedRemove>builder()
                            .channel(event -> ratesChannels.isEmpty() ? null :
                                    ratesChannels.get((int) (ratesChannelRotator++ % ratesChannels.size())))
                            .levelIdGetter(event -> Optional.empty())
                            .recipientAccountId(event -> gdClient
                                    .searchUsers("" + event.removedLevel().creatorPlayerId(), 0)
                                    .next()
                                    .map(GDUser::accountId))
                            .messageTemplateFactory(event -> levelService
                                    .compactEmbed(tr, event.removedLevel(), EmbedType.UNRATE, null)
                                    .map(embed -> MessageCreateSpec.create()
                                            .withContent(randomString(publicRandomMessages.unrates()))
                                            .withEmbeds(embed)))
                            .congratMessage(event -> randomString(dmRandomMessages.unrates()))
                            .isUpdate(false)
                            .build())
                    .matchType(Class.class, AwardedUpdate.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<AwardedUpdate>builder()
                            .channel(event -> { throw new UnsupportedOperationException(); })
                            .levelIdGetter(event -> Optional.of(event.newData().id()))
                            .recipientAccountId(event -> gdClient
                                    .searchUsers("" + event.newData().creatorPlayerId(), 0)
                                    .next()
                                    .map(GDUser::accountId))
                            .messageTemplateFactory(event -> levelService
                                    .compactEmbed(tr, event.newData(), EmbedType.RATE, null)
                                    .map(embed -> MessageCreateSpec.create()
                                            .withEmbeds(embed)))
                            .congratMessage(event -> { throw new UnsupportedOperationException(); })
                            .isUpdate(true)
                            .build())
                    .matchType(Class.class, DailyLevelChange.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<DailyLevelChange>builder()
                            .channel(event -> timelyChannel)
                            .levelIdGetter(event -> Optional.empty())
                            .recipientAccountId(event -> gdClient.downloadDailyLevel()
                                    .map(GDLevel::creatorPlayerId)
                                    .flatMap(playerId -> gdClient.searchUsers("" + playerId, 0).next())
                                    .map(GDUser::accountId))
                            .messageTemplateFactory(event -> gdClient.withWriteOnlyCache().downloadDailyLevel()
                                    .flatMap(level -> levelService
                                            .compactEmbed(tr, level, EmbedType.DAILY_LEVEL, event.after())
                                            .map(embed -> MessageCreateSpec.create()
                                                    .withContent(randomString(publicRandomMessages.daily()))
                                                    .withEmbeds(embed))))
                            .congratMessage(event -> randomString(dmRandomMessages.daily()))
                            .isUpdate(false)
                            .build())
                    .matchType(Class.class, WeeklyDemonChange.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<WeeklyDemonChange>builder()
                            .channel(event -> timelyChannel)
                            .levelIdGetter(event -> Optional.empty())
                            .recipientAccountId(event -> gdClient.downloadWeeklyDemon()
                                    .map(GDLevel::creatorPlayerId)
                                    .flatMap(playerId -> gdClient.searchUsers("" + playerId, 0).next())
                                    .map(GDUser::accountId))
                            .messageTemplateFactory(event -> gdClient.withWriteOnlyCache().downloadWeeklyDemon()
                                    .flatMap(level -> levelService
                                            .compactEmbed(tr, level, EmbedType.WEEKLY_DEMON, event.after())
                                            .map(embed -> MessageCreateSpec.create()
                                                    .withContent(randomString(publicRandomMessages.weekly()))
                                                    .withEmbeds(embed))))
                            .congratMessage(event -> randomString(dmRandomMessages.weekly()))
                            .isUpdate(false)
                            .build())
                    .matchType(Class.class, ModStatusUpdate.class::isAssignableFrom, __ -> ImmutableGDEvent
                            .<ModStatusUpdate>builder()
                            .channel(event -> modsChannel)
                            .levelIdGetter(event -> Optional.empty())
                            .recipientAccountId(event -> Mono.just(event.user().accountId()))
                            .messageTemplateFactory(event -> userService
                                    .buildProfile(tr, event.user(), event.type().embedType())
                                    .map(messageTemplate -> messageTemplate.withContent(
                                            randomString(event.type().selectList(publicRandomMessages)))))
                            .congratMessage(event -> randomString(event.type().selectList(dmRandomMessages)))
                            .isUpdate(false)
                            .build())
                    .apply(type);
        }
    };

    @RdiFactory
    public GDEventService(GDClient gdClient, GDLevelService levelService, GDUserService userService,
                          ConfigContainer configContainer, DatabaseService db, GatewayDiscordClient gateway,
                          DefaultTranslator tr, ManualEventProducer eventProducer) {
        this.gdClient = gdClient;
        this.levelService = levelService;
        this.userService = userService;
        this.db = db;
        this.gateway = gateway;
        this.tr = tr;
        final var config = configContainer.get(UltimateGDBotConfig.class).gd().events();
        this.ratesChannels = config.ratesChannelIds().stream()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .collect(Collectors.toUnmodifiableList());
        this.demonsChannels = config.demonsChannelIds().stream()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .collect(Collectors.toUnmodifiableList());
        this.timelyChannel = config.timelyChannelId()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .orElse(null);
        this.modsChannel = config.modsChannelId()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .orElse(null);
        this.crosspostQueue = config.crosspost() ? new CrosspostQueue(tr) : null;
        this.publicRandomMessages = config.publicRandomMessages();
        this.dmRandomMessages = config.dmRandomMessages();
        GDEventLoop.builder(gdClient)
                .setEventProducers(Set.of(
                        GDEventProducer.awardedLevels(),
                        GDEventProducer.timelyLevels(),
                        eventProducer))
                .setInterval(Duration.ofSeconds(config.eventLoopIntervalSeconds()))
                .buildAndStart()
                .on(Object.class)
                .subscribe(new GDEventSubscriber(this));
    }

    private static String randomString(List<String> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    public Mono<Void> process(Object event) {
        final var gdEvent = events.get(event.getClass()).orElse(null);
        if (gdEvent == null) {
            LOGGER.warn("Unrecognized event type: {}", event.getClass().getName());
            return Mono.empty();
        }
        if (gdEvent.isUpdate()) {
            return Mono.justOrEmpty(gdEvent.levelId(event).flatMap(broadcastResultCache::get))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(old -> gdEvent.createMessageTemplate(event)
                            .map(msg -> {
                                // Make sure NOT to remove message content when editing only the embed
                                final var editSpec = toMessageEditSpec(msg);
                                //noinspection ConstantConditions
                                if (editSpec.contentOrElse(null) == null) {
                                    return editSpec.withContent(Possible.absent());
                                }
                                return editSpec;
                            })
                            .flatMapMany(old::edit))
                    .collectList()
                    .filter(results -> !results.isEmpty())
                    .doOnNext(results -> gdEvent.levelId(event).ifPresent(id -> broadcastResultCache.put(id, results)))
                    .then();
        }
        final var sendGuild = gdEvent.createMessageTemplate(event)
                .flatMap(msg -> Mono.justOrEmpty(gdEvent.channel(event))
                        .flatMap(channel -> channel.createMessage(msg.asRequest()))
                        .map(data -> new Message(gateway, data)))
                .doOnNext(msg -> {
                    if (crosspostQueue != null) {
                        crosspostQueue.submit(msg, event);
                    }
                });
        final var sendDm = gdEvent.recipientAccountId(event)
                .flatMapMany(db.gdLinkedUserDao()::getDiscordAccountsForGDUser)
                .flatMap(userId -> gateway.getUserById(Snowflake.of(userId)))
                .flatMap(user -> user.getPrivateChannel()
                        .flatMap(channel -> gdEvent.createMessageTemplate(event)
                                .map(msg -> msg.withContent(gdEvent.congratMessage(event)))
                                .flatMap(channel::createMessage)))
                .onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.debug("Could not DM user for GD event", e)));
        return Flux.concat(sendGuild, sendDm)
                .collectList()
                .doOnNext(results -> gdEvent.levelId(event).ifPresent(id -> broadcastResultCache.put(id, results)))
                .then();
    }
}
