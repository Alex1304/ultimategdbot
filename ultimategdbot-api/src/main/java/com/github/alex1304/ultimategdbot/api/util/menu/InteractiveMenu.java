package com.github.alex1304.ultimategdbot.api.util.menu;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.util.InputTokenizer;
import com.github.alex1304.ultimategdbot.api.util.MessageSpecTemplate;

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
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.util.annotation.Nullable;

/**
 * Utility to create interactive menus in Discord. An interactive menu first
 * sends a message as a prompt and waits for a interaction from the user. The
 * said interaction can be either a message or a reaction.
 */
public class InteractiveMenu {
	
	private final Mono<Consumer<MessageCreateSpec>> specMono;
	private final Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems = new LinkedHashMap<>();
	private final Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems = new LinkedHashMap<>();
	private boolean deleteMenuOnClose;
	private boolean deleteMenuOnTimeout;
	private boolean closeAfterMessage = true;
	private boolean closeAfterReaction = true;
	private int timeoutSeconds = -1;

	private InteractiveMenu(Mono<Consumer<MessageCreateSpec>> specMono) {
		this.specMono = specMono;
	}
	
	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt.
	 * 
	 * @param spec the spec to build the menu message
	 * @return a new InteractiveMenu
	 */
	public static InteractiveMenu create(Consumer<MessageCreateSpec> spec) {
		requireNonNull(spec);
		return create(Mono.just(spec));
	}
	
	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt.
	 * 
	 * @param message the menu message
	 * @return a new InteractiveMenu
	 */
	public static InteractiveMenu create(String message) {
		requireNonNull(message);
		return create(mcs -> mcs.setContent(message));
	}
	
	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt. The menu message may be supplied from an asynchronous source.
	 * 
	 * @param specMono the Mono emitting the spec to build the menu message
	 * @return a new InteractiveMenu
	 */
	public static InteractiveMenu create(Mono<Consumer<MessageCreateSpec>> specMono) {
		requireNonNull(specMono);
		return new InteractiveMenu(specMono);
	}

	/**
	 * Creates a new InteractiveMenu prefilled with menu items useful for
	 * pagination.
	 * 
	 * @param currentPageRef an AtomicInteger that stores the current page number,
	 *                       may be null if you don't need it
	 * @param controls       the emojis to use for reaction-based navigation
	 *                       controls
	 * @param paginator      a Function that generates the message to display
	 *                       according to the current page number. If the page
	 *                       number is out of range, the function may throw a
	 *                       {@link PageNumberOutOfRangeException} which is handled
	 *                       by default to cover cases where the user inputs an
	 *                       invalid page number. Note that if the paginator
	 *                       function throws {@link PageNumberOutOfRangeException}
	 *                       with min/max values that aren't the same depending on
	 *                       the current page number, the behavior of the
	 *                       InteractiveMenu will be undefined.
	 * @return a new InteractiveMenu prefilled with menu items useful for
	 *         pagination.
	 */
	public static InteractiveMenu createPaginated(@Nullable AtomicInteger currentPageRef, PaginationControls controls, IntFunction<MessageSpecTemplate> paginator) {
		requireNonNull(paginator);
		return createAsyncPaginated(currentPageRef, controls, p -> Mono.just(paginator.apply(p)));
	}

	/**
	 * Creates a new InteractiveMenu prefilled with menu items useful for
	 * pagination. Unlike
	 * {@link #createPaginated(AtomicInteger, PaginationControls, IntFunction)},
	 * this method support asynchronous paginator functions.
	 * 
	 * @param currentPageRef an AtomicInteger that stores the current page number,
	 *                       may be null if you don't need it
	 * @param controls       the emojis to use for reaction-based navigation
	 *                       controls
	 * @param asyncPaginator a Function that asynchronously generates the message to
	 *                       display according to the current page number. If the
	 *                       page number is out of range, the Mono returned by this
	 *                       function may emit a
	 *                       {@link PageNumberOutOfRangeException} which is handled
	 *                       by default to cover cases where the user inputs an
	 *                       invalid page number. Note that if
	 *                       {@link PageNumberOutOfRangeException} is emitted with
	 *                       min/max values that aren't the same depending on the
	 *                       current page number, the behavior of the
	 *                       InteractiveMenu will be undefined.
	 * @return a new InteractiveMenu prefilled with menu items useful for
	 *         pagination.
	 */
	public static InteractiveMenu createAsyncPaginated(@Nullable AtomicInteger currentPageRef, PaginationControls controls, IntFunction<Mono<MessageSpecTemplate>> asyncPaginator) {
		requireNonNull(controls);
		requireNonNull(asyncPaginator);
		var currentPage = requireNonNullElse(currentPageRef, new AtomicInteger());
		var oldPage = new AtomicInteger();
		return create(asyncPaginator.apply(currentPage.get()).map(MessageSpecTemplate::toMessageCreateSpec))
				.addReactionItem(controls.getPreviousEmoji(), interaction -> Mono.fromCallable(currentPage::decrementAndGet)
						.flatMap(targetPage -> asyncPaginator.apply(targetPage)
								.map(MessageSpecTemplate::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono
								.just(currentPage.get() + e.getMaxPage() - e.getMinPage() + 1)
								.doOnNext(currentPage::set)
								.flatMap(targetPage -> asyncPaginator.apply(targetPage)
										.map(MessageSpecTemplate::toMessageEditSpec)))
						.flatMap(interaction.getMenuMessage()::edit)
						.then())
				.addReactionItem(controls.getNextEmoji(), interaction -> Mono.fromCallable(currentPage::incrementAndGet)
						.flatMap(targetPage -> asyncPaginator.apply(targetPage).map(MessageSpecTemplate::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono
								.just(currentPage.get() - e.getMaxPage() + e.getMinPage() - 1)
								.doOnNext(currentPage::set)
								.flatMap(targetPage -> asyncPaginator.apply(targetPage)
										.map(MessageSpecTemplate::toMessageEditSpec)))
						.flatMap(interaction.getMenuMessage()::edit)
						.then())
				.addMessageItem("page", interaction -> Mono.fromCallable(() -> Integer.parseInt(interaction.getArgs().get(1)))
						.onErrorMap(IndexOutOfBoundsException.class, e -> new UnexpectedReplyException("Please specify a page number."))
						.onErrorMap(NumberFormatException.class, e -> new UnexpectedReplyException("Invalid page number."))
						.map(p -> p - 1)
						.doOnNext(targetPage -> {
							oldPage.set(currentPage.get());
							currentPage.set(targetPage);
						})
						.flatMap(targetPage -> asyncPaginator.apply(targetPage)
								.map(MessageSpecTemplate::toMessageEditSpec)
								.flatMap(interaction.getMenuMessage()::edit))
						.onErrorMap(PageNumberOutOfRangeException.class, e -> {
							currentPage.set(oldPage.get());
							return new UnexpectedReplyException("Page number must be between "
									+ (e.getMinPage() + 1) + " and " + (e.getMaxPage() + 1) + ".");
						})
						.then(interaction.getEvent().getMessage().delete().onErrorResume(e -> Mono.empty())))
				.addReactionItem(controls.getCloseEmoji(), interaction -> Mono.fromRunnable(interaction::closeMenu))
				.closeAfterMessage(false)
				.closeAfterReaction(false);
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
	 * Sets a timeout in seconds after which the menu automatically closes. The
	 * timeout starts when the menu opens, and is not reset by user interaction.
	 * 
	 * @param timeoutSeconds the timeout in seconds
	 * @return this menu
	 */
	public InteractiveMenu withTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
		return this;
	}
	
	/**
	 * Opens the interactive menu, that is, sends the menu message over Discord and
	 * starts listening for user's interaction. The returned Mono completes once the
	 * menu closes or timeouts. If the menu was created using the factory method
	 * {@link #create(Mono)} and the supplied Mono completes empty or with an
	 * error, the respective signals will be forwarded through the returning Mono.
	 * 
	 * @param ctx the context of the command invoking this menu
	 * @return a Mono completing when the menu closes or timeouts. Any error
	 *         happening while the menu is open will be forwarded through the Mono
	 */
	public Mono<Void> open(Context ctx) {
		requireNonNull(ctx);
		if (timeoutSeconds < 0) {
			timeoutSeconds = ctx.getBot().getConfig().getInteractiveMenuTimeoutSeconds();
		}
		var closeNotifier = MonoProcessor.<Void>create();
		return specMono.flatMap(ctx::reply)
				.flatMap(menuMessage -> addReactionsToMenu(ctx, menuMessage))
				.flatMap(menuMessage -> {
					@SuppressWarnings("deprecation")
					var menuMono = Mono.first(
						closeNotifier,
						ctx.getBot().getGateway().on(MessageCreateEvent.class)
								.filter(event -> event.getMessage().getAuthor().equals(ctx.getEvent().getMessage().getAuthor())
										&& event.getMessage().getChannelId().equals(ctx.getEvent().getMessage().getChannelId()))
								.flatMap(event -> {
									var tokens = InputTokenizer.tokenize(ctx.getBot().getConfig().getFlagPrefix(), event.getMessage().getContent());
									var args = tokens.getT2();
									var flags = tokens.getT1();
									if (args.isEmpty()) {
										return Mono.empty();
									}
									var action = messageItems.get(args.get(0));
									if (action == null) {
										return Mono.empty();
									}
									var interaction = new MessageMenuInteraction(menuMessage, closeNotifier, event, new ArgumentList(args), flags);
									return action.apply(interaction).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterMessage)
								.onErrorResume(UnexpectedReplyException.class, e -> ctx.reply(":no_entry_sign: " + e.getMessage()).then(Mono.error(e)))
								.retry(UnexpectedReplyException.class::isInstance)
								.then(),
						Flux.merge(ctx.getBot().getGateway().on(ReactionAddEvent.class), ctx.getBot().getGateway().on(ReactionRemoveEvent.class))
								.map(ReactionToggleEvent::new)
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
									var reactionCtx = new ReactionMenuInteraction(menuMessage, closeNotifier, event);
									return action.apply(reactionCtx).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterReaction)
								.then())
						.then(handleTermination(menuMessage, deleteMenuOnClose));
					return timeoutSeconds == 0
							? menuMono
							: menuMono.timeout(Duration.ofSeconds(timeoutSeconds), handleTermination(menuMessage, deleteMenuOnTimeout));
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
				.flatMap(emojiName -> Flux.fromIterable(ctx.getBot().getConfig().getEmojiGuildIds())
						.flatMap(ctx.getBot().getGateway()::getGuild)
						.flatMap(Guild::getEmojis)
						.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
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
