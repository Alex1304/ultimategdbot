package ultimategdbot.command;

import botrino.api.util.MessageUtils;
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
import jdash.common.entity.GDUserProfile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;
import ultimategdbot.util.EmbedType;

import java.util.List;

@RdiService
@ChatInputCommand(name = "profile", description = "View the profile of any player in Geometry Dash.")
public final class ProfileCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final DatabaseService db;
    private final GDClient gdClient;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public ProfileCommand(GDCommandCooldown commandCooldown, DatabaseService db, GDUserService userService,
                          GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.db = db;
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
                                ctx.translate(Strings.GD, "error_profile_user_not_specified"))))
                        .map(GdLinkedUser::gdUserId)
                        .flatMap(gdClient::getUserProfile)
                        .flatMap(db.gdLeaderboardDao()::saveStats)
                        .cast(GDUserProfile.class))
                .flatMap(user -> userService.buildProfile(ctx, user, EmbedType.USER_PROFILE)
                        .map(MessageUtils::toFollowupCreateSpec)
                        .flatMap(ctx.event()::createFollowup))
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
