package ultimategdbot.command;

import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.Subcommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.database.ImmutableGdLeaderboardBan;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;

@ChatInputCommand(
        name = "leaderboard-mod",
        description = "Moderation command for the leaderboard feature (Bot Admin only).",
        defaultPermission = false,
        subcommands = {
                @Subcommand(
                        name = "ban",
                        description = "Ban a user from the leaderboards.",
                        listener = LeaderboardModCommand.Ban.class
                ),
                @Subcommand(
                        name = "unban",
                        description = "Unban a user from the leaderboards.",
                        listener = LeaderboardModCommand.Unban.class
                ),
                @Subcommand(
                        name = "is-banned",
                        description = "Check whether a user is banned from the leaderboards.",
                        listener = LeaderboardModCommand.IsBanned.class
                )
        }
)
public final class LeaderboardModCommand {

    private static final ChatInputCommandGrammar<Options> GRAMMAR = ChatInputCommandGrammar.of(Options.class);

    @RdiService
    public static final class Ban implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;
        private final GDUserService userService;

        @RdiFactory
        public Ban(PrivilegeFactory privilegeFactory, DatabaseService db, GDUserService userService) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
            this.userService = userService;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event())
                    .flatMap(options -> userService.stringToUser(ctx, options.gdUsername))
                    .flatMap(user -> db.gdLeaderboardBanDao().save(ImmutableGdLeaderboardBan.builder()
                                    .accountId(user.accountId())
                                    .bannedBy(ctx.user().getId().asLong())
                                    .build())
                            .then(ctx.event().createFollowup(ctx.translate(Strings.GD, "ban_success",
                                    user.name()))));
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return GRAMMAR.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botAdmin();
        }
    }

    @RdiService
    public static final class Unban implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;
        private final GDUserService userService;

        @RdiFactory
        public Unban(PrivilegeFactory privilegeFactory, DatabaseService db, GDUserService userService) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
            this.userService = userService;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event())
                    .flatMap(options -> userService.stringToUser(ctx, options.gdUsername))
                    .flatMap(user -> db.gdLeaderboardBanDao().delete(user.accountId())
                            .then(ctx.event().createFollowup(ctx.translate(Strings.GD, "unban_success",
                                    user.name()))));
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return GRAMMAR.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botAdmin();
        }
    }

    @RdiService
    public static final class IsBanned implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;
        private final GDUserService userService;

        @RdiFactory
        public IsBanned(PrivilegeFactory privilegeFactory, DatabaseService db, GDUserService userService) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
            this.userService = userService;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event())
                    .flatMap(options -> userService.stringToUser(ctx, options.gdUsername))
                    .flatMap(user -> db.gdLeaderboardBanDao().get(user.accountId())
                            .flatMap(ban -> ctx.event().getClient().getUserById(Snowflake.of(ban.bannedBy())))
                            .flatMap(bannedBy -> ctx.event()
                                    .createFollowup(ctx.translate(Strings.GD, "is_banned", user.name(),
                                            bannedBy.getTag())))
                            .switchIfEmpty(ctx.event()
                                    .createFollowup(ctx.translate(Strings.GD, "is_not_banned", user.name()))));
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return GRAMMAR.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botAdmin();
        }
    }

    private static final class Options {

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "gd-username",
                description = "The GD username of the target.",
                required = true
        )
        String gdUsername;
    }
}