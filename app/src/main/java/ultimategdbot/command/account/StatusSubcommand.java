package ultimategdbot.command.account;

import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import org.reactivestreams.Publisher;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.service.GDCommandCooldown;

import static reactor.function.TupleUtils.function;

@RdiService
public final class StatusSubcommand implements ChatInputInteractionListener {

    private final GdLinkedUserDao gdLinkedUserDao;
    private final GDClient gdClient;
    private final GDCommandCooldown commandCooldown;

    @RdiFactory
    public StatusSubcommand(GdLinkedUserDao gdLinkedUserDao, GDClient gdClient, GDCommandCooldown commandCooldown) {
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.gdClient = gdClient;
        this.commandCooldown = commandCooldown;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return gdLinkedUserDao.getActiveLink(ctx.user().getId().asLong())
                .flatMap(linkedUser -> gdClient.getUserProfile(linkedUser.gdUserId()))
                .map(profile -> Tuples.of(true, ctx.translate(Strings.GD, "currently_linked", profile.user().name())))
                .defaultIfEmpty(Tuples.of(false, ctx.translate(Strings.GD, "not_yet_linked")))
                .flatMap(function((isLinked, message) -> ctx.event()
                        .createFollowup(ctx.translate(Strings.GD, "link_intro") + "\n\n" + message + "\n" +
                                (isLinked ? ctx.translate(Strings.GD, "how_to_unlink")
                                        : ctx.translate(Strings.GD, "how_to_link")))))
                .then();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }
}
