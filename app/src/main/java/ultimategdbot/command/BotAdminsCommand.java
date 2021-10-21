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
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;

@ChatInputCommand(
        name = "bot-admins",
        description = "Manage users who are granted Bot Administrator permissions (Bot Owner only).",
        defaultPermission = false,
        subcommands = {
                @Subcommand(
                        name = "grant",
                        description = "Grant Bot Admin permission to a user.",
                        listener = BotAdminsCommand.Grant.class
                ),
                @Subcommand(
                        name = "revoke",
                        description = "Revoke Bot Admin permission from a user.",
                        listener = BotAdminsCommand.Revoke.class
                ),
                @Subcommand(
                        name = "view",
                        description = "View the list of users that currently have the Bot Admin permission.",
                        listener = BotAdminsCommand.View.class
                )
        }
)
public final class BotAdminsCommand {

    private static final ChatInputCommandGrammar<Options> GRAMMAR = ChatInputCommandGrammar.of(Options.class);

    @RdiService
    public static final class Grant implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;

        @RdiFactory
        public Grant(PrivilegeFactory privilegeFactory, DatabaseService db) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event()).flatMap(options -> db.botAdminDao()
                    .add(options.user.getId().asLong())
                    .then(ctx.event().createFollowup(
                            ctx.translate(Strings.GENERAL, "item_add_success", options.user.getTag()))));
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return GRAMMAR.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }
    }

    @RdiService
    public static final class Revoke implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;

        @RdiFactory
        public Revoke(PrivilegeFactory privilegeFactory, DatabaseService db) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event()).flatMap(options -> db.botAdminDao()
                    .remove(options.user.getId().asLong())
                    .then(ctx.event().createFollowup(
                            ctx.translate(Strings.GENERAL, "item_remove_success", options.user.getTag()))));
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return GRAMMAR.toOptions();
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }
    }

    @RdiService
    public static final class View implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final DatabaseService db;
        private final OutputPaginator paginator;

        @RdiFactory
        public View(PrivilegeFactory privilegeFactory, DatabaseService db, OutputPaginator paginator) {
            this.privilegeFactory = privilegeFactory;
            this.db = db;
            this.paginator = paginator;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return db.botAdminDao().getAllIds()
                    .map(Snowflake::of)
                    .flatMap(ctx.event().getClient()::getUserById)
                    .map(User::getTag)
                    .sort()
                    .collectList()
                    .flatMap(list -> paginator.paginate(ctx, list));
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }
    }

    private static final class Options {

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.USER,
                name = "user",
                description = "The target user.",
                required = true
        )
        User user;
    }
}