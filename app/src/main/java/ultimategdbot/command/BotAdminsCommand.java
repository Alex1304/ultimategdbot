package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

@CommandCategory(CommandCategory.GENERAL)
@Alias("botadmins")
@TopLevelCommand
@RdiService
public final class BotAdminsCommand extends AddRemoveListCommand<User> {

    private final PrivilegeFactory privilegeFactory;
    private final DatabaseService db;

    @RdiFactory
    public BotAdminsCommand(OutputPaginator outputPaginator,
                            PrivilegeFactory privilegeFactory,
                            DatabaseService db) {
        super(outputPaginator);
        this.privilegeFactory = privilegeFactory;
        this.db = db;
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.botOwner();
    }

    @Override
    ArgumentMapper<User> argumentMapper() {
        return ArgumentMapper.asUser();
    }

    @Override
    Mono<Void> add(CommandContext ctx, User user) {
        return db.botAdminDao().add(user.getId().asLong());
    }

    @Override
    Mono<Void> remove(CommandContext ctx, User user) {
        return db.botAdminDao().remove(user.getId().asLong());
    }

    @Override
    Flux<String> listFormattedItems(CommandContext ctx) {
        return db.botAdminDao().getAllIds()
                .map(Snowflake::of)
                .flatMap(ctx.event().getClient()::getUserById)
                .map(User::getTag)
                .sort();
    }

    @Override
    String formatItem(User element) {
        return element.getTag();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "botadmins_description"))
                .build();
    }
}
