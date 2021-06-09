package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.api.config.object.I18nConfig;
import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.Scope;
import botrino.command.annotation.Alias;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.ArgumentMapper;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.framework.UltimateGDBotCommandEventProcessor;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Alias("locale")
@RdiService
@SetupEntry
public class LocaleCommand extends GetSetResetCommand<String> {

    private final UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor;
    private final I18nConfig i18nConfig;

    @RdiFactory
    public LocaleCommand(UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor,
                         ConfigContainer configContainer) {
        this.ultimateGDBotCommandEventProcessor = ultimateGDBotCommandEventProcessor;
        this.i18nConfig = configContainer.get(I18nConfig.class);
    }

    @Override
    ArgumentMapper<String> argumentMapper() {
        return ArgumentMapper.as(Function.identity());
    }

    @Override
    Mono<String> getFormattedValue(CommandContext ctx) {
        final var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElseThrow();
        return Mono.justOrEmpty(ultimateGDBotCommandEventProcessor.getCurrentGuildConfig(guildId)
                .locale()
                .map(Locale::forLanguageTag)
                .map(this::formatLocale));
    }

    @Override
    Mono<Void> setValue(CommandContext ctx, @Nullable String value) {
        final var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElseThrow();
        final var locale = value == null ? null : Locale.forLanguageTag(value);
        if (value != null && !i18nConfig.supportedLocales().contains(value)) {
            return Mono.error(new CommandFailedException(
                    ctx.translate(Strings.GENERAL, "error_unsupported_locale") + ' ' +
                            ctx.translate(Strings.GENERAL, "supported_locales", listLocales())));
        }
        return ultimateGDBotCommandEventProcessor.changeLocaleForGuild(guildId, locale);
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "locale_description") + ' ' +
                        tr.translate(Strings.GENERAL, "supported_locales", listLocales()))
                .build();
    }

    @Override
    public Scope scope() {
        return Scope.GUILD_ONLY;
    }

    private String listLocales() {
        return i18nConfig.supportedLocales().stream()
                .map(loc -> "\t\t- " + formatLocale(Locale.forLanguageTag(loc)))
                .collect(Collectors.joining("\n"));
    }

    private String formatLocale(Locale loc) {
        return Markdown.code(loc.toLanguageTag()) +
                " [" + loc.getDisplayLanguage(loc) +
                (loc.getDisplayCountry(loc).isEmpty() ? "" : " (" + loc.getDisplayCountry(loc) + ")") + "]";
    }
}
