package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.CommandGrammar;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;

@CommandCategory(CommandCategory.GD)
@Alias("modwave")
@TopLevelCommand
@RdiService
public final class ModWaveCommand implements Command {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDClient gdClient;
    private final PrivilegeFactory privilegeFactory;

    private final CommandGrammar<Args> grammar;

    @RdiFactory
    public ModWaveCommand(DatabaseService db, EmojiService emoji, GDUserService gdUserService, GDClient gdClient,
                          PrivilegeFactory privilegeFactory) {
        this.db = db;
        this.emoji = emoji;
        this.gdClient = gdClient.withWriteOnlyCache();
        this.grammar = CommandGrammar.builder()
                .setVarargs(true)
                .nextArgument("gdUsers", gdUserService::stringToUser)
                .build(Args.class);
        this.privilegeFactory = privilegeFactory;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return grammar.resolve(ctx)
                .flatMapMany(args -> Flux.fromIterable(args.gdUsers))
                .switchIfEmpty(Mono.error(new CommandFailedException(
                        ctx.translate(Strings.GD, "error_at_least_one_user"))))
                .flatMap(user ->
                        ctx.channel()
                        .createMessage(ctx.translate(Strings.GD, "checking_mod", user.name()) + "\n||" +
                                (user.role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                        user.role().orElseThrow())) + "||")
                        .then(db.gdModDao().get(user.accountId()))
                        .switchIfEmpty(Mono.defer(Mono::empty)) // TODO requires event service
                        //.flatMap(gdMod -> ...
                )
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "modwave_description"))
                .setBody(tr.translate(Strings.HELP, "modwave_body"))
                .build();
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.elderMod();
    }

    private static final class Args {
        List<GDUserProfile> gdUsers;
    }
}
