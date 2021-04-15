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
import ultimategdbot.database.BotAdminDao;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

@Alias("botadmins")
@TopLevelCommand
@RdiService
public final class BotAdminsCommand extends AddRemoveListCommand<User> {

    private final PrivilegeFactory privilegeFactory;
    private final BotAdminDao botAdminDao;

    @RdiFactory
    public BotAdminsCommand(OutputPaginator outputPaginator,
                            PrivilegeFactory privilegeFactory,
                            BotAdminDao botAdminDao) {
        super(outputPaginator);
        this.privilegeFactory = privilegeFactory;
        this.botAdminDao = botAdminDao;
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
        return botAdminDao.add(user.getId().asLong());
    }

    @Override
    Mono<Void> remove(CommandContext ctx, User user) {
        return botAdminDao.remove(user.getId().asLong());
    }

    @Override
    Flux<String> listFormattedItems(CommandContext ctx) {
        return botAdminDao.getAllIds()
                .map(Snowflake::of)
                .flatMap(ctx.event().getClient()::getUserById)
                .map(User::getTag)
                .sort();
    }

    @Override
    String syntax() {
        return "botadmins";
    }

    @Override
    String formatElement(User element) {
        return element.getTag();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.APP, "description_botadmins"))
                .build();
    }
}
