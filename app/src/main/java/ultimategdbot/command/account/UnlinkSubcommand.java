package ultimategdbot.command.account;

import botrino.interaction.InteractionFailedException;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.service.EmojiService;

import java.util.List;
import java.util.UUID;

import static ultimategdbot.util.Interactions.confirmButtons;
import static ultimategdbot.util.Interactions.deleteFollowupAndPropagate;

@RdiService
public final class UnlinkSubcommand implements ChatInputInteractionListener {

    private final GdLinkedUserDao gdLinkedUserDao;
    private final EmojiService emoji;

    @RdiFactory
    public UnlinkSubcommand(GdLinkedUserDao gdLinkedUserDao, EmojiService emoji) {
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.emoji = emoji;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        final var authorId = ctx.user().getId().asLong();
        return gdLinkedUserDao.getAllActiveLinks(authorId)
                .switchIfEmpty(Mono.error(new InteractionFailedException(
                        ctx.translate(Strings.GD, "error_not_linked"))))
                .collectList()
                .flatMap(linkedUsers -> {
                    if (linkedUsers.size() == 1) {
                        return doUnlink(ctx, List.of(linkedUsers.get(0).discordUserId()), false);
                    }
                    return selectUnlink(ctx, linkedUsers);
                });
    }

    private Mono<Void> doUnlink(ChatInputInteractionContext ctx, List<Long> discordUserIds, boolean isMany) {
        final var yesId = UUID.randomUUID().toString();
        final var noId = UUID.randomUUID().toString();
        return ctx.event().createFollowup(ctx.translate(Strings.GD, isMany ? "unlink_confirm" :
                        "unlink_confirm_many"))
                .withComponents(confirmButtons(ctx, yesId, noId))
                .map(Message::getId)
                .flatMap(messageId -> Mono.firstWithValue(ctx.awaitButtonClick(yesId), ctx.awaitButtonClick(noId))
                        .flatMap(clicked -> clicked.equals(noId) ? ctx.event().deleteFollowup(messageId) : Flux
                                .fromIterable(discordUserIds)
                                .flatMap(gdLinkedUserDao::delete)
                                .then(ctx.event().editFollowup(messageId)
                                        .withContentOrNull(emoji.get("success") + ' ' +
                                                ctx.translate(Strings.GD, isMany ? "unlink_success" :
                                                        "unlink_success_many"))
                                        .withComponentsOrNull(null))))
                .then();
    }

    private Mono<Void> selectUnlink(ChatInputInteractionContext ctx, List<GdLinkedUser> linkedUsers) {
        final var selectId = UUID.randomUUID().toString();
        final var authorId = ctx.user().getId().asLong();
        return Flux.fromIterable(linkedUsers)
                .flatMap(linkedUser -> ctx.event().getClient()
                        .getUserById(Snowflake.of(linkedUser.discordUserId()))
                        .map(User::getTag)
                        .onErrorResume(e -> Mono.empty())
                        .defaultIfEmpty("Unknown User")
                        .map(tag -> Tuples.of(linkedUser.discordUserId(), tag)))
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .flatMap(tagsByUserId -> ctx.event()
                        .createFollowup(ctx.translate(Strings.GD, "unlink_select"))
                        .withComponents(ActionRow.of(SelectMenu.of(selectId, tagsByUserId.entrySet()
                                        .stream()
                                        .map(entry -> SelectMenu.Option
                                                .of(entry.getValue(), "" + entry.getKey())
                                                .withDescription("" + entry.getKey())
                                                .withDefault(entry.getKey() == authorId))
                                        .toList())
                                .withMaxValues(tagsByUserId.size())))
                        .map(Message::getId)
                        .flatMap(messageId -> ctx.awaitSelectMenuItems(selectId)
                                .map(items -> items.stream()
                                        .map(Snowflake::asLong)
                                        .toList())
                                .flatMap(userIds -> doUnlink(ctx, userIds, true))
                                .then(ctx.event().deleteFollowup(messageId))
                                .onErrorResume(deleteFollowupAndPropagate(ctx, messageId))))
                .then();
    }
}
