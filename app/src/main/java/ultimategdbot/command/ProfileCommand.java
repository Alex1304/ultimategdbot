package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.MessageTemplate;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.cooldown.Cooldown;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import botrino.command.grammar.CommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.EmbedType;

import static ultimategdbot.util.InteractionUtils.writeOnlyIfRefresh;

@CommandCategory(CommandCategory.GD)
@Alias("profile")
@TopLevelCommand
@RdiService
public final class ProfileCommand implements Command {

    private final GDCommandCooldown commandCooldown;
    private final DatabaseService db;
    private final GDUserService gdUserService;
    private final GDClient gdClient;

    private final CommandGrammar<Args> grammar;

    @RdiFactory
    public ProfileCommand(GDCommandCooldown commandCooldown, DatabaseService db, GDUserService gdUserService,
                          GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.db = db;
        this.gdUserService = gdUserService;
        this.gdClient = gdClient;
        this.grammar = CommandGrammar.builder()
                .beginOptionalArguments()
                .nextArgument("gdUser", gdUserService::stringToUser)
                .build(Args.class);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        final var gdClient = writeOnlyIfRefresh(ctx, this.gdClient);
        return grammar.resolve(ctx)
                .flatMap(args -> Mono.justOrEmpty(args.gdUser))
                .switchIfEmpty(db.gdLinkedUserDao().getActiveLink(ctx.author().getId().asLong())
                        .switchIfEmpty(Mono.error(new CommandFailedException(
                                ctx.translate(Strings.GD, "error_profile_user_not_specified", ctx.getPrefixUsed(),
                                        "profile"))))
                        .map(GdLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile)
                        .flatMap(db.gdLeaderboardDao()::saveStats)
                        .cast(GDUserProfile.class))
                .flatMap(user -> gdUserService.buildProfile(ctx, user, EmbedType.USER_PROFILE)
                        .map(MessageTemplate::toCreateSpec)
                        .flatMap(ctx.channel()::createMessage))
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "profile_description"))
                .setBody(tr.translate(Strings.HELP, "profile_body"))
                .addFlag(FlagInformation.builder()
                        .setValueFormat("refresh")
                        .setDescription(tr.translate(Strings.HELP, "common_flag_refresh"))
                        .build())
                .build();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private static final class Args {
        @Nullable
        GDUserProfile gdUser;
    }
}
