package ultimategdbot.command;

import botrino.api.Botrino;
import botrino.api.i18n.Translator;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.util.Resources;

import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

import static reactor.function.TupleUtils.function;

@RdiService
@ChatInputCommand(name = "about", description = "Show information about the bot itself.")
public final class AboutCommand implements ChatInputInteractionListener {

    private static final Properties D4J_PROPS = GitProperties.getProperties();
    private static final Properties UGDB_PROPS = Resources.ugdbGitProperties();
    private static final String ABOUT = Resources.about();

    private final User botOwner;

    private AboutCommand(User botOwner) {
        this.botOwner = botOwner;
    }

    @RdiFactory
    public static Mono<AboutCommand> create(ApplicationInfo applicationInfo) {
        return applicationInfo.getOwner().map(AboutCommand::new);
    }

    private static String version(Translator tr, Properties props) {
        return Optional.ofNullable(props.getProperty(GitProperties.APPLICATION_VERSION))
                .orElse("*" + tr.translate(Strings.GENERAL, "unknown") + "*");
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return Mono.zip(ctx.event().getClient().getSelf(), ctx.event().getClient().getGuilds().count())
                .map(function((self, guildCount) -> {
                    final var versionInfoBuilder = new StringBuilder("**")
                            .append(ctx.translate(Strings.GENERAL, "ugdb_version"))
                            .append("** ");
                    versionInfoBuilder.append(version(ctx, UGDB_PROPS)).append("\n");
                    versionInfoBuilder.append("**");
                    versionInfoBuilder.append(ctx.translate(Strings.GENERAL, "d4j_version"));
                    versionInfoBuilder.append("** ")
                            .append(version(ctx, D4J_PROPS))
                            .append("\n");
                    versionInfoBuilder.append("**");
                    versionInfoBuilder.append(ctx.translate(Strings.GENERAL, "botrino_version"));
                    versionInfoBuilder.append("** ")
                            .append(Botrino.API_VERSION)
                            .append("\n");
                    final var vars = new HashMap<String, String>();
                    vars.put("bot_name", self.getUsername());
                    vars.put("bot_owner", botOwner.getTag());
                    vars.put("server_count", "" + guildCount);
                    vars.put("version_info", versionInfoBuilder.toString());
                    final var box = new Object() {
                        private String text = ABOUT;
                    };
                    vars.forEach((k, v) -> box.text = box.text.replaceAll("\\{\\{ *" + k + " *\\}\\}", "" + v));
                    return box.text;
                }))
                .flatMap(ctx.event()::createFollowup)
                .then();
    }
}
