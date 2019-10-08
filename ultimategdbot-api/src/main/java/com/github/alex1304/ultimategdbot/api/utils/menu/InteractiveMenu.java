package com.github.alex1304.ultimategdbot.api.utils.menu;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.InputTokenizer;
import com.github.alex1304.ultimategdbot.api.utils.UniversalMessageSpec;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
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

/**
 * Utility to create interactive menus in Discord. An interactive menu first
 * sends a message as a prompt and waits for a interaction from the user. The
 * said interaction can be either a message or a reaction.
 */
public class InteractiveMenu {
	
	private final Mono<Consumer<MessageCreateSpec>> specMono;
	private final Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems;
	private final Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems;
	private final boolean deleteMenuOnClose;
	private final boolean deleteMenuOnTimeout;
	private final boolean closeAfterMessage;
	private final boolean closeAfterReaction;

	private InteractiveMenu(Mono<Consumer<MessageCreateSpec>> specMono,
			Map<String, Function<MessageMenuInteraction, Mono<Void>>> messageItems,
			Map<String, Function<ReactionMenuInteraction, Mono<Void>>> reactionItems, boolean deleteMenuOnClose,
			boolean deleteMenuOnTimeout, boolean closeAfterMessage, boolean closeAfterReaction) {
		this.specMono = specMono;
		this.messageItems = messageItems;
		this.reactionItems = reactionItems;
		this.deleteMenuOnClose = deleteMenuOnClose;
		this.deleteMenuOnTimeout = deleteMenuOnTimeout;
		this.closeAfterMessage = closeAfterMessage;
		this.closeAfterReaction = closeAfterReaction;
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
		return new InteractiveMenu(specMono, Map.of(), Map.of(), false, false, true, true);
	}

	/**
	 * Creates a new InteractiveMenu prefilled with menu items useful for
	 * pagination.
	 * 
	 * @param currentPage an AtomicInteger that stores the current page number
	 * @param controls    the emojis to use for reaction-based navigation controls
	 * @param paginator   a Function that generates the message to display according
	 *                    to the current page number. If the page number is out of
	 *                    range, the function may throw a
	 *                    {@link PageNumberOutOfRangeException} which is handled by
	 *                    default to cover cases where the user inputs an invalid
	 *                    page number. Note that if the paginator function throws
	 *                    {@link PageNumberOutOfRangeException} with min/max values
	 *                    that aren't the same depending on the current page number,
	 *                    the behavior of the InteractiveMenu will be undefined.
	 * @return a new InteractiveMenu prefilled with menu items useful for
	 *         pagination.
	 */
	public static InteractiveMenu createPaginated(AtomicInteger currentPage, PaginationControls controls, IntFunction<UniversalMessageSpec> paginator) {
		requireNonNull(paginator);
		return createAsyncPaginated(currentPage, controls, p -> Mono.just(paginator.apply(p)));
	}

	/**
	 * Creates a new InteractiveMenu prefilled with menu items useful for
	 * pagination. Unlike
	 * {@link #createPaginated(AtomicInteger, PaginationControls, IntFunction)},
	 * this method support asynchronous paginator functions.
	 * 
	 * @param currentPage    an AtomicInteger that stores the current page number
	 * @param controls    the emojis to use for reaction-based navigation controls
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
	public static InteractiveMenu createAsyncPaginated(AtomicInteger currentPage, PaginationControls controls, IntFunction<Mono<UniversalMessageSpec>> asyncPaginator) {
		requireNonNull(currentPage);
		requireNonNull(controls);
		requireNonNull(asyncPaginator);
		var oldPage = new AtomicInteger();
		return create(asyncPaginator.apply(currentPage.get()).map(UniversalMessageSpec::toMessageCreateSpec))
				.addReactionItem(controls.getPreviousEmoji(), interaction -> Mono.fromCallable(currentPage::decrementAndGet)
						.flatMap(targetPage -> asyncPaginator.apply(targetPage)
								.map(UniversalMessageSpec::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono
								.just(currentPage.get() + e.getMaxPage() - e.getMinPage() + 1)
								.doOnNext(currentPage::set)
								.flatMap(targetPage -> asyncPaginator.apply(targetPage)
										.map(UniversalMessageSpec::toMessageEditSpec)))
						.flatMap(interaction.getMenuMessage()::edit)
						.then())
				.addReactionItem(controls.getNextEmoji(), interaction -> Mono.fromCallable(currentPage::incrementAndGet)
						.flatMap(targetPage -> asyncPaginator.apply(targetPage).map(UniversalMessageSpec::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono
								.just(currentPage.get() - e.getMaxPage() + e.getMinPage() - 1)
								.doOnNext(currentPage::set)
								.flatMap(targetPage -> asyncPaginator.apply(targetPage)
										.map(UniversalMessageSpec::toMessageEditSpec)))
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
								.map(UniversalMessageSpec::toMessageEditSpec)
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

	public InteractiveMenu addMessageItem(String message, Function<MessageMenuInteraction, Mono<Void>> action) {
		requireNonNull(message);
		requireNonNull(action);
		var newMessageItems = new LinkedHashMap<>(messageItems);
		newMessageItems.put(message, action);
		return new InteractiveMenu(specMono, Collections.unmodifiableMap(newMessageItems), reactionItems, deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterMessage, closeAfterReaction);
	}

	public InteractiveMenu addReactionItem(String emojiName, Function<ReactionMenuInteraction, Mono<Void>> action) {
		requireNonNull(emojiName);
		requireNonNull(action);
		var newReactionItems = new LinkedHashMap<>(reactionItems);
		newReactionItems.put(emojiName, action);
		return new InteractiveMenu(specMono, messageItems, Collections.unmodifiableMap(newReactionItems), deleteMenuOnClose,
				deleteMenuOnTimeout, closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnClose(boolean deleteMenuOnClose) {
		return new InteractiveMenu(specMono, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu deleteMenuOnTimeout(boolean deleteMenuOnTimeout) {
		return new InteractiveMenu(specMono, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterMessage(boolean closeAfterMessage) {
		return new InteractiveMenu(specMono, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
	}
	
	public InteractiveMenu closeAfterReaction(boolean closeAfterReaction) {
		return new InteractiveMenu(specMono, messageItems, reactionItems, deleteMenuOnClose, deleteMenuOnTimeout,
				closeAfterMessage, closeAfterReaction);
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
		var closeNotifier = MonoProcessor.<Void>create();
		return specMono.flatMap(ctx::reply)
				.flatMap(menuMessage -> addReactionsToMenu(ctx, menuMessage))
				.flatMap(menuMessage -> Mono.first(
						closeNotifier,
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
									var replyCtx = new MessageMenuInteraction(menuMessage, closeNotifier, event, new ArgumentList(args), flags);
									return action.apply(replyCtx).thenReturn(0);
								})
								.takeUntil(__ -> closeAfterMessage)
								.onErrorResume(UnexpectedReplyException.class, e -> ctx.reply(":no_entry_sign: " + e.getMessage()).then(Mono.error(e)))
								.retry(UnexpectedReplyException.class::isInstance)
								.then(),
						ctx.getBot().getDiscordClients()
								.flatMap(client -> Flux.merge(
										client.getEventDispatcher().on(ReactionAddEvent.class),
										client.getEventDispatcher().on(ReactionRemoveEvent.class)))
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
						.then(handleTermination(menuMessage, deleteMenuOnClose))
						.timeout(Duration.ofSeconds(ctx.getBot().getInteractiveMenuTimeout()),
								handleTermination(menuMessage, deleteMenuOnTimeout)));
	}
	
	private static Mono<Void> handleTermination(Message menuMessage, boolean shouldDelete) {
		return (shouldDelete 
						? menuMessage.delete()
						: menuMessage.removeAllReactions())
				.onErrorResume(e -> Mono.empty());
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
