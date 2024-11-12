package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.PrivateCommand;
import botrino.interaction.annotation.Subcommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import botrino.interaction.util.MessagePaginator;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import jdash.client.GDClient;
import jdash.common.LevelSearchMode;
import jdash.events.object.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.event.GDEventService;
import ultimategdbot.event.ManualEventProducer;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.PrivilegeFactory;
import ultimategdbot.util.GDLevels;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ultimategdbot.util.Interactions.paginationAndConfirmButtons;

@PrivateCommand
@ChatInputCommand(
        name = "gd-events",
        description = "Manage the GD event announcement system (Bot Owner only).",
        defaultMemberPermissions = Permission.ADMINISTRATOR,
        subcommands = {
                @Subcommand(
                        name = "dispatch",
                        description = "Manually dispatch a new GD event.",
                        listener = GDEventsCommand.Dispatch.class
                ),
                @Subcommand(
                        name = "dispatch-all",
                        description = "Dispatch new awarded events for all levels that have been rated after the " +
                                "specified one.",
                        listener = GDEventsCommand.DispatchAll.class
                )
        }
)
public final class GDEventsCommand {

    @RdiService
    public static final class Dispatch implements ChatInputInteractionListener {

        private final GDClient gdClient;
        private final ManualEventProducer eventProducer;
        private final EmojiService emoji;
        private final PrivilegeFactory privilegeFactory;
        private final GDEventService eventService;

        private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

        @RdiFactory
        public Dispatch(GDClient gdClient, ManualEventProducer eventProducer, EmojiService emoji,
                        PrivilegeFactory privilegeFactory, GDEventService eventService) {
            this.gdClient = gdClient;
            this.eventProducer = eventProducer;
            this.emoji = emoji;
            this.privilegeFactory = privilegeFactory;
            this.eventService = eventService;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return grammar.resolve(ctx.event())
                    .flatMap(options -> {
                        Mono<Object> eventToDispatch;
                        switch (options.eventName) {
                            case Options.DAILY_LEVEL_CHANGED -> eventToDispatch = gdClient.getDailyLevelInfo()
                                    .map(info -> new DailyLevelChange(info, info, false));
                            case Options.WEEKLY_DEMON_CHANGED -> eventToDispatch = gdClient.getWeeklyDemonInfo()
                                    .map(info -> new DailyLevelChange(info, info, true));
                            case Options.EVENT_LEVEL_CHANGED -> eventToDispatch = gdClient.getEventLevelInfo()
                                    .map(info -> new EventLevelChange(info, info));
                            default -> {
                                if (options.levelId == null) {
                                    return Mono.error(new InteractionFailedException(
                                            ctx.translate(Strings.GD, "error_id_not_specified")));
                                }
                                switch (options.eventName) {
                                    case Options.AWARDED_LEVEL_ADDED ->
                                            eventToDispatch = gdClient.findLevelById(options.levelId)
                                                    .map(AwardedLevelAdd::new);
                                    case Options.AWARDED_LEVEL_REMOVED ->
                                            eventToDispatch = gdClient.findLevelById(options.levelId)
                                                    .map(AwardedLevelRemove::new);
                                    case Options.AWARDED_LEVEL_UPDATED -> {
                                        if (options.channelId != null && options.messageId != null) {
                                            eventService.cacheMessage(options.levelId, Snowflake.of(options.channelId),
                                                    Snowflake.of(options.messageId));
                                        }
                                        eventToDispatch = gdClient.findLevelById(options.levelId)
                                                .map(level -> new AwardedLevelUpdate(level, level));
                                    }
                                    default -> {
                                        return Mono.error(new AssertionError());
                                    }
                                }
                            }
                        }
                        return eventToDispatch.doOnNext(eventProducer::submit)
                                .then(ctx.event().createFollowup(emoji.get("success") + ' ' +
                                        ctx.translate(Strings.GD, "dispatch_success")));
                    });
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return grammar.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }

        private record Options(
                @ChatInputCommandGrammar.Option(
                        type = ApplicationCommandOption.Type.STRING,
                        name = "event-name",
                        description = "The name of the event to dispatch.",
                        required = true,
                        choices = {
                                @ChatInputCommandGrammar.Choice(
                                        name = "Daily Level Changed",
                                        stringValue = DAILY_LEVEL_CHANGED
                                ),
                                @ChatInputCommandGrammar.Choice(
                                        name = "Weekly Demon Changed",
                                        stringValue = WEEKLY_DEMON_CHANGED
                                ),
                                @ChatInputCommandGrammar.Choice(
                                        name = "Event Level Changed",
                                        stringValue = EVENT_LEVEL_CHANGED
                                ),
                                @ChatInputCommandGrammar.Choice(
                                        name = "Awarded Level Added",
                                        stringValue = AWARDED_LEVEL_ADDED
                                ),
                                @ChatInputCommandGrammar.Choice(
                                        name = "Awarded Level Removed",
                                        stringValue = AWARDED_LEVEL_REMOVED
                                ),
                                @ChatInputCommandGrammar.Choice(
                                        name = "Awarded Level Updated",
                                        stringValue = AWARDED_LEVEL_UPDATED
                                )
                        }
                )
                String eventName,
                @ChatInputCommandGrammar.Option(
                        type = ApplicationCommandOption.Type.INTEGER,
                        name = "level-id",
                        description = "The ID of the level concerned by the event, if applicable."
                )
                @Nullable Long levelId,
                @ChatInputCommandGrammar.Option(
                        type = ApplicationCommandOption.Type.STRING,
                        name = "channel-id",
                        description = "If dispatching an Update event, the ID of the channel that contains the " +
                                "message to edit."
                )
                @Nullable String channelId,
                @ChatInputCommandGrammar.Option(
                        type = ApplicationCommandOption.Type.STRING,
                        name = "message-id",
                        description = "If dispatching an Update event, the ID of the message to edit."
                )
                @Nullable String messageId
        ) {
            private static final String DAILY_LEVEL_CHANGED = "daily_level_changed";
            private static final String WEEKLY_DEMON_CHANGED = "weekly_demon_changed";
            private static final String EVENT_LEVEL_CHANGED = "event_level_changed";
            private static final String AWARDED_LEVEL_ADDED = "awarded_level_added";
            private static final String AWARDED_LEVEL_REMOVED = "awarded_level_removed";
            private static final String AWARDED_LEVEL_UPDATED = "awarded_level_updated";
        }
    }

    @RdiService
    public static final class DispatchAll implements ChatInputInteractionListener {

        private final GDClient gdClient;
        private final ManualEventProducer eventProducer;
        private final EmojiService emoji;
        private final PrivilegeFactory privilegeFactory;

        private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

        @RdiFactory
        public DispatchAll(GDClient gdClient, ManualEventProducer eventProducer, EmojiService emoji,
                           PrivilegeFactory privilegeFactory) {
            this.gdClient = gdClient;
            this.eventProducer = eventProducer;
            this.emoji = emoji;
            this.privilegeFactory = privilegeFactory;
        }

        private static MessageCreateSpec paginateEvents(Translator tr, MessagePaginator.State state,
                                                        List<AwardedLevelAdd> events, String okId,
                                                        String cancelId) {
            return MessageCreateSpec.create()
                    .withContent(tr.translate(Strings.GD, "dispatch_list") + "\n\n" +
                            tr.translate(Strings.GENERAL, "page_x", state.getPage() + 1,
                                    state.getPageCount()) + '\n' +
                            events.stream()
                                    .skip(state.getPage() * 10L)
                                    .limit(10)
                                    .map(event -> Markdown.quote(GDLevels.format(event.addedLevel())))
                                    .collect(Collectors.joining("\n")) + "\n\n" +
                            tr.translate(Strings.GD, "dispatch_confirm"))
                    .withComponents(paginationAndConfirmButtons(tr, state, okId, cancelId));
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return grammar.resolve(ctx.event()).flatMap(options -> {
                if (options.maxPage < 1) {
                    return Mono.error(new InteractionFailedException(
                            ctx.translate(Strings.GD, "error_invalid_max_page")));
                }
                return Flux.range(0, (int) (options.maxPage + 1))
                        .concatMap(n -> n <= options.maxPage
                                ? gdClient.searchLevels(LevelSearchMode.AWARDED, null, null, n)
                                : Mono.error(new InteractionFailedException(
                                ctx.translate(Strings.GD, "error_max_page_reached", options.maxPage))))
                        .takeWhile(level -> level.id() != options.fromLevelId)
                        .map(AwardedLevelAdd::new)
                        .collectList()
                        .doOnNext(Collections::reverse)
                        .flatMap(events -> {
                            final var pageCount = (events.size() - 1) / 10 + 1;
                            final var okId = UUID.randomUUID().toString();
                            final var cancelId = UUID.randomUUID().toString();
                            return Mono.firstWithSignal(
                                    MessagePaginator.paginate(ctx, pageCount, state -> Mono.just(
                                            paginateEvents(ctx, state, events, okId, cancelId))),
                                    Mono.firstWithValue(ctx.awaitButtonClick(okId), ctx.awaitButtonClick(cancelId))
                                            .filter(okId::equals)
                                            .flatMap(__ -> {
                                                events.forEach(eventProducer::submit);
                                                return ctx.event().createFollowup(emoji.get("success") + ' ' +
                                                        ctx.translate(Strings.GD, "dispatch_success_multi",
                                                                events.size()));
                                            }));
                        });
            });
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return grammar.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }

        private static final class Options {
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.INTEGER,
                    name = "from-level-id",
                    description = "The ID of the level concerned by the event, if applicable.",
                    required = true
            )
            long fromLevelId;

            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.INTEGER,
                    name = "max-page",
                    description = "The maximum number of pages to load when searching for the selected level. Default" +
                            " is 10."
            )
            Long maxPage = 10L;
        }
    }
}
