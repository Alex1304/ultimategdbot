package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.MessageTemplate;
import botrino.command.*;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.CommandGrammar;
import botrino.command.menu.PageNumberOutOfRangeException;
import botrino.command.menu.UnexpectedReplyException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.store.action.read.ReadActions;
import discord4j.common.store.api.object.ExactResultNotAvailableException;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import jdash.client.exception.GDClientException;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.database.GDLeaderboard;
import ultimategdbot.database.GDLeaderboardBan;
import ultimategdbot.database.GDLinkedUser;
import ultimategdbot.database.ImmutableGDLeaderboardBan;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.service.PrivilegeFactory;
import ultimategdbot.util.GDFormatter;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static botrino.api.util.Markdown.bold;
import static botrino.api.util.Markdown.underline;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static reactor.function.TupleUtils.function;

@Alias({"leaderboard", "leaderboards", "top"})
@TopLevelCommand
@RdiService
public final class LeaderboardCommand implements Command {

	private static final int ENTRIES_PER_PAGE = 20;

	private final DatabaseService db;
    private final EmojiService emoji;
    private final CommandService commandService;
    private final GDUserService userService;
    private final PrivilegeFactory privilegeFactory;

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .beginOptionalArguments()
            .nextArgument("statName")
            .build(Args.class);
    private final CommandGrammar<BanArgs> banGrammar;

    @RdiFactory
    public LeaderboardCommand(DatabaseService db, EmojiService emoji, CommandService commandService,
                              GDUserService userService, PrivilegeFactory privilegeFactory) {
        this.db = db;
        this.emoji = emoji;
        this.commandService = commandService;
        this.userService = userService;
        this.banGrammar = CommandGrammar.builder()
                .nextArgument("gdUser", userService::stringToUser)
                .build(BanArgs.class);
        this.privilegeFactory = privilegeFactory;
    }

    private static EmbedCreateSpec leaderboardEmbed(Translator tr, String prefix, Guild guild,
                                                    List<LeaderboardEntry> entryList, int page,
                                                    @Nullable String highlighted, String emoji) {
        final var size = entryList.size();
		final var maxPage = (size - 1) / ENTRIES_PER_PAGE;
		final var offset = page * ENTRIES_PER_PAGE;
		final var subList = entryList.subList(offset, Math.min(offset + ENTRIES_PER_PAGE, size));
		var embed = EmbedCreateSpec.builder()
                .title(tr.translate(Strings.GD, "lb_title", guild.getName()));
		if (size == 0 || subList.isEmpty()) {
		    return embed.description(tr.translate(Strings.GD, "lb_no_entries")).build();
        }
		var sb = new StringBuilder();
        var rankWidth = (int) Math.log10(size) + 1;
        var statWidth = (int) Math.log10(subList.get(0).getValue()) + 1;
        final var maxRowLength = 100;
        for (var i = 1 ; i <= subList.size() ; i++) {
            var entry = subList.get(i - 1);
            var isHighlighted = entry.getStats().name().equalsIgnoreCase(highlighted);
            var rank = page * ENTRIES_PER_PAGE + i;
            if (isHighlighted) {
                sb.append("**");
            }
            var row = String.format("%s | %s %s | %s (%s)",
                    String.format("`#%" + rankWidth + "d`", rank).replaceAll(" ", " ‌‌"),
                    emoji,
                    GDFormatter.formatCode(entry.getValue(), statWidth),
                    entry.getStats().name(),
                    entry.getDiscordUser());
            if (row.length() > maxRowLength) {
                row = row.substring(0, maxRowLength - 3) + "...";
            }
            sb.append(row).append("\n");
            if (isHighlighted) {
                sb.append("**");
            }
        }
        embed.description("**" + tr.translate(Strings.GD, "lb_total_players", size, emoji) + "**\n\n" + sb);
        embed.addField("───────────",
                tr.translate(Strings.GD, "lb_account_notice", prefix), false);
        if (maxPage > 0) {
            embed.addField(tr.translate(Strings.APP, "page_x", page + 1, maxPage + 1),
                    tr.translate(Strings.APP, "page_instructions") + '\n' +
                    tr.translate(Strings.GD, "lb_jump_to_user"), false);
        }
		return embed.build();
    }

    private static List<Long> gdAccIds(List<GDLinkedUser> l) {
        return l.stream().map(GDLinkedUser::gdUserId).collect(Collectors.toList());
    }

    private static Flux<Member> getMembers(Guild guild) {
        return Flux.from(guild.getClient().getGatewayResources().getStore()
                .execute(ReadActions.getExactMembersInGuild(guild.getId().asLong())))
                .map(data -> new Member(guild.getClient(), data, guild.getId().asLong()))
                .onErrorResume(ExactResultNotAvailableException.class, e -> guild.requestMembers());
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return grammar.resolve(ctx).flatMap(args -> {
            if (args.statName == null) {
                return ctx.channel().createMessage(bold(ctx.translate(Strings.GD, "lb_intro")) + '\n' +
                        underline(ctx.translate(Strings.GD, "select_lb")) + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("star") + " Stars", ctx.getPrefixUsed(), "stars") + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("diamond") + " Diamonds", ctx.getPrefixUsed(), "diamonds") + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("user_coin") + " User Coins", ctx.getPrefixUsed(), "ucoins") + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("secret_coin") + " Secret Coins", ctx.getPrefixUsed(), "scoins") + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("demon") + " Demons", ctx.getPrefixUsed(), "demons") + '\n' +
                        ctx.translate(Strings.GD, "select_lb_item",
                                emoji.get("creator_points") + " Creator Points", ctx.getPrefixUsed(), "cp") + '\n')
                        .then();
            }
            ToIntFunction<GDLeaderboard> stat;
            String statEmoji;
            boolean noBanList;
            switch (args.statName.toLowerCase()) {
                case "stars":
                    stat = GDLeaderboard::stars;
                    statEmoji = emoji.get("star");
                    noBanList = false;
                    break;
                case "diamonds":
                    stat = GDLeaderboard::diamonds;
                    statEmoji = emoji.get("diamond");
                    noBanList = false;
                    break;
                case "ucoins":
                    stat = GDLeaderboard::userCoins;
                    statEmoji = emoji.get("user_coin");
                    noBanList = false;
                    break;
                case "scoins":
                    stat = GDLeaderboard::secretCoins;
                    statEmoji = emoji.get("secret_coin");
                    noBanList = false;
                    break;
                case "demons":
                    stat = GDLeaderboard::demons;
                    statEmoji = emoji.get("demon");
                    noBanList = false;
                    break;
                case "cp":
                    stat = GDLeaderboard::creatorPoints;
                    statEmoji = emoji.get("creator_points");
                    noBanList = true;
                    break;
                default:
                    return Mono.error(new CommandFailedException(ctx.translate(Strings.GD, "error_unknown_lb_type")));
		    }
		    return ctx.event().getGuild()
                    .flatMap(guild -> getMembers(guild)
                            .collect(toMap(m -> m.getId().asLong(), User::getTag, (a, b) -> a))
						    .filter(not(Map::isEmpty))
                            .flatMap(members -> db.gdLinkedUserDao().getAllIn(List.copyOf(members.keySet()))
                                    .collectList()
								    .filter(not(List::isEmpty))
                                    .flatMap(linkedUsers -> Mono.zip(
                                            db.gdLeaderboardDao().getAllIn(gdAccIds(linkedUsers)).collectList(),
                                            db.gdLeaderboardBanDao().getAllIn(gdAccIds(linkedUsers))
                                                    .map(GDLeaderboardBan::accountId)
                                                    .collect(Collectors.toUnmodifiableSet()))
                                            .map(function((userStats, bans) -> userStats.stream()
                                                    .filter(u -> noBanList || !bans.contains(u.accountId()))
                                                    .flatMap(u -> linkedUsers.stream()
                                                            .filter(l -> l.gdUserId() == u.accountId())
                                                            .map(GDLinkedUser::discordUserId)
                                                            .map(members::get)
                                                            .map(tag -> new LeaderboardEntry(
                                                                    stat.applyAsInt(u), u, tag)))
                                                    .collect(toCollection(TreeSet::new))
                                            ))))
                            .map(List::copyOf)
						    .defaultIfEmpty(List.of())
                            .flatMap(list -> {
                                if (list.size() <= ENTRIES_PER_PAGE) {
                                    return ctx.channel().createEmbed(leaderboardEmbed(ctx, ctx.getPrefixUsed(), guild,
                                            list, 0, null, statEmoji)).then();
                                }
							    final var highlighted = new AtomicReference<String>();
                                final IntFunction<MessageTemplate> templateGenerator = page -> MessageTemplate.builder()
                                            .setEmbed(leaderboardEmbed(ctx, ctx.getPrefixUsed(), guild, list, page,
                                                    highlighted.get(), statEmoji))
                                            .build();
                                return commandService.interactiveMenuFactory()
                                        .createPaginated((tr, page) -> {
                                            PageNumberOutOfRangeException.check(page,
                                                    (list.size() - 1) / ENTRIES_PER_PAGE);
                                            return Mono.just(templateGenerator.apply(page));
                                        })
                                        .addMessageItem("finduser", interaction -> Mono
                                                .just(interaction.getInput().getArguments().stream().skip(1)
                                                        .collect(Collectors.joining(" ")))
                                                .filter(not(String::isEmpty))
                                                .switchIfEmpty(ctx.channel()
                                                        .createMessage(ctx.translate(Strings.GD,
                                                                "error_username_not_specified"))
                                                        .then(Mono.error(new UnexpectedReplyException())))
                                                .flatMap(userName -> userService.stringToUser(ctx, userName))
                                                .onErrorResume(GDClientException.class, e -> ctx.channel()
                                                        .createMessage(ctx.translate(Strings.GD,
                                                                "error_user_fetch"))
                                                        .then(Mono.error(new UnexpectedReplyException())))
                                                .flatMap(gdUser -> {
                                                    final var ids = list.stream()
                                                            .map(entry -> entry.getStats().accountId())
                                                            .collect(Collectors.toList());
                                                    final var rank = ids.indexOf(gdUser.accountId());
                                                    if (rank == -1) {
                                                        return ctx.channel().createMessage(ctx.translate(Strings.GD,
                                                                    "error_user_not_on_lb"))
                                                                .then(Mono.error(new UnexpectedReplyException()));
                                                    }
                                                    final var jumpTo = rank / ENTRIES_PER_PAGE;
                                                    interaction.set("currentPage", jumpTo);
                                                    highlighted.set(gdUser.name());
                                                    return interaction.getMenuMessage()
                                                            .edit(templateGenerator.apply(jumpTo).toEditSpec())
                                                            .then();
                                                }))
                                        .open(ctx);
                            })).then();
        }).then();
    }

    private Mono<Void> runBan(CommandContext ctx) {
        return banGrammar.resolve(ctx)
                .flatMap(args -> db.gdLeaderboardBanDao()
                        .save(ImmutableGDLeaderboardBan.builder()
                                .accountId(args.gdUser.accountId())
                                .bannedBy(ctx.author().getId().asLong())
                                .build())
                        .then(ctx.channel()
                                .createMessage(ctx.translate(Strings.GD, "ban_success", args.gdUser.name()))))
                .then();
    }

    private Mono<Void> runUnban(CommandContext ctx) {
        return banGrammar.resolve(ctx)
                .flatMap(args -> db.gdLeaderboardBanDao()
                        .delete(args.gdUser.accountId())
                        .then(ctx.channel()
                                .createMessage(ctx.translate(Strings.GD, "unban_success", args.gdUser.name()))))
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "leaderboard_description"))
                .setBody(tr.translate(Strings.HELP, "leaderboard_body"))
                .build();
    }

    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("ban", this::runBan)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setSyntax(grammar.toString())
                                .setDescription(tr.translate(Strings.HELP, "leaderboard_ban_description"))
                                .setBody(tr.translate(Strings.HELP, "leaderboard_ban_body"))
                                .build())
                        .setPrivilege(privilegeFactory.botAdmin())
                        .build(),
                Command.builder("unban", this::runUnban)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setSyntax(grammar.toString())
                                .setDescription(tr.translate(Strings.HELP, "leaderboard_unban_description"))
                                .setBody(tr.translate(Strings.HELP, "leaderboard_unban_body"))
                                .build())
                        .setPrivilege(privilegeFactory.botAdmin())
                        .build()
        );
    }

    @Override
    public Scope scope() {
        return Scope.GUILD_ONLY;
    }

	private static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
		private final int value;
		private final GDLeaderboard stats;
		private final String discordUser;

		public LeaderboardEntry(int value, GDLeaderboard stats, String discordUser) {
			this.value = value;
			this.stats = Objects.requireNonNull(stats);
			this.discordUser = Objects.requireNonNull(discordUser);
		}

		public int getValue() {
			return value;
		}

		public GDLeaderboard getStats() {
			return stats;
		}

		public String getDiscordUser() {
			return discordUser;
		}

		@Override
		public int compareTo(LeaderboardEntry o) {
			return value == o.value ? stats.name().compareToIgnoreCase(o.stats.name()) : o.value - value;
		}

		@Override
		public String toString() {
			return "LeaderboardEntry{" + stats.name() + ": " + value + "}";
		}
	}

	private static final class Args {
        String statName;
    }

	private static final class BanArgs {
        GDUserProfile gdUser;
    }
}
