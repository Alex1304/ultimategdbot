package ultimategdbot.command;

import botrino.api.util.MessageUtils;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.Acknowledge;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.UserCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.context.UserInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.listener.UserInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.EmbedType;

import java.util.List;
import java.util.Objects;

@RdiService
@Acknowledge(Acknowledge.Mode.NONE)
@ChatInputCommand(name = "profile", description = "View the profile of any player in Geometry Dash.")
@UserCommand("View GD Profile")
public final class ProfileCommand implements ChatInputInteractionListener, UserInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GdLinkedUserDao gdLinkedUserDao;
    private final GDClient gdClient;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public ProfileCommand(GDCommandCooldown commandCooldown, GdLinkedUserDao gdLinkedUserDao, GDUserService userService,
                          GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.userService = userService;
        this.gdClient = gdClient.withWriteOnlyCache();
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return ctx.event().deferReply().then(grammar.resolve(ctx.event()))
                .flatMap(options -> runWithOptions(ctx, options));
    }

    private Mono<?> runWithOptions(ChatInputInteractionContext ctx, Options options) {
        return Mono.justOrEmpty(options.gdUsername)
                .flatMap(username -> userService.stringToUser(ctx, username))
                .switchIfEmpty(gdLinkedUserDao.getActiveLink(ctx.user().getId().asLong())
                        .switchIfEmpty(Mono.error(new InteractionFailedException(
                                ctx.translate(Strings.GD, "error_profile_user_not_specified"))))
                        .map(GdLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile))
                .flatMap(user -> userService.buildProfile(ctx, user, EmbedType.USER_PROFILE,
                                Objects.requireNonNullElse(options.showCompletedLevels, false))
                        .map(MessageUtils::toFollowupCreateSpec)
                        .flatMap(ctx.event()::createFollowup));
    }

    @Override
    public Publisher<?> run(UserInteractionContext ctx) {
        return ctx.event().deferReply().withEphemeral(true)
                .then(gdLinkedUserDao.getActiveLink(ctx.event().getTargetId().asLong()))
                .switchIfEmpty(Mono.error(new InteractionFailedException(
                        ctx.translate(Strings.GD, "error_no_gd_account"))))
                .map(GdLinkedUser::gdUserId)
                .flatMap(gdClient::getUserProfile)
                .flatMap(user -> userService.buildProfile(ctx, user, EmbedType.USER_PROFILE, false)
                        .map(MessageUtils::toFollowupCreateSpec)
                        .map(spec -> spec.withEphemeral(true))
                        .flatMap(ctx.event()::createFollowup));
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private record Options(
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.STRING,
                    name = "gd-username",
                    description = "The GD username of the target."
            )
            @Nullable String gdUsername,
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.BOOLEAN,
                    name = "show-completed-levels",
                    description = "Whether to show completed levels."
            )
            @Nullable Boolean showCompletedLevels
    ) {}
}
