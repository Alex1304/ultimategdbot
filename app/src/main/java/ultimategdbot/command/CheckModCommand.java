package ultimategdbot.command;

import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.Acknowledge;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.UserCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.context.UserInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.listener.UserInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import jdash.common.Role;
import jdash.common.entity.GDUserProfile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;

import java.util.List;

@RdiService
@Acknowledge(Acknowledge.Mode.NONE)
@ChatInputCommand(name = "check-mod", description = "Checks for the presence of the Moderator badge on someone's " +
        "profile.")
@UserCommand("Check Mod Status")
public final class CheckModCommand implements ChatInputInteractionListener, UserInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GdLinkedUserDao gdLinkedUserDao;
    private final EmojiService emoji;
    private final GDClient gdClient;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public CheckModCommand(GDCommandCooldown commandCooldown, GdLinkedUserDao gdLinkedUserDao, EmojiService emoji,
                           GDUserService userService, GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.emoji = emoji;
        this.userService = userService;
        this.gdClient = gdClient.withWriteOnlyCache();
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return ctx.event().deferReply().then(grammar.resolve(ctx.event()))
                .flatMap(options -> Mono.justOrEmpty(options.gdUsername)
                        .flatMap(username -> userService.stringToUser(ctx, username)))
                .switchIfEmpty(gdLinkedUserDao.getActiveLink(ctx.user().getId().asLong())
                        .switchIfEmpty(Mono.error(new InteractionFailedException(
                                ctx.translate(Strings.GD, "error_checkmod_user_not_specified"))))
                        .map(GdLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile))
                .flatMap(user -> sendModStatus(ctx, user, false));
    }

    @Override
    public Publisher<?> run(UserInteractionContext ctx) {
        return ctx.event().deferReply().withEphemeral(true)
                .then(gdLinkedUserDao.getActiveLink(ctx.event().getTargetId().asLong()))
                .switchIfEmpty(Mono.error(new InteractionFailedException(
                        ctx.translate(Strings.GD, "error_no_gd_account"))))
                .map(GdLinkedUser::gdUserId)
                .flatMap(gdClient::getUserProfile)
                .flatMap(user -> sendModStatus(ctx, user, true));
    }

    private Mono<Message> sendModStatus(InteractionContext ctx, GDUserProfile profile, boolean ephemeral) {
        return ctx.event()
                .createFollowup(ctx.translate(Strings.GD, "checking_mod", profile.user().name()) + "\n||" +
                        (profile.user().role().orElse(Role.USER) == Role.USER
                                ? emoji.get("failed") + ' ' + ctx.translate(Strings.GD, "checkmod_failed")
                                : emoji.get("success") + ' ' + ctx.translate(Strings.GD, "checkmod_success",
                                profile.user().role().orElseThrow())) + "||")
                .withEphemeral(ephemeral);
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
