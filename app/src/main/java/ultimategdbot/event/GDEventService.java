package ultimategdbot.event;

import botrino.api.config.ConfigContainer;
import botrino.api.i18n.Translator;
import botrino.api.util.MatcherFunction;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.entity.RestChannel;
import jdash.client.GDClient;
import jdash.events.GDEventLoop;
import jdash.events.object.*;
import jdash.events.producer.GDEventProducer;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.retry.Retry;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.database.GdAwardedLevelDao;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.database.UserSettings;
import ultimategdbot.database.UserSettingsDao;
import ultimategdbot.event.BroadcastResultCache.MessageId;
import ultimategdbot.exception.CreateMessage500Exception;
import ultimategdbot.service.DefaultTranslator;
import ultimategdbot.service.GDLevelService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.EmbedType;

import java.time.Duration;
import java.util.*;

import static botrino.api.util.MessageUtils.toMessageEditSpec;
import static reactor.function.TupleUtils.function;

@RdiService
public final class GDEventService {

    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Loggers.getLogger(GDEventService.class);

    private final GDClient gdClient;
    private final GDLevelService levelService;
    private final GDUserService userService;
    private final GdLinkedUserDao gdLinkedUserDao;
    private final GdAwardedLevelDao gdAwardedLevelDao;
    private final UserSettingsDao userSettingsDao;
    private final GatewayDiscordClient gateway;
    private final Translator tr;

    private final BroadcastResultCache broadcastResultCache = new BroadcastResultCache();
    private final @Nullable CrosspostQueue crosspostQueue;

    private final List<RestChannel> ratesChannels;
    private final List<RestChannel> demonsChannels;
    private final @Nullable RestChannel timelyChannel;
    private final @Nullable RestChannel modsChannel;
    private final UltimateGDBotConfig.GD.Events.RandomMessages publicRandomMessages;
    private final UltimateGDBotConfig.GD.Events.@Nullable RandomMessages dmRandomMessages;
    private final GDEventMapper eventMapper = new GDEventMapper();
    private long ratesChannelRotator;
    private long demonsChannelRotator;

    @RdiFactory
    public GDEventService(GDClient gdClient, GDLevelService levelService, GDUserService userService,
                          GdLinkedUserDao gdLinkedUserDao,
                          GdAwardedLevelDao gdAwardedLevelDao,
                          ConfigContainer configContainer, GatewayDiscordClient gateway,
                          DefaultTranslator tr, ManualEventProducer eventProducer, UserSettingsDao userSettingsDao) {
        this.gdClient = gdClient;
        this.levelService = levelService;
        this.userService = userService;
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.gdAwardedLevelDao = gdAwardedLevelDao;
        this.userSettingsDao = userSettingsDao;
        this.gateway = gateway;
        this.tr = tr;
        final var config = configContainer.get(UltimateGDBotConfig.class).gd().events();
        this.ratesChannels = config.ratesChannelIds().stream()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .toList();
        this.demonsChannels = config.demonsChannelIds().stream()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .toList();
        this.timelyChannel = config.timelyChannelId()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .orElse(null);
        this.modsChannel = config.modsChannelId()
                .map(v -> RestChannel.create(gateway.rest(), Snowflake.of(v)))
                .orElse(null);
        this.crosspostQueue = config.crosspost() ? new CrosspostQueue(tr) : null;
        this.publicRandomMessages = config.publicRandomMessages();
        this.dmRandomMessages = config.dmRandomMessages().orElse(null);
        GDEventLoop.builder(gdClient)
                .setEventProducers(Set.of(
                        GDEventProducer.awardedLevels(),
                        //GDEventProducer.awardedLists(),
                        GDEventProducer.dailyLevels(),
                        GDEventProducer.eventLevels(),
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
        final var gdEvent = eventMapper.get(event.getClass()).orElse(null);
        if (gdEvent == null) {
            LOGGER.warn("Unrecognized event type: {}", event.getClass().getName());
            return Mono.empty();
        }
        if (gdEvent.isUpdate()) {
            final var doUpdate = Mono.justOrEmpty(gdEvent.levelId(event).flatMap(broadcastResultCache::get))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(old -> gdEvent.createMessageTemplate(event)
                            .map(spec -> {
                                final var editSpec = toMessageEditSpec(spec)
                                        // Workaround to issue https://github.com/Discord4J/Discord4J#1334
                                        .withComponents(Possible.absent())
                                        .withAttachments(Possible.absent());
                                //noinspection ConstantConditions
                                if (editSpec.contentOrElse(null) == null) {
                                    return editSpec.withContent(Possible.absent());
                                }
                                return editSpec;
                            })
                            .flatMap(spec -> old.toMessage(gateway).flatMap(msg -> msg.edit(spec))))
                    .collectList()
                    .filter(results -> !results.isEmpty())
                    .doOnNext(results -> gdEvent.levelId(event).ifPresent(id -> cacheMessage(id, results)))
                    .then();
            return Mono.fromRunnable(() -> doUpdate.subscribe(null,
                    t -> LOGGER.error("Unable to update message for event " + event, t),
                    () -> LOGGER.info("Successfully updated message for event {}", event)));
        }
        final var sendGuild = Mono.defer(() -> gdEvent.createMessageTemplate(event))
                .flatMap(msg -> Mono.justOrEmpty(gdEvent.channel(event))
                        .flatMap(channel -> channel.createMessage(msg.asRequest()))
                        .map(data -> new Message(gateway, data)))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .filter(CreateMessage500Exception.class::isInstance))
                .doOnNext(msg -> {
                    if (crosspostQueue != null) {
                        crosspostQueue.submit(msg, event);
                    }
                });
        final var sendDm = dmRandomMessages == null ? Flux.<Message>empty() : gdEvent.recipientAccountId(event)
                .flatMapMany(gdLinkedUserDao::getDiscordAccountsForGDUser)
                .filterWhen(userId -> userSettingsDao.getById(userId).map(UserSettings::receiveDmOnEvent))
                .flatMap(userId -> gateway.getUserById(Snowflake.of(userId)))
                .flatMap(user -> user.getPrivateChannel()
                        .flatMap(channel -> gdEvent.createMessageTemplate(event)
                                .map(msg -> msg.withContent(gdEvent.congratMessage(event)))
                                .flatMap(channel::createMessage)))
                .onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.debug("Could not DM user for GD event", e)));
        return Flux.concat(sendGuild, sendDm)
                .collectList()
                .doOnNext(results -> gdEvent.levelId(event)
                        .ifPresent(id -> cacheMessage(id, results)))
                .then();
    }

    public void cacheMessage(long levelId, Snowflake channelId, Snowflake messageId) {
        broadcastResultCache.put(levelId, List.of(new MessageId(channelId, messageId)));
    }

    private void cacheMessage(long levelId, List<Message> results) {
        broadcastResultCache.put(levelId, results.stream()
                .map(msg -> new MessageId(msg.getChannelId(), msg.getId()))
                .toList());
    }

    @SuppressWarnings("DataFlowIssue")
    private class GDEventMapper extends ClassValue<Optional<GDEvent<?>>> {

        private GDEvent<?> awardedLevelAddEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<AwardedLevelAdd>builder()
                    .channel(event -> event.addedLevel().isDemon()
                            ? demonsChannels.isEmpty() ? null :
                            demonsChannels.get((int) (demonsChannelRotator++ % demonsChannels.size()))
                            : ratesChannels.isEmpty() ? null :
                            ratesChannels.get((int) (ratesChannelRotator++ % ratesChannels.size())))
                    .levelIdGetter(event -> Optional.of(event.addedLevel().id()))
                    .recipientAccountId(event -> gdAwardedLevelDao.saveEvent(event).then(gdClient
                            .searchUsers("" + event.addedLevel().creatorPlayerId(), 0)
                            .next()
                            .map(stats -> stats.user().accountId())))
                    .messageTemplateFactory(event -> levelService
                            .compactEmbed(tr, event.addedLevel(), EmbedType.RATE, null)
                            .map(function((EmbedCreateSpec embed,
                                           List<MessageCreateFields.File> files) -> MessageCreateSpec.create()
                                    .withContent(randomString(publicRandomMessages.rates()))
                                    .withEmbeds(embed)
                                    .withFiles(files))))
                    .congratMessage(event -> randomString(Objects.requireNonNull(dmRandomMessages).rates()))
                    .isUpdate(false)
                    .build();
        }

        private GDEvent<?> awardedLevelRemoveEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<AwardedLevelRemove>builder()
                    .channel(event -> event.removedLevel().isDemon()
                            ? demonsChannels.isEmpty() ? null :
                            demonsChannels.get((int) (demonsChannelRotator++ % demonsChannels.size()))
                            : ratesChannels.isEmpty() ? null :
                            ratesChannels.get((int) (ratesChannelRotator++ % ratesChannels.size())))
                    .levelIdGetter(event -> Optional.empty())
                    .recipientAccountId(event -> gdClient
                            .searchUsers("" + event.removedLevel().creatorPlayerId(), 0)
                            .next()
                            .map(stats -> stats.user().accountId()))
                    .messageTemplateFactory(event -> levelService
                            .compactEmbed(tr, event.removedLevel(), EmbedType.UNRATE, null)
                            .map(function((EmbedCreateSpec embed,
                                           List<MessageCreateFields.File> files) -> MessageCreateSpec.create()
                                    .withContent(randomString(publicRandomMessages.unrates()))
                                    .withEmbeds(embed)
                                    .withFiles(files))))
                    .congratMessage(event -> randomString(Objects.requireNonNull(dmRandomMessages).unrates()))
                    .isUpdate(false)
                    .build();
        }

        private GDEvent<?> awardedLevelUpdateEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<AwardedLevelUpdate>builder()
                    .channel(event -> {throw new UnsupportedOperationException();})
                    .levelIdGetter(event -> Optional.of(event.newData().id()))
                    .recipientAccountId(event -> gdClient
                            .searchUsers("" + event.newData().creatorPlayerId(), 0)
                            .next()
                            .map(stats -> stats.user().accountId()))
                    .messageTemplateFactory(event -> levelService
                            .compactEmbed(tr, event.newData(), EmbedType.RATE, null)
                            .map(function((EmbedCreateSpec embed,
                                           List<MessageCreateFields.File> files) -> MessageCreateSpec.create()
                                    .withEmbeds(embed)
                                    .withFiles(files))))
                    .congratMessage(event -> {throw new UnsupportedOperationException();})
                    .isUpdate(true)
                    .build();
        }

        private GDEvent<?> dailyLevelChangeEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<DailyLevelChange>builder()
                    .channel(event -> timelyChannel)
                    .levelIdGetter(event -> Optional.empty())
                    .recipientAccountId(event -> (event.isWeekly() ?
                            gdClient.downloadWeeklyDemon() :
                            gdClient.downloadDailyLevel())
                            .map(dl -> dl.level().creatorPlayerId())
                            .flatMap(playerId -> gdClient.searchUsers("" + playerId, 0).next())
                            .map(stats -> stats.user().accountId()))
                    .messageTemplateFactory(event -> (event.isWeekly() ?
                            gdClient.withWriteOnlyCache().downloadWeeklyDemon() :
                            gdClient.withWriteOnlyCache().downloadDailyLevel())
                            .flatMap(dl -> levelService
                                    .compactEmbed(tr, dl.level(), event.isWeekly() ?
                                                    EmbedType.WEEKLY_DEMON : EmbedType.DAILY_LEVEL,
                                            event.after())
                                    .map(function((EmbedCreateSpec embed,
                                                   List<MessageCreateFields.File> files) -> MessageCreateSpec.create()
                                            .withContent(randomString(event.isWeekly() ?
                                                    publicRandomMessages.weekly() :
                                                    publicRandomMessages.daily()))
                                            .withEmbeds(embed)
                                            .withFiles(files)))))
                    .congratMessage(event -> randomString(event.isWeekly() ?
                            Objects.requireNonNull(dmRandomMessages).weekly() :
                            Objects.requireNonNull(dmRandomMessages).daily()))
                    .isUpdate(false)
                    .build();
        }

        private GDEvent<?> eventLevelChangeEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<EventLevelChange>builder()
                    .channel(event -> timelyChannel)
                    .levelIdGetter(event -> Optional.empty())
                    .recipientAccountId(event -> (gdClient.downloadEventLevel())
                            .map(dl -> dl.level().creatorPlayerId())
                            .flatMap(playerId -> gdClient.searchUsers("" + playerId, 0).next())
                            .map(stats -> stats.user().accountId()))
                    .messageTemplateFactory(event -> gdClient.withWriteOnlyCache().downloadEventLevel()
                            .flatMap(dl -> levelService
                                    .compactEmbed(tr, dl.level(), EmbedType.EVENT_LEVEL,
                                            event.after())
                                    .map(function((EmbedCreateSpec embed, List<MessageCreateFields.File> files) ->
                                            MessageCreateSpec.create()
                                                    .withContent(randomString(publicRandomMessages.event()))
                                                    .withEmbeds(embed)
                                                    .withFiles(files)))))
                    .congratMessage(event -> randomString(Objects.requireNonNull(dmRandomMessages).event()))
                    .isUpdate(false)
                    .build();
        }

        private GDEvent<?> modStatusUpdateEvent(Class<?> __) {
            return ImmutableGDEvent
                    .<ModStatusUpdate>builder()
                    .channel(event -> modsChannel)
                    .levelIdGetter(event -> Optional.empty())
                    .recipientAccountId(event -> Mono.just(event.user().user().accountId()))
                    .messageTemplateFactory(event -> userService
                            .buildProfile(tr, event.user(), event.type().embedType(), false)
                            .map(messageTemplate -> messageTemplate.withContent(
                                    randomString(event.type().selectList(publicRandomMessages)))))
                    .congratMessage(event -> randomString(
                            event.type().selectList(Objects.requireNonNull(dmRandomMessages))))
                    .isUpdate(false)
                    .build();
        }

        @Override
        protected Optional<GDEvent<?>> computeValue(Class<?> type) {
            return MatcherFunction.<GDEvent<?>>create()
                    .matchType(Class.class, AwardedLevelAdd.class::isAssignableFrom, this::awardedLevelAddEvent)
                    .matchType(Class.class, AwardedLevelRemove.class::isAssignableFrom, this::awardedLevelRemoveEvent)
                    .matchType(Class.class, AwardedLevelUpdate.class::isAssignableFrom, this::awardedLevelUpdateEvent)
                    .matchType(Class.class, DailyLevelChange.class::isAssignableFrom, this::dailyLevelChangeEvent)
                    .matchType(Class.class, EventLevelChange.class::isAssignableFrom, this::eventLevelChangeEvent)
                    .matchType(Class.class, ModStatusUpdate.class::isAssignableFrom, this::modStatusUpdateEvent)
                    .apply(type);
        }
    }
}
