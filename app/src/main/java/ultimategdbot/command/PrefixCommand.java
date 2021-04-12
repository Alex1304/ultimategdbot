package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.CommandContext;
import botrino.command.Scope;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.grammar.ArgumentMapper;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.framework.UltimateGDBotCommandEventProcessor;

import java.util.function.Function;

@Alias("prefix")
@TopLevelCommand
@RdiService
public class PrefixCommand extends GetSetResetCommand<String> {

    private final UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor;

    @RdiFactory
    public PrefixCommand(UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor) {
        this.ultimateGDBotCommandEventProcessor = ultimateGDBotCommandEventProcessor;
    }

    @Override
    ArgumentMapper<String> argumentMapper() {
        return ArgumentMapper.as(Function.identity());
    }

    @Override
    Mono<String> getFormattedValue(CommandContext ctx) {
        final var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElseThrow();
        return Mono.justOrEmpty(ultimateGDBotCommandEventProcessor.getCurrentGuildConfig(guildId).prefix());
    }

    @Override
    Mono<Void> setValue(CommandContext ctx, @Nullable String value) {
        final var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElseThrow();
        return ultimateGDBotCommandEventProcessor.changePrefixForGuild(guildId, value);
    }

    @Override
    String description(Translator tr) {
        return tr.translate(Strings.APP, "description_prefix");
    }

    @Override
    String syntax() {
        return "prefix";
    }

    @Override
    public Scope scope() {
        return Scope.GUILD_ONLY;
    }
}
