package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.api.config.object.I18nConfig;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.framework.UGDBEventProcessor;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.PrivilegeFactory;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static ultimategdbot.util.Interactions.deleteFollowupAndPropagate;

@RdiService
@ChatInputCommand(name = "set-language", description = "Configure the language of the bot for this server.")
public class SetLanguageCommand implements ChatInputInteractionListener {

    private final PrivilegeFactory privilegeFactory;
    private final UGDBEventProcessor eventProcessor;
    private final I18nConfig i18nConfig;
    private final EmojiService emoji;

    @RdiFactory
    public SetLanguageCommand(PrivilegeFactory privilegeFactory, UGDBEventProcessor eventProcessor,
                              ConfigContainer configContainer, EmojiService emoji) {
        this.privilegeFactory = privilegeFactory;
        this.eventProcessor = eventProcessor;
        this.i18nConfig = configContainer.get(I18nConfig.class);
        this.emoji = emoji;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        final var guildId = ctx.event().getInteraction().getGuildId().orElseThrow().asLong();
        final var customId = UUID.randomUUID().toString();
        return ctx.event().createFollowup(ctx.translate(Strings.GENERAL, "language_select"))
                .withComponents(ActionRow.of(SelectMenu.of(customId, listLocales())))
                .map(Message::getId)
                .flatMap(messageId -> ctx.awaitSelectMenuItems(customId)
                        .flatMap(items -> eventProcessor
                                .changeLocaleForGuild(guildId, Locale.forLanguageTag(items.get(0)))
                                .then(ctx.event().createFollowup(emoji.get("success") + ' ' +
                                        ctx.translate(Strings.GENERAL, "language_update_success")))
                                .then(ctx.event().deleteFollowup(messageId))
                                .onErrorResume(deleteFollowupAndPropagate(ctx, messageId))));
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.guildAdmin();
    }

    private List<SelectMenu.Option> listLocales() {
        return i18nConfig.supportedLocales().stream()
                .map(Locale::forLanguageTag)
                .map(loc -> SelectMenu.Option.of(loc.getDisplayLanguage(loc) +
                        (loc.getDisplayCountry(loc).isEmpty() ? "" : " (" + loc.getDisplayCountry(loc) + ")"),
                        loc.toLanguageTag()))
                .collect(Collectors.toUnmodifiableList());
    }
}
