package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.command.Context;

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
	private final Map<String, BiFunction<Message, Context, Mono<Void>>> replyItems;
	private final Map<String, BiFunction<Message, ReactionAddEvent, Mono<Void>>> reactionItems;
	private final boolean deleteMenuOnReply;
	private final boolean deleteMenuOnTimeout;

	private InteractiveMenu(Consumer<MessageCreateSpec> spec,
			Map<String, BiFunction<Message, Context, Mono<Void>>> replyItems,
			Map<String, BiFunction<Message, ReactionAddEvent, Mono<Void>>> reactionItems, boolean deleteMenuOnReply,
			boolean deleteMenuOnTimeout) {
		this.spec = spec;
		this.replyItems = replyItems;
		this.reactionItems = reactionItems;
		this.deleteMenuOnReply = deleteMenuOnReply;
		this.deleteMenuOnTimeout = deleteMenuOnTimeout;
	}
	
	public static InteractiveMenu create(Consumer<MessageCreateSpec> spec) {
		return new InteractiveMenu(spec, Map.of(), Map.of(), false, false);
	}
	
	public static InteractiveMenu create(String message) {
		return create(mcs -> mcs.setContent(message));
	}

	public InteractiveMenu addReplyItem(String reply, BiFunction<Message, Context, Mono<Void>> action) {
		var newReplyItems = new HashMap<>(replyItems);
		newReplyItems.put(Objects.requireNonNull(reply), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, Collections.unmodifiableMap(newReplyItems), reactionItems, deleteMenuOnReply, deleteMenuOnTimeout);
	}

	public InteractiveMenu addReactionItem(String emojiName, BiFunction<Message, ReactionAddEvent, Mono<Void>> action) {
		var newReactionItems = new HashMap<>(reactionItems);
		newReactionItems.put(Objects.requireNonNull(emojiName), Objects.requireNonNull(action));
		return new InteractiveMenu(spec, replyItems, Collections.unmodifiableMap(newReactionItems), deleteMenuOnReply, deleteMenuOnTimeout);
	}
	
	public InteractiveMenu deleteMenuOnReply(boolean deleteMenuOnReply) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnReply, deleteMenuOnTimeout);
	}
	
	public InteractiveMenu deleteMenuOnTimeout(boolean deleteMenuOnTimeout) {
		return new InteractiveMenu(spec, replyItems, reactionItems, deleteMenuOnReply, deleteMenuOnTimeout);
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
									var replyCtx = new Context(ctx.getCommand(), event, args, flags, ctx.getBot(), "");
									return action.apply(menuMessage, replyCtx).materialize();
								})
								.next()
								.dematerialize()
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
									return action.apply(menuMessage, event);
								}).then())
						.then(deleteMenuOnReply ? menuMessage.delete().onErrorResume(e -> Mono.empty()) : Mono.empty())
						.timeout(Duration.ofSeconds(ctx.getBot().getReplyMenuTimeout()), deleteMenuOnTimeout 
								? menuMessage.delete().onErrorResume(e -> Mono.empty()) : Mono.empty()));
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
						.onErrorResume(ClientException.class, e -> Mono.empty()))
				.then()
				.thenReturn(menuMessage);
	}
}
