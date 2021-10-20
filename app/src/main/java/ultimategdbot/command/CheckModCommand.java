package ultimategdbot.command;

import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;

import java.util.List;

@RdiService
@ChatInputCommand(name = "check-mod", description = "Checks for the presence of the Moderator badge on someone's " +
        "profile.")
public final class CheckModCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDClient gdClient;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public CheckModCommand(GDCommandCooldown commandCooldown, DatabaseService db, EmojiService emoji,
                           GDUserService userService, GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.db = db;
        this.emoji = emoji;
        this.userService = userService;
        this.gdClient = gdClient.withWriteOnlyCache();
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event())
                .flatMap(options -> Mono.justOrEmpty(options.gdUsername)
                        .flatMap(username -> userService.stringToUser(ctx, username)))
                .switchIfEmpty(db.gdLinkedUserDao().getActiveLink(ctx.user().getId().asLong())
                        .switchIfEmpty(Mono.error(new InteractionFailedException(
                                ctx.translate(Strings.GD, "error_checkmod_user_not_specified"))))
                        .map(GdLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile)
                        .flatMap(db.gdLeaderboardDao()::saveStats)
                        .cast(GDUserProfile.class))
                .flatMap(user -> ctx.event()
                        .createFollowup(ctx.translate(Strings.GD, "checking_mod", user.name()) + "\n||" +
                                (user.role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                        user.role().orElseThrow())) + "||"))
                .then();
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private static final class Options {
        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "gd-username",
                description = "The GD username of the target."
        )
        String gdUsername;
    }
}
