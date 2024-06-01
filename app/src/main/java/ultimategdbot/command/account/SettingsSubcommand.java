package ultimategdbot.command.account;

import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.reactivestreams.Publisher;
import reactor.util.annotation.Nullable;
import ultimategdbot.database.ImmutableUserSettings;
import ultimategdbot.database.UserSettings;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

@RdiService
public final class SettingsSubcommand implements ChatInputInteractionListener {

    private static final ChatInputCommandGrammar<Options> GRAMMAR = ChatInputCommandGrammar.of(Options.class);

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return GRAMMAR.resolve(ctx.event());
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return GRAMMAR.toOptions();
    }

    private record Options(
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.BOOLEAN,
                    name = "show-discord-on-profile",
                    description = "Whether to show your Discord account on your /profile."
            )
            @Nullable Boolean showDiscordOnProfile
    ) {

        boolean isViewRequest() {
            return showDiscordOnProfile == null;
        }

        UserSettings toSettings(UserSettings initialSettings) {
            final var defaultSettings = UserSettings.defaultSettings(initialSettings.userId());
            return ImmutableUserSettings.builder()
                    .from(initialSettings)
                    .showDiscordOnProfile(
                            requireNonNullElse(showDiscordOnProfile, defaultSettings.showDiscordOnProfile()))
                    .build();
        }
    }
}
