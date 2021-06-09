package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.api.util.MessageTemplate;
import botrino.command.*;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.grammar.CommandGrammar;
import botrino.command.menu.InteractiveMenu;
import botrino.command.menu.PageNumberOutOfRangeException;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.reaction.ReactionEmoji;
import jdash.client.GDClient;
import jdash.common.LevelBrowseMode;
import jdash.events.object.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.event.ManualEventProducer;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.PrivilegeFactory;
import ultimategdbot.util.GDLevels;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandCategory(CommandCategory.GD)
@Alias("gdevents")
@TopLevelCommand
@RdiService
public final class GDEventsCommand implements ParentCommand {

    private final GDClient gdClient;
    private final ManualEventProducer eventProducer;
    private final EmojiService emoji;
    private final CommandService commandService;
    private final PrivilegeFactory privilegeFactory;
    private final ReactionEmoji reactionCross, reactionSuccess;

    private final CommandGrammar<DispatchArgs> grammar = CommandGrammar.builder()
            .nextArgument("eventName")
            .beginOptionalArguments()
            .nextArgument("levelId", ArgumentMapper.asLong())
            .build(DispatchArgs.class);

    private final CommandGrammar<DispatchAllArgs> grammar2 = CommandGrammar.builder()
            .nextArgument("levelId", ArgumentMapper.asLong())
            .build(DispatchAllArgs.class);

    @RdiFactory
    public GDEventsCommand(GDClient gdClient, ManualEventProducer eventProducer, EmojiService emoji,
                           CommandService commandService, PrivilegeFactory privilegeFactory) {
        this.gdClient = gdClient.withWriteOnlyCache();
        this.eventProducer = eventProducer;
        this.emoji = emoji;
        this.commandService = commandService;
        this.reactionCross = commandService.interactiveMenuFactory().getPaginationControls().getCloseEmoji();
        this.reactionSuccess = ReactionEmoji.custom(emoji.getEmojiManager().get("success"));
        this.privilegeFactory = privilegeFactory;
    }

    private Mono<Void> runDispatch(CommandContext ctx) {
        return grammar.resolve(ctx).flatMap(args -> {
            Mono<Object> eventToDispatch;
            switch (args.eventName) {
                case "daily_level_changed":
                    eventToDispatch = gdClient.getDailyLevelInfo()
                            .map(info -> ImmutableDailyLevelChange.of(info, info));
                    break;
                case "weekly_demon_changed":
                    eventToDispatch = gdClient.getWeeklyDemonInfo()
                            .map(info -> ImmutableWeeklyDemonChange.of(info, info));
                    break;
                default:
                    if (args.levelId == null) {
                        return Mono.error(new CommandFailedException(
                                ctx.translate(Strings.GD, "error_id_not_specified")));
                    }
                    switch (args.eventName) {
                        case "awarded_level_added":
                            eventToDispatch = gdClient.findLevelById(args.levelId).map(ImmutableAwardedAdd::of);
                            break;
                        case "awarded_level_removed":
                            eventToDispatch = gdClient.findLevelById(args.levelId).map(ImmutableAwardedRemove::of);
                            break;
                        case "awarded_level_updated":
                            eventToDispatch = gdClient.findLevelById(args.levelId)
                                    .map(level -> ImmutableAwardedUpdate.of(level, level));
                            break;
                        default:
                            return Mono.error(new CommandFailedException(
                                    ctx.translate(Strings.GD, "error_unknown_event", ctx.getPrefixUsed())));
                    }
            }
            return eventToDispatch.doOnNext(eventProducer::submit)
                    .then(ctx.channel().createMessage(emoji.get("success") + ' ' +
                            ctx.translate(Strings.GD, "dispatch_success")));
        }).then();
    }

    private Mono<Void> runDispatchAll(CommandContext ctx) {
        return grammar2.resolve(ctx).flatMap(args -> {
            final var maxPage = ctx.input().getFlag("max-page")
                    .filter(s -> s.matches("[0-9]{1,3}"))
                    .map(Integer::parseInt)
                    .orElse(10);
            if (maxPage < 1) {
			    return Mono.error(new CommandFailedException(ctx.translate(Strings.GD, "error_invalid_max_page")));
            }
            return Flux.range(0, maxPage + 1)
                    .concatMap(n -> n <= maxPage
                            ? gdClient.browseLevels(LevelBrowseMode.AWARDED, null, null, n)
                            : Mono.error(new CommandFailedException(
                            ctx.translate(Strings.GD, "error_max_page_reached", maxPage))))
                    .takeWhile(level -> level.id() != args.levelId)
                    .map(ImmutableAwardedAdd::of)
                    .collectList()
                    .doOnNext(Collections::reverse)
                    .flatMap(events -> {
                        final var lastPage = (events.size() - 1) / 10;
                        InteractiveMenu menu;
                        if (lastPage == 0) {
                            menu = commandService.interactiveMenuFactory()
                                    .create(paginateEvents(ctx, 0, 0, events).toCreateSpec()
                                            .contentOrElse(""))
                                    .closeAfterReaction(false)
                                    .addReactionItem(reactionCross, interaction ->
                                            Mono.fromRunnable(interaction::closeMenu));
                        } else {
                            menu = commandService.interactiveMenuFactory()
                                    .createPaginated((tr, page) ->
                                            Mono.just(paginateEvents(tr, page, lastPage, events)));
                        }
                        return menu.deleteMenuOnClose(true)
                                .addReactionItem(reactionSuccess, interaction -> {
                                    events.forEach(eventProducer::submit);
                                    return ctx.channel().createMessage(emoji.get("success") + ' ' +
                                            ctx.translate(Strings.GD, "dispatch_success_multi", events.size()))
                                            .then(Mono.fromRunnable(interaction::closeMenu));
                                })
                                .open(ctx);
                    });
        }).then();
    }

	private static MessageTemplate paginateEvents(Translator tr, int page, int lastPage,
                                                  List<? extends AwardedAdd> events) {
		PageNumberOutOfRangeException.check(page, lastPage);
		return MessageTemplate.builder()
                .setMessageContent(tr.translate(Strings.GD, "dispatch_list") + "\n\n" +
                        tr.translate(Strings.GENERAL, "page_x", page + 1, lastPage + 1) + '\n' +
                        events.stream()
                                .skip(page * 10L)
                                .limit(10)
                                .map(event -> Markdown.quote(GDLevels.format(event.addedLevel())))
                                .collect(Collectors.joining("\n")) + "\n\n" +
                        tr.translate(Strings.GD, "dispatch_confirm"))
                .build();
	}

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "gdevents_description"))
                .build();
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.botOwner();
    }

    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("dispatch", this::runDispatch)
                        .inheritFrom(this)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setSyntax(grammar.toString())
                                .setDescription(tr.translate(Strings.HELP, "gdevents_dispatch_description"))
                                .setBody(tr.translate(Strings.HELP, "gdevents_dispatch_body"))
                                .build())
                        .build(),
                Command.builder("dispatch_all", this::runDispatchAll)
                        .inheritFrom(this)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setSyntax(grammar2.toString())
                                .setDescription(tr.translate(Strings.HELP, "gdevents_dispatch_all_description"))
                                .addFlag(FlagInformation.builder()
                                        .setValueFormat("max-page")
                                        .setDescription(tr.translate(Strings.HELP, "gdevents_dispatch_flag_max_page"))
                                        .build())
                                .build())
                        .build()
        );
    }

    private static final class DispatchArgs {
        String eventName;
        @Nullable
        Long levelId;
    }

    private static final class DispatchAllArgs {
        long levelId;
    }
}
