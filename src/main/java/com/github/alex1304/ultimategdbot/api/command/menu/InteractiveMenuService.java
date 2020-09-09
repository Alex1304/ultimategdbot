package com.github.alex1304.ultimategdbot.api.command.menu;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.BotConfig;
import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.command.CommandService;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiService;
import com.github.alex1304.ultimategdbot.api.util.MessageSpecTemplate;
import com.github.alex1304.ultimategdbot.api.util.MessageUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public final class InteractiveMenuService {
	
	public static final String CONFIG_RESOURCE_NAME = "interactive_menu";
	
	private final GatewayDiscordClient gateway;
	private final CommandService commandService;
	private final EmojiService emojiService;
	
	private final Duration timeout;
	private final PaginationControls controls;

	public InteractiveMenuService(BotConfig botConfig, GatewayDiscordClient gateway, CommandService commandService,
			EmojiService emojiService) {
		this.gateway = gateway;
		this.commandService = commandService;
		this.emojiService = emojiService;
		var config = botConfig.resource(CONFIG_RESOURCE_NAME);
		this.timeout = config.readOptional("interactive_menu.timeout_seconds")
				.map(Integer::parseInt)
				.map(Duration::ofSeconds)
				.orElse(Duration.ofMinutes(10));
		this.controls = new PaginationControls(
				config.readOptional("interactive_menu.controls.previous").orElse(PaginationControls.DEFAULT_PREVIOUS_EMOJI),
				config.readOptional("interactive_menu.controls.next").orElse(PaginationControls.DEFAULT_NEXT_EMOJI),
				config.readOptional("interactive_menu.controls.close").orElse(PaginationControls.DEFAULT_CLOSE_EMOJI));
	}

	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt.
	 * 
	 * @param messageCreateSpec the spec to build the menu message
	 * @return a new InteractiveMenu
	 */
	public InteractiveMenu create(Consumer<MessageCreateSpec> messageCreateSpec) {
		requireNonNull(messageCreateSpec);
		return create((Translator tr) -> Mono.just(messageCreateSpec));
	}
	
	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt.
	 * 
	 * @param message the menu message
	 * @return a new InteractiveMenu
	 */
	public InteractiveMenu create(String message) {
		requireNonNull(message);
		return create((MessageCreateSpec mcs) -> mcs.setContent(message));
	}
	
	/**
	 * Creates a new empty InteractiveMenu with a given message that will serve as
	 * menu prompt. The menu message may be supplied from an asynchronous source.
	 * 
	 * @param source a Function accepting a translator and asynchronusly generating
	 *               the message of the menu
	 * @return a new InteractiveMenu
	 */
	public InteractiveMenu create(Function<Translator, Mono<Consumer<MessageCreateSpec>>> source) {
		requireNonNull(source);
		return new InteractiveMenu(gateway, commandService, emojiService, source, timeout);
	}
	
	/**
	 * Creates a new {@link InteractiveMenu} splitting the given text into several
	 * pages, returning a paginated {@link InteractiveMenu} if it contains 2 pages
	 * or more.
	 * 
	 * <p>
	 * If the whole text fits in one page (that is, if
	 * <code>text.length() &lt;= pageLength</code>), it will create a simple
	 * {@link InteractiveMenu} with no pagination items.
	 * </p>
	 * 
	 * <p>
	 * Pages will always be made so it doesn't cut at the middle of a line, and will
	 * properly close codeblock markdowns, as specified in
	 * {@link MessageUtils#chunk(String, int)}
	 * </p>
	 * 
	 * @param text       the text to paginate
	 * @param pageLength the max characters per page
	 * @return a new {@link InteractiveMenu}
	 */
	public InteractiveMenu createPaginated(String text, int pageLength) {
		if (text.length() <= pageLength) {
			return create(text).closeAfterMessage(false).closeAfterReaction(false);
		}
		var parts = MessageUtils.chunk(text, pageLength);
		return createPaginated((tr, page) -> {
			PageNumberOutOfRangeException.check(page, 0, parts.size() - 1);
			return new MessageSpecTemplate(parts.get(page), embed -> embed.addField(
					tr.translate("CommonStrings", "pagination_page_counter", page + 1, parts.size()),
					tr.translate("CommonStrings", "pagination_go_to"), true));
		});
	}

	/**
	 * Creates a new {@link InteractiveMenu} prefilled with menu items useful for
	 * pagination.
	 * 
	 * @param paginator a Function that generates the message to display according
	 *                  to the current page number. If the page number is out of
	 *                  range, the function may throw a
	 *                  {@link PageNumberOutOfRangeException} which is handled by
	 *                  default to cover cases where the user inputs an invalid page
	 *                  number. Note that if the paginator function throws
	 *                  {@link PageNumberOutOfRangeException} with min/max values
	 *                  that aren't the same depending on the current page number,
	 *                  the behavior of the InteractiveMenu will be undefined.
	 * @return a new InteractiveMenu prefilled with menu items useful for
	 *         pagination.
	 */
	public InteractiveMenu createPaginated(BiFunction<Translator, Integer, MessageSpecTemplate> paginator) {
		requireNonNull(paginator);
		return createAsyncPaginated((tr, page) -> Mono.just(paginator.apply(tr, page)));
	}

	/**
	 * Creates a new {@link InteractiveMenu} prefilled with menu items useful for
	 * pagination. Unlike {@link #createPaginated(BiFunction)}
	 * this method support asynchronous paginator functions.
	 * 
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
	public InteractiveMenu createAsyncPaginated(BiFunction<Translator, Integer, Mono<MessageSpecTemplate>> asyncPaginator) {
		requireNonNull(asyncPaginator);
		return create((Translator tr) -> asyncPaginator.apply(tr, 0).map(MessageSpecTemplate::toMessageCreateSpec))
				.withInteractionContext(context -> context.put("currentPage", 0))
				.addReactionItem(controls.getPreviousEmoji(), interaction -> Mono.fromCallable(
								() -> interaction.update("currentPage", x -> x - 1, -1))
						.flatMap(targetPage -> asyncPaginator.apply(interaction.getTranslator(), targetPage)
								.map(MessageSpecTemplate::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono.fromCallable(
										 () -> interaction.update("currentPage",
												 x -> x + e.getMaxPage() - e.getMinPage() + 1, 0))
								.flatMap(targetPage -> asyncPaginator.apply(interaction.getTranslator(), targetPage)
										.map(MessageSpecTemplate::toMessageEditSpec)))
						.flatMap(interaction.getMenuMessage()::edit)
						.then())
				.addReactionItem(controls.getNextEmoji(), interaction -> Mono.fromCallable(
								() -> interaction.update("currentPage", x -> x + 1, 1))
						.flatMap(targetPage -> asyncPaginator.apply(interaction.getTranslator(), targetPage)
								.map(MessageSpecTemplate::toMessageEditSpec))
						.onErrorResume(PageNumberOutOfRangeException.class, e -> Mono.fromCallable(
										 () -> interaction.update("currentPage",
												 x -> x - e.getMaxPage() + e.getMinPage() - 1, 0))
								.flatMap(targetPage -> asyncPaginator.apply(interaction.getTranslator(), targetPage)
										.map(MessageSpecTemplate::toMessageEditSpec)))
						.flatMap(interaction.getMenuMessage()::edit)
						.then())
				.addMessageItem("page", interaction -> Mono.fromCallable(() -> Integer.parseInt(interaction.getArgs().get(1)))
						.onErrorMap(IndexOutOfBoundsException.class, e -> new UnexpectedReplyException(
								interaction.getTranslator().translate("CommonStrings", "pagination_page_number_not_specified")))
						.onErrorMap(NumberFormatException.class, e -> new UnexpectedReplyException(
								interaction.getTranslator().translate("CommonStrings", "pagination_page_number_invalid")))
						.map(p -> p - 1)
						.doOnNext(targetPage -> {
							interaction.set("oldPage", interaction.get("currentPage"));
							interaction.set("currentPage", targetPage);
						})
						.flatMap(targetPage -> asyncPaginator.apply(interaction.getTranslator(), targetPage)
								.map(MessageSpecTemplate::toMessageEditSpec)
								.flatMap(interaction.getMenuMessage()::edit))
						.onErrorMap(PageNumberOutOfRangeException.class, e -> {
							interaction.set("currentPage", interaction.get("oldPage"));
							return new UnexpectedReplyException(interaction.getTranslator().translate("CommonStrings",
									"pagination_page_number_out_of_range", e.getMinPage() + 1, e.getMaxPage() + 1));
						})
						.then(interaction.getEvent().getMessage().delete().onErrorResume(e -> Mono.empty())))
				.addReactionItem(controls.getCloseEmoji(), interaction -> Mono.fromRunnable(interaction::closeMenu))
				.closeAfterMessage(false)
				.closeAfterReaction(false);
	}
	
	/**
	 * Gets the pagination controls used by this service.
	 * 
	 * @return the controls
	 */
	public PaginationControls getPaginationControls() {
		return controls;
	}
}
