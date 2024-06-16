package ultimategdbot.command.account;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.interaction.annotation.Acknowledge;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.Strings;
import ultimategdbot.command.CommandWithOptions;
import ultimategdbot.database.ImmutableUserSettings;
import ultimategdbot.database.UserSettings;
import ultimategdbot.database.UserSettingsDao;
import ultimategdbot.service.EmojiService;

import static java.util.Objects.requireNonNullElse;

@RdiService
@Acknowledge(Acknowledge.Mode.DEFER_EPHEMERAL)
public final class SettingsSubcommand extends CommandWithOptions<SettingsSubcommand.Options> {

    private final UserSettingsDao userSettingsDao;
    private final EmojiService emoji;

    @RdiFactory
    public SettingsSubcommand(UserSettingsDao userSettingsDao, EmojiService emoji) {
        this.userSettingsDao = userSettingsDao;
        this.emoji = emoji;
    }

    @Override
    protected Class<Options> optionClass() {
        return Options.class;
    }

    @Override
    protected Publisher<?> runWithOptions(ChatInputInteractionContext ctx, Options options) {
        return userSettingsDao.getById(ctx.event().getInteraction().getUser().getId().asLong())
                .map(options::toSettings)
                .flatMap(settings -> options.isViewRequest() ? Mono.just(settings) :
                        userSettingsDao.save(settings)
                                .then(Mono.defer(() -> ctx.event()
                                        .createFollowup(emoji.get("success") + ' ' +
                                                ctx.translate(Strings.GENERAL, "settings_saved"))
                                        .withEphemeral(true)))
                                .thenReturn(settings))
                .flatMap(settings -> ctx.event().createFollowup(formatSettings(ctx, settings)).withEphemeral(true));
    }

    private static String formatSettings(Translator tr, UserSettings settings) {
        return tr.translate(Strings.GENERAL, "settings_intro") + "\n\n" +
                Markdown.bold(tr.translate(Strings.GENERAL, "settings_hide_discord_from_profile") + ": ") +
                formatBoolean(tr, settings.hideDiscordFromProfile()) + '\n' +
                Markdown.bold(tr.translate(Strings.GENERAL, "settings_receive_dm_on_event") + ": ") +
                formatBoolean(tr, settings.receiveDmOnEvent());
    }

    private static String formatBoolean(Translator tr, boolean bool) {
        return tr.translate(Strings.GENERAL, bool ? "yes" : "no");
    }

    protected record Options(
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.BOOLEAN,
                    name = "hide-discord-from-profile",
                    description = "Whether to show your Discord account on your /profile."
            )
            @Nullable Boolean hideDiscordFromProfile,
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.BOOLEAN,
                    name = "receive-dm-on-event",
                    description = "Whether to receive a DM when you get a level rated or get promoted in the game."
            )
            @Nullable Boolean receiveDmOnEvent
    ) {

        boolean isViewRequest() {
            return hideDiscordFromProfile == null && receiveDmOnEvent == null;
        }

        UserSettings toSettings(UserSettings initialSettings) {
            if (isViewRequest()) return initialSettings;
            return ImmutableUserSettings.builder()
                    .from(initialSettings)
                    .hideDiscordFromProfile(
                            requireNonNullElse(hideDiscordFromProfile, initialSettings.hideDiscordFromProfile()))
                    .receiveDmOnEvent(requireNonNullElse(receiveDmOnEvent, initialSettings.receiveDmOnEvent()))
                    .build();
        }
    }
}
