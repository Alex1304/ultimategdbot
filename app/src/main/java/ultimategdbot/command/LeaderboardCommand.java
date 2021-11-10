package ultimategdbot.command;

import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.util.MessagePaginator;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.store.action.read.ReadActions;
import discord4j.common.store.api.object.ExactResultNotAvailableException;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.exception.GDClientException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLeaderboard;
import ultimategdbot.database.GdLeaderboardBan;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.Leaderboards;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.Interactions.paginationButtons;

@ChatInputCommand(
        name = "leaderboard",
        description = "Show a leaderboard ranking members of this server based on in-game stats."
)
@RdiService
public final class LeaderboardCommand implements ChatInputInteractionListener {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public LeaderboardCommand(DatabaseService db, EmojiService emoji, GDUserService userService) {
        this.db = db;
        this.emoji = emoji;
        this.userService = userService;
    }

    private static List<Long> gdAccIds(List<GdLinkedUser> l) {
        return l.stream().map(GdLinkedUser::gdUserId).collect(Collectors.toList());
    }

    private static Flux<Member> getMembers(Guild guild) {
        return Flux.from(guild.getClient().getGatewayResources().getStore()
                        .execute(ReadActions.getExactMembersInGuild(guild.getId().asLong())))
                .map(data -> new Member(guild.getClient(), data, guild.getId().asLong()))
                .onErrorResume(ExactResultNotAvailableException.class,
                        e -> guild.getMemberCount() > 50_000 ?
                                Flux.error(new InteractionFailedException("Leaderboard command is temporarily " +
                                                "disabled in servers larger than 50k members.")) :
                                guild.requestMembers()
                        /*.index()
                        .map(t2 -> t2.getT1() + 1)
                        .filter(l -> l % 1000 == 0)
                        .elapsed()
                        .map(function((time, count) -> "1000 more members processed in " +
                                DurationUtils.format(Duration.ofMillis(time)) + ", total processed: " + count))
                        .log("LeaderboardCommand.requestMembers", Level.INFO, true)
                        .then(Mono.empty())*/);
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event()).flatMap(options -> {
            ToIntFunction<GdLeaderboard> stat;
            String statEmoji;
            boolean noBanList;
            switch (options.type) {
                case Options.STARS:
                    stat = GdLeaderboard::stars;
                    statEmoji = emoji.get("star");
                    noBanList = false;
                    break;
                case Options.DIAMONDS:
                    stat = GdLeaderboard::diamonds;
                    statEmoji = emoji.get("diamond");
                    noBanList = false;
                    break;
                case Options.USER_COINS:
                    stat = GdLeaderboard::userCoins;
                    statEmoji = emoji.get("user_coin");
                    noBanList = false;
                    break;
                case Options.SECRET_COINS:
                    stat = GdLeaderboard::secretCoins;
                    statEmoji = emoji.get("secret_coin");
                    noBanList = false;
                    break;
                case Options.DEMONS:
                    stat = GdLeaderboard::demons;
                    statEmoji = emoji.get("demon");
                    noBanList = false;
                    break;
                case Options.CREATOR_POINTS:
                    stat = GdLeaderboard::creatorPoints;
                    statEmoji = emoji.get("creator_points");
                    noBanList = true;
                    break;
                default:
                    return Mono.error(new AssertionError());
            }
            return ctx.event().getInteraction().getGuild().flatMap(guild -> getMembers(guild)
                    .collect(toMap(m -> m.getId().asLong(), User::getTag, (a, b) -> a))
                    .filter(not(Map::isEmpty))
                    .flatMap(members -> db.gdLinkedUserDao().getAllIn(List.copyOf(members.keySet()))
                            .collectList()
                            .filter(not(List::isEmpty))
                            .flatMap(linkedUsers -> Mono.zip(
                                            db.gdLeaderboardDao().getAllIn(gdAccIds(linkedUsers)).collectList(),
                                            db.gdLeaderboardBanDao().getAllIn(gdAccIds(linkedUsers))
                                                    .map(GdLeaderboardBan::accountId)
                                                    .collect(Collectors.toUnmodifiableSet()))
                                    .map(function((userStats, bans) -> userStats.stream()
                                            .filter(u -> noBanList || !bans.contains(u.accountId()))
                                            .flatMap(u -> linkedUsers.stream()
                                                    .filter(l -> l.gdUserId() == u.accountId())
                                                    .map(GdLinkedUser::discordUserId)
                                                    .map(members::get)
                                                    .map(tag -> new Leaderboards.LeaderboardEntry(
                                                            stat.applyAsInt(u), u, tag)))
                                            .collect(toCollection(TreeSet::new))
                                    ))))
                    .map(List::copyOf)
                    .defaultIfEmpty(List.of())
                    .flatMap(list -> {
                        if (list.size() <= Leaderboards.ENTRIES_PER_PAGE) {
                            return ctx.event().createFollowup()
                                    .withEmbeds(Leaderboards.embed(ctx, guild, list, 0, null, statEmoji))
                                    .then();
                        }
                        final var pageCount = ((list.size() - 1) / Leaderboards.ENTRIES_PER_PAGE) + 1;
                        return Mono.justOrEmpty(options.searchUser)
                                .flatMap(username -> userService.stringToUser(ctx, username))
                                .onErrorMap(GDClientException.class, e -> new InteractionFailedException(
                                        ctx.translate(Strings.GD, "error_user_fetch")))
                                .map(user -> {
                                    final var ids = list.stream()
                                            .map(entry -> entry.getStats().accountId())
                                            .collect(Collectors.toList());
                                    final var rank = ids.indexOf(user.accountId());
                                    if (rank == -1) {
                                        throw new InteractionFailedException(
                                                ctx.translate(Strings.GD, "error_user_not_on_lb"));
                                    }
                                    return Tuples.of(rank / Leaderboards.ENTRIES_PER_PAGE, user.name());
                                })
                                .defaultIfEmpty(Tuples.of(0, ""))
                                .flatMap(function((initialPage, highlighted) -> MessagePaginator.paginate(
                                        ctx, initialPage, pageCount, state -> Mono.just(MessageCreateSpec.create()
                                                .withEmbeds(Leaderboards.embed(ctx, guild, list, state.getPage(),
                                                        highlighted, statEmoji))
                                                .withComponents(paginationButtons(ctx, state))))));
                    })).then();
        }).then();
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    private static final class Options {

        private static final String STARS = "stars";
        private static final String DIAMONDS = "diamonds";
        private static final String DEMONS = "demons";
        private static final String USER_COINS = "user_coins";
        private static final String SECRET_COINS = "secret_coins";
        private static final String CREATOR_POINTS = "creator_points";

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "type",
                description = "The type of leaderboard to show.",
                required = true,
                choices = {
                        @ChatInputCommandGrammar.Choice(name = "Stars", stringValue = STARS),
                        @ChatInputCommandGrammar.Choice(name = "Diamonds", stringValue = DIAMONDS),
                        @ChatInputCommandGrammar.Choice(name = "Demons", stringValue = DEMONS),
                        @ChatInputCommandGrammar.Choice(name = "User Coins", stringValue = USER_COINS),
                        @ChatInputCommandGrammar.Choice(name = "Secret Coins", stringValue = SECRET_COINS),
                        @ChatInputCommandGrammar.Choice(name = "Creator Points", stringValue = CREATOR_POINTS),
                }
        )
        String type;

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "search-user",
                description = "Search for a specific user by GD username in the leaderboard."
        )
        String searchUser;
    }
}
