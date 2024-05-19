package ultimategdbot.command;

import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.PrivateCommand;
import botrino.interaction.annotation.Subcommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.service.BlacklistService;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;

@PrivateCommand
@ChatInputCommand(
        name = "blacklist",
        description = "Manage IDs of users, servers, or channels to be ignored by the bot (Bot Admin only).",
        defaultMemberPermissions = Permission.ADMINISTRATOR,
        subcommands = {
                @Subcommand(
                        name = "add",
                        description = "Add an ID into the blacklist.",
                        listener = BlacklistCommand.Add.class
                ),
                @Subcommand(
                        name = "remove",
                        description = "Remove an ID from the blacklist.",
                        listener = BlacklistCommand.Remove.class
                ),
                @Subcommand(
                        name = "view",
                        description = "View the IDs that are currently blacklisted.",
                        listener = BlacklistCommand.View.class
                )
        }
)
public final class BlacklistCommand {

    private static final ChatInputCommandGrammar<Options> GRAMMAR = ChatInputCommandGrammar.of(Options.class);

    @RdiService
    public static final class Add implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final BlacklistService blacklist;

        @RdiFactory
        public Add(PrivilegeFactory privilegeFactory, BlacklistService blacklist) {
            this.privilegeFactory = privilegeFactory;
            this.blacklist = blacklist;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event()).flatMap(options -> blacklist.addToBlacklist(options.id)
                    .then(ctx.event().createFollowup(
                            ctx.translate(Strings.GENERAL, "item_add_success", options.id))));
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
    public static final class Remove implements ChatInputInteractionListener {

        private final PrivilegeFactory privilegeFactory;
        private final BlacklistService blacklist;

        @RdiFactory
        public Remove(PrivilegeFactory privilegeFactory, BlacklistService blacklist) {
            this.privilegeFactory = privilegeFactory;
            this.blacklist = blacklist;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return GRAMMAR.resolve(ctx.event()).flatMap(options -> blacklist.removeFromBlacklist(options.id)
                    .then(ctx.event().createFollowup(
                            ctx.translate(Strings.GENERAL, "item_remove_success", options.id))));
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
        private final BlacklistService blacklist;
        private final OutputPaginator paginator;

        @RdiFactory
        public View(PrivilegeFactory privilegeFactory, BlacklistService blacklist, OutputPaginator paginator) {
            this.privilegeFactory = privilegeFactory;
            this.blacklist = blacklist;
            this.paginator = paginator;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return paginator.paginate(ctx, blacklist.blacklist().stream()
                    .map(String::valueOf)
                    .toList());
        }

        @Override
        public Privilege privilege() {
            return privilegeFactory.botOwner();
        }
    }

    private record Options(
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.INTEGER,
                    name = "id",
                    description = "The user, channel or guild ID.",
                    required = true
            )
            long id
    ) {}
}
