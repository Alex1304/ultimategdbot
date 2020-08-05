package com.github.alex1304.ultimategdbot.api.command.menu;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.CommandService;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiService;
import com.github.alex1304.ultimategdbot.api.util.MessageUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Custom;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.util.retry.Retry;

/**
 * Utility to create interactive menus in Discord. An interactive menu first
 * sends a message as a prompt and waits for a interaction from the user. The
 * said interaction can be either a message or a reaction.
 */
public final class InteractiveMenu {
	
	private final GatewayDiscordClient gateway;
	private final CommandService commandService;
	private final EmojiService emojiService;
	private final Function<Translator, Mono<Consumer<MessageCreateSpec>>> menuMessageFactory;
	private final Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems = new LinkedHashMap<>();
	private final Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems = new LinkedHashMap<>();
	private boolean deleteMenuOnClose;
	private boolean deleteMenuOnTimeout;
	private boolean closeAfterMessage = true;
	private boolean closeAfterReaction = true;
	private Duration timeout;
	private ConcurrentHashMap<String, Object> interactionContext = new ConcurrentHashMap<>();

	InteractiveMenu(GatewayDiscordClient gateway, CommandService commandService, EmojiService emojiService,
			Function<Translator, Mono<Consumer<MessageCreateSpec>>> menuMessageFactory, Duration timeout) {
		this.gateway = gateway;
		this.commandService = commandService;
		this.emojiService = emojiService;
		this.menuMessageFactory = menuMessageFactory;
		this.timeout = timeout;
	}

	/**
	 * Initializes the interaction context that will be passed to all interaction
	 * instances happening while this menu is open.
	 * 
	 * @param contextConsumer the consumer that populates the context map
	 * @return this menu
	 */
	public InteractiveMenu withInteractionContext(Consumer<Map<String, Object>> contextConsumer) {
		requireNonNull(contextConsumer);
		contextConsumer.accept(interactionContext);
		return this;
	}

	/**
	 * Adds an item to this menu that is triggered when replying with a specific message.
	 * 
	 * @param message the text the message must start with in order to trigger this
	 *                item
	 * @param action  the action associated to this item
	 * @return this menu
	 */
	public InteractiveMenu addMessageItem(String message, Function<MessageMenuInteraction, Mono<Void>> action) {
		requireNonNull(message);
		requireNonNull(action);
		messageItems.put(message, action);
		return this;
	}

	/**
	 * Adds an item to this menu that is triggered when adding or removing a
	 * reaction to the menu message.
	 * 
	 * @param emojiName the name of the reaction emoji identifying this item. It can
	 *                  be a Unicode emoji character, or the name of one of the
	 *                  emojis in the emoji servers configured on the bot
	 * @param action    the action associated to this item
	 * @return this menu
	 */
	public InteractiveMenu addReactionItem(String emojiName, Function<ReactionMenuInteraction, Mono<Void>> action) {
		requireNonNull(emojiName);
		requireNonNull(action);
		reactionItems.put(emojiName, action);
		return this;
	}
	
	/**
	 * Sets whether to delete the menu message when the menu is closed by user.
	 * 
	 * @param deleteMenuOnClose a boolean
	 * @return this menu
	 */
	public InteractiveMenu deleteMenuOnClose(boolean deleteMenuOnClose) {
		this.deleteMenuOnClose = deleteMenuOnClose;
		return this;
	}
	
	/**
	 * Sets whether to delete the menu message when the menu is closed automatically
	 * by timeout
	 * 
	 * @param deleteMenuOnTimeout a boolean
	 * @return this menu
	 */
	public InteractiveMenu deleteMenuOnTimeout(boolean deleteMenuOnTimeout) {
		this.deleteMenuOnTimeout = deleteMenuOnTimeout;
		return this;
	}
	
	/**
	 * Sets whether to close this menu after a message item is triggered.
	 * 
	 * @param closeAfterMessage a boolean
	 * @return this menu
	 */
	public InteractiveMenu closeAfterMessage(boolean closeAfterMessage) {
		this.closeAfterMessage = closeAfterMessage;
		return this;
	}
	
	/**
	 * Sets whether to close this menu after a reaction item is triggered.
	 * 
	 * @param closeAfterReaction a boolean
	 * @return this menu
	 */
	public InteractiveMenu closeAfterReaction(boolean closeAfterReaction) {
		this.closeAfterReaction = closeAfterReaction;
		return this;
	}
	
	/**
	 * Sets a timeou after which the menu automatically closes. The timeout starts
	 * when the menu opens, and is not reset by user interaction.
	 * 
	 * @param timeout the timeout
	 * @return this menu
	 */
	public InteractiveMenu withTimeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}
	
	/**
	 * Opens the interactive menu, that is, sends the menu message over Discord and
	 * starts listening for user's interaction. The returned Mono completes once the
	 * menu closes or timeouts.
	 * 
	 * @param ctx the context of the command invoking this menu
	 * @return a Mono completing when the menu closes or timeouts. Any error
	 *         happening while the menu is open will be forwarded through the Mono
	 */
	public Mono<Void> open(Context ctx) {
		requireNonNull(ctx);
		if (messageItems.isEmpty() && reactionItems.isEmpty()) {
			return menuMessageFactory.apply(ctx).flatMap(ctx::reply).then();
		}
		var closeNotifier = MonoProcessor.<MenuTermination>create();
		var onInteraction = ReplayProcessor.cacheLastOrDefault(0); // Signals onNext each time user interacts with the menu
		var onInteractionSink = onInteraction.sink(FluxSink.OverflowStrategy.LATEST);
		return menuMessageFactory.apply(ctx).<Message>flatMap(ctx::reply)
				.flatMap(menuMessage -> addReactionsToMenu(ctx, menuMessage))
				.flatMap(menuMessage -> {
					var messageInteractionHandler = gateway.on(MessageCreateEvent.class)
							.takeUntilOther(closeNotifier)
							.filter(event -> event.getMessage().getAuthor().equals(ctx.event().getMessage().getAuthor())
									&& event.getMessage().getChannelId().equals(ctx.event().getMessage().getChannelId()))
							.flatMap(event -> {
								var tokens = MessageUtils.tokenize(commandService.getFlagPrefix(), event.getMessage().getContent());
								var args = tokens.getT2();
								var flags = tokens.getT1();
								if (args.isEmpty()) {
									return Mono.empty();
								}
								var action = messageItems.get(args.get(0));
								if (action == null) {
									action = messageItems.get("");
									if (action == null) {
										return Mono.empty();
									}
								}
								var interaction = new MessageMenuInteraction(ctx, menuMessage, interactionContext, closeNotifier,
										event, new ArgumentList(args), flags);
								return action.apply(interaction).doOnSubscribe(__ -> onInteractionSink.next(0)).thenReturn(0);
							})
							.takeUntil(__ -> closeAfterMessage)
							.onErrorResume(UnexpectedReplyException.class, e -> ctx.reply(":no_entry_sign: " + e.getMessage()).then(Mono.error(e)))
							.retryWhen(Retry.indefinitely().filter(UnexpectedReplyException.class::isInstance))
							.doFinally(__ -> closeNotifier.onNext(MenuTermination.CLOSED_BY_USER));
					var reactionInteractionHandler = Flux.merge(
									gateway.on(ReactionAddEvent.class),
									gateway.on(ReactionRemoveEvent.class))
							.takeUntilOther(closeNotifier)
							.map(ReactionToggleEvent::new)
							.filter(event -> event.getMessageId().equals(menuMessage.getId())
									&& event.getUserId().equals(ctx.event().getMessage().getAuthor().map(User::getId).orElse(null)))
							.flatMap(event -> {
								var emojiName = event.getEmoji().asCustomEmoji().map(Custom::getName)
										.or(() -> event.getEmoji().asUnicodeEmoji().map(Unicode::getRaw))
										.orElseThrow();
								var action = reactionItems.get(emojiName);
								if (action == null) {
									return Mono.empty();
								}
								var interaction = new ReactionMenuInteraction(ctx, menuMessage, interactionContext, closeNotifier, event);
								return action.apply(interaction).doOnSubscribe(__ -> onInteractionSink.next(0)).thenReturn(0);
							})
							.takeUntil(__ -> closeAfterReaction)
							.doFinally(__ -> closeNotifier.onNext(MenuTermination.CLOSED_BY_USER));
					var menuMono = Mono.when(messageInteractionHandler, reactionInteractionHandler);
					var timeoutNotifier = timeout.isZero()
							? Mono.never()
							: onInteraction.timeout(timeout, Flux.empty())
									.then(Mono.fromRunnable(() -> closeNotifier.onNext(MenuTermination.TIMEOUT)));
					var terminationHandler = closeNotifier.flatMap(termination -> {
						switch (termination) {
							case TIMEOUT:        return handleTermination(menuMessage, deleteMenuOnTimeout);
							case CLOSED_BY_USER: return handleTermination(menuMessage, deleteMenuOnClose);
							default:             return Mono.error(new AssertionError());
						}
					});
					return Mono.when(menuMono, timeoutNotifier.takeUntilOther(closeNotifier))
							.then(terminationHandler)
							.onErrorResume(e -> terminationHandler
									.onErrorResume(e2 -> Mono.fromRunnable(() -> e.addSuppressed(e2)))
									.then(Mono.error(e)));
				});
	}
	
	private static Mono<Void> handleTermination(Message menuMessage, boolean shouldDelete) {
		return (shouldDelete 
						? menuMessage.delete()
						: menuMessage.removeAllReactions())
				.onErrorResume(e -> Mono.empty());
	}
	
	private Mono<Message> addReactionsToMenu(Context ctx, Message menuMessage) {
		return Flux.fromIterable(reactionItems.keySet())
				.flatMap(emojiName -> Flux.fromIterable(emojiService.getEmojiGuildIds())
						.flatMap(gateway::getGuildById)
						.flatMap(Guild::getEmojis)
						.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
						.next()
						.map(ReactionEmoji::custom)
						.cast(ReactionEmoji.class)
						.switchIfEmpty(Mono.just(emojiName)
								.map(ReactionEmoji::unicode)))
				.concatMap(reaction -> menuMessage.addReaction(reaction)
						.onErrorResume(ClientException.isStatusCode(403).negate(), e -> Mono.empty()))
				.onErrorResume(ClientException.isStatusCode(403), e -> ctx.reply(ctx.translate("CommonStrings", "interactive_menu_missing_reaction_perms")).then())
				.then()
				.thenReturn(menuMessage);
	}
	
	static enum MenuTermination {
		TIMEOUT, CLOSED_BY_USER;
	}
}
