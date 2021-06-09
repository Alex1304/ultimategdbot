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
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.ImmutableGDMod;
import ultimategdbot.event.ImmutableModStatusUpdate;
import ultimategdbot.event.ManualEventProducer;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;

import static ultimategdbot.event.ModStatusUpdate.Type.*;

@CommandCategory(CommandCategory.GD)
@Alias("modwave")
@TopLevelCommand
@RdiService
public final class ModWaveCommand implements Command {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final PrivilegeFactory privilegeFactory;
    private final ManualEventProducer eventProducer;

    private final CommandGrammar<Args> grammar;

    @RdiFactory
    public ModWaveCommand(DatabaseService db, EmojiService emoji, GDUserService gdUserService,
                          PrivilegeFactory privilegeFactory, ManualEventProducer eventProducer) {
        this.db = db;
        this.emoji = emoji;
        this.grammar = CommandGrammar.builder()
                .setVarargs(true)
                .nextArgument("gdUsers", gdUserService::stringToUserAlwaysRefresh)
                .build(Args.class);
        this.privilegeFactory = privilegeFactory;
        this.eventProducer = eventProducer;
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
                        .switchIfEmpty(Mono.defer(() -> {
                            if (user.role().map(Role.USER::equals).orElse(true)) {
                                return Mono.empty();
                            }
                            final var isElder = user.role().map(Role.ELDER_MODERATOR::equals).orElse(false);
                            eventProducer.submit(ImmutableModStatusUpdate.of(user, isElder
                                    ? PROMOTED_TO_ELDER : PROMOTED_TO_MOD));
                            return db.gdModDao()
                                    .save(ImmutableGDMod.builder()
                                            .accountId(user.accountId())
                                            .name(user.name())
                                            .elder(isElder ? 1 : 0)
                                            .build())
                                    .then(Mono.empty());
                        }))
                        .flatMap(gdMod -> {
                            if (user.role().map(Role.USER::equals).orElse(true)) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, gdMod.isElder()
                                        ? DEMOTED_FROM_ELDER : DEMOTED_FROM_MOD));
                                return db.gdModDao().delete(gdMod.accountId());
                            }
                            final var newGdMod = ImmutableGDMod.builder().from(gdMod);
                            if(user.role().map(Role.MODERATOR::equals).orElse(false) && gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, DEMOTED_FROM_ELDER));
                                newGdMod.elder(0);
                            } else if (user.role().map(Role.ELDER_MODERATOR::equals).orElse(false) && !gdMod.isElder()) {
                                eventProducer.submit(ImmutableModStatusUpdate.of(user, PROMOTED_TO_ELDER));
                                newGdMod.elder(1);
                            }
                            newGdMod.name(user.name());
                            return db.gdModDao().save(newGdMod.build());
                        })
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
