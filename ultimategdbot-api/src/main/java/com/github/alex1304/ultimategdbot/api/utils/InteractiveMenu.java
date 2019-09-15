package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.FlagSet;

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

public class InteractiveMenu {
	
	private final Consumer<MessageCreateSpec> spec;
	private final Map<String, Function<ReplyContext, Mono<Void>>> replyItems;
	private final Map<String, Function<ReactionContext, Mono<Void>>> reactionItems;
	private final boolean deleteMenuOnClose;
	private final boolean deleteMenuOnTimeout;
	private final boolean closeAfterReply;
	private final boolean closeAfterReaction;

	private InteractiveMenu(Consumer<MessageCreateSpec> spec,
			Map<String, Function<ReplyContext, Mono<Void>>> replyItems,
			Map<String, Function<ReactionContext, Mono<Void>>> reactionItems, boolean deleteMenuOnClose,
			boolean deleteMenuOnTimeout, boolean closeAfterReply, boolean closeAfterReaction) {
		this.spec = spec;
		this.replyItems = replyItems;
		this.reactionItems = reactionItems;
		this.deleteMenuOnClose = deleteMenuOnClose;
		this.deleteMenuOnTimeout = deleteMenuOnTimeout;
		this.closeAfterReply = closeAfterReply;
		this.closeAfterReaction = closeAfterReaction;
	}
	
	public static InteractiveMenu create(Consumer<MessageCreateSpec> spec) {
		return new InteractiveMenu(spec, Map.of(), Map.of(), false, false, true, true);
	}
	
	public static InteractiveMenu create(String message) {
		return create(mcs -> mcs.setContent(message));
	}

	public InteractiveMenu addReplyItem(String reply, Function<ReplyContext, Mono<Void>> action) {
		var newReplyItems = new HashMap<>(replyItems);
		newReplyItems.put(Objects.requireNonNull(reply), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, Collections.unmodifiableMap(newReplyItems), reactionItems, deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterReply, closeAfterReaction);
	}

	public InteractiveMenu addReactionItem(String emojiName, Function<ReactionContext, Mono<Void>> action) {
		var newReactionItems = new HashMap<>(reactionItems);
		newReactionItems.put(Objects.requireNonNull(emojiName), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, replyItems, Collections.unmodifiableMap(newReactionItems), deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterReply, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnClose(boolean deleteMenuOnClose) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterReply, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnTimeout(boolean deleteMenuOnTimeout) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterReply, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterReply(boolean closeAfterReply) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterReply, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterReaction(boolean closeAfterReaction) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterReply, closeAfterReaction);
	}
	
	public Mono<Void> open(Context ctx) {
		return ctx.reply(spec)
				.flatMap(menuMessage -> addReactionsToMenu(ctx, menuMessage))
				.flatMap(menuMessage -> Mono.zip(
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
									var action = replyItems.get(args.get(0));
									if (action == null) {
										return Mono.empty();
									}
									var replyCtx = new ReplyContext(menuMessage, event, new ArgumentList(args, ctx), flags);
									return action.apply(replyCtx).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterReply)
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
									var reactionCtx = new ReactionContext(menuMessage, event);
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
						+ "Interactive menus using reactions (such as navigation controls or confirmation dialogs) may not work properly.").then())
				.then()
				.thenReturn(menuMessage);
	}
	
	private static abstract class InteractionContext {
		
		private final Message menuMessage;

		private InteractionContext(Message menuMessage) {
			this.menuMessage = menuMessage;
		}

		public Message getMenuMessage() {
			return menuMessage;
		}
	}
	
	public static class ReplyContext extends InteractionContext {
		
		private final MessageCreateEvent event;
		private final ArgumentList args;
		private final FlagSet flags;

		private ReplyContext(Message menuMessage, MessageCreateEvent event, ArgumentList args, FlagSet flags) {
			super(menuMessage);
			this.event = event;
			this.args = args;
			this.flags = flags;
		}

		public MessageCreateEvent getEvent() {
			return event;
		}

		public ArgumentList getArgs() {
			return args;
		}

		public FlagSet getFlags() {
			return flags;
		}
	}
	
	public static class ReactionContext extends InteractionContext {
		
		private final ReactionAddEvent event;

		private ReactionContext(Message menuMessage, ReactionAddEvent event) {
			super(menuMessage);
			this.event = event;
		}

		public ReactionAddEvent getEvent() {
			return event;
		}
	}
}
