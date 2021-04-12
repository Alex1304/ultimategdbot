package ultimategdbot.command;

import botrino.api.Botrino;
import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.util.VersionUtils;

import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

import static reactor.function.TupleUtils.function;

@Alias("about")
@TopLevelCommand
@RdiService
public final class AboutCommand implements Command {

    private static final Mono<Properties> D4J_PROPS = Mono.fromCallable(GitProperties::getProperties).cache();

    private final User botOwner;

    public AboutCommand(User botOwner) {
        this.botOwner = botOwner;
    }

    @RdiFactory
    public static Mono<AboutCommand> create(ApplicationInfo applicationInfo) {
        return applicationInfo.getOwner().map(AboutCommand::new);
    }

    private static Mono<String> version(Translator tr, Mono<Properties> props) {
        return props.map(p -> Optional.ofNullable(p.getProperty(GitProperties.APPLICATION_VERSION))
                .orElse("*" + tr.translate(Strings.APP, "unknown") + "*"))
                .defaultIfEmpty("*" + tr.translate(Strings.APP, "unknown") + "*");
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return Mono.zip(D4J_PROPS.transform(props -> version(ctx, props)),
                VersionUtils.getGitProperties(VersionUtils.API_GIT_RESOURCE).transform(props -> version(ctx, props)),
                ctx.event().getClient().getSelf(),
                ctx.event().getClient().getGuilds().count())
                .map(function((d4jVersion, apiVersion, self, guildCount) -> {
                    var versionInfoBuilder = new StringBuilder("**")
                            .append(ctx.translate(Strings.APP, "ugdb_version"))
                            .append("** ");
                    versionInfoBuilder.append(apiVersion).append("\n");
                    versionInfoBuilder.append("**");
                    versionInfoBuilder.append(ctx.translate(Strings.APP, "d4j_version"));
                    versionInfoBuilder.append("** ")
                            .append(d4jVersion)
                            .append("\n");
                    versionInfoBuilder.append("**");
                    versionInfoBuilder.append(ctx.translate(Strings.APP, "botrino_version"));
                    versionInfoBuilder.append("** ")
                            .append(Botrino.API_VERSION)
                            .append("\n");
                    var vars = new HashMap<String, String>();
                    vars.put("bot_name", self.getUsername());
                    vars.put("bot_owner", botOwner.getTag());
                    vars.put("server_count", "" + guildCount);
                    vars.put("version_info", versionInfoBuilder.toString());
                    var box = new Object() {
                        private String text = ctx.translate(Strings.ABOUT, "about_text");
                    };
                    vars.forEach((k, v) -> box.text = box.text.replaceAll("\\{\\{ *" + k + " *\\}\\}", "" + v));
                    return box.text;
                }))
                .flatMap(ctx.channel()::createMessage)
                .then();
    }
}
