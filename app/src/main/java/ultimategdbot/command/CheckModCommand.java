package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import botrino.command.grammar.CommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.database.GDLinkedUser;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;

import static ultimategdbot.util.InteractionUtils.writeOnlyIfRefresh;

@CommandCategory(CommandCategory.GD)
@Alias("checkmod")
@TopLevelCommand
@RdiService
public final class CheckModCommand implements Command {

    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDClient gdClient;

    private final CommandGrammar<Args> grammar;

    @RdiFactory
    public CheckModCommand(DatabaseService db, EmojiService emoji, GDUserService gdUserService, GDClient gdClient) {
        this.db = db;
        this.emoji = emoji;
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
                                ctx.translate(Strings.GD, "error_checkmod_user_not_specified", ctx.getPrefixUsed(),
                                        "profile"))))
                        .map(GDLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile)
                        .flatMap(db.gdLeaderboardDao()::saveStats)
                        .cast(GDUserProfile.class))
                .flatMap(user -> ctx.channel()
                        .createMessage(ctx.translate(Strings.GD, "checking_mod", user.name()) + "\n||" +
                                (user.role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                        user.role().orElseThrow())) + "||"))
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "checkmod_description"))
                .setBody(tr.translate(Strings.HELP, "checkmod_body"))
                .addFlag(FlagInformation.builder()
                        .setValueFormat("refresh")
                        .setDescription(tr.translate(Strings.HELP, "common_flag_refresh"))
                        .build())
                .build();
    }

    private static final class Args {
        @Nullable
        GDUserProfile gdUser;
    }
}
