package com.github.alex1304.ultimategdbot.api.utils.menu;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.InputTokenizer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Custom;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utility to create interactive menus in Discord. An interactive menu first
 * sends a message as a prompt and waits for a interaction from the user. The
 * said interaction can be either a message or a reaction.
 */
public class InteractiveMenu {
	
	private final Consumer<MessageCreateSpec> spec;
	private final Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems;
	private final Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems;
	private final boolean deleteMenuOnClose;
	private final boolean deleteMenuOnTimeout;
	private final boolean closeAfterMessage;
	private final boolean closeAfterReaction;

	private InteractiveMenu(Consumer<MessageCreateSpec> spec,
			Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems,
			Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems, boolean deleteMenuOnClose,
			boolean deleteMenuOnTimeout, boolean closeAfterMessage, boolean closeAfterReaction) {
		this.spec = spec;
		this.messageItems = messageItems;
		this.reactionItems = reactionItems;
		this.deleteMenuOnClose = deleteMenuOnClose;
		this.deleteMenuOnTimeout = deleteMenuOnTimeout;
		this.closeAfterMessage = closeAfterMessage;
		this.closeAfterReaction = closeAfterReaction;
	}
	
	public static InteractiveMenu create(Consumer<MessageCreateSpec> spec) {
		return new InteractiveMenu(spec, Map.of(), Map.of(), false, false, true, true);
	}
	
	public static InteractiveMenu create(String message) {
		return create(mcs -> mcs.setContent(message));
	}

	public InteractiveMenu addMessageItem(String message, Function<MessageMenuInteraction, Mono<Void>> action) {
		var newMessageItems = new HashMap<>(messageItems);
		newMessageItems.put(Objects.requireNonNull(message), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, Collections.unmodifiableMap(newMessageItems), reactionItems, deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterMessage, closeAfterReaction);
	}

	public InteractiveMenu addReactionItem(String emojiName, Function<ReactionMenuInteraction, Mono<Void>> action) {
		var newReactionItems = new HashMap<>(reactionItems);
		newReactionItems.put(Objects.requireNonNull(emojiName), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, messageItems, Collections.unmodifiableMap(newReactionItems), deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnClose(boolean deleteMenuOnClose) {
		return new InteractiveMenu(spec, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnTimeout(boolean deleteMenuOnTimeout) {
		return new InteractiveMenu(spec, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterMessage(boolean closeAfterMessage) {
		return new InteractiveMenu(spec, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterReaction(boolean closeAfterReaction) {
		return new InteractiveMenu(spec, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public Mono<Void> open(Context ctx) {
		return ctx.reply(spec)
				.flatMap(menuMessage -> addReactionsToMenu(ctx, menuMessage))
				.flatMap(menuMessage -> Mono.first(
						ctx.getBot().getDiscordClients()
								.flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
								.filter(event -> event.getMessage().getAuthor().equals(ctx.getEvent().getMessage().getAuthor())
										&& event.getMessage().getChannelId().equals(ctx.getEvent().getMessage().getChannelId()))
								.flatMap(event -> {
									var tokens = InputTokenizer.tokenize(event.getMessage().getContent().orElse(""));
									var args = tokens.getT2();
									var flags = tokens.getT1();
									if (args.isEmpty()) {
										return Mono.empty();
									}
									var action = messageItems.get(args.get(0));
									if (action == null) {
										return Mono.empty();
									}
									var replyCtx = new MessageMenuInteraction(menuMessage, event, new ArgumentList(args), flags);
									return action.apply(replyCtx).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterMessage)
								.onErrorResume(UnexpectedReplyException.class, e -> ctx.reply(":no_entry_sign: " + e.getMessage()).then(Mono.error(e)))
								.retry(UnexpectedReplyException.class::isInstance)
								.then(),
						ctx.getBot().getDiscordClients()
								.flatMap(client -> client.getEventDispatcher().on(ReactionAddEvent.class))
								.filter(event -> event.getMessageId().equals(menuMessage.getId())
										&& event.getUserId().equals(ctx.getEvent().getMessage().getAuthor().map(User::getId).orElse(null)))
								.flatMap(event -> {
									var emojiName = event.getEmoji().asCustomEmoji().map(Custom::getName)
											.or(() -> event.getEmoji().asUnicodeEmoji().map(Unicode::getRaw))
											.orElseThrow();
									var action = reactionItems.get(emojiName);
									if (action == null) {
										return Mono.empty();
									}
									var reactionCtx = new ReactionMenuInteraction(menuMessage, event);
									return action.apply(reactionCtx).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterReaction)
								.then())
						.then(deleteMenuOnClose ? menuMessage.delete().onErrorResume(e -> Mono.empty()) : Mono.empty())
						.timeout(Duration.ofSeconds(ctx.getBot().getReplyMenuTimeout()), deleteMenuOnTimeout 
								? menuMessage.delete().onErrorResume(e -> Mono.empty())
								: menuMessage.removeAllReactions().onErrorResume(e -> Mono.empty())));
	}
	
	private Mono<Message> addReactionsToMenu(Context ctx, Message menuMessage) {
		return Flux.fromIterable(reactionItems.keySet())
				.flatMap(emojiName -> ctx.getBot().getInstalledEmojis()
						.filter(installedEmoji -> installedEmoji.getName().equalsIgnoreCase(emojiName))
						.next()
						.map(ReactionEmoji::custom)
						.cast(ReactionEmoji.class)
						.switchIfEmpty(Mono.just(emojiName)
								.map(ReactionEmoji::unicode)))
				.concatMap(reaction -> menuMessage.addReaction(reaction)
						.onErrorResume(ClientException.isStatusCode(403).negate(), e -> Mono.empty()))
				.onErrorResume(ClientException.isStatusCode(403), e -> ctx.reply(":warning: It seems that I am missing Add Reactions permission. "
						+ "Interactive menus using reactions (such as navigation controls or confirmation dialogs) may be unusable.").then())
				.then()
				.thenReturn(menuMessage);
	}
}
