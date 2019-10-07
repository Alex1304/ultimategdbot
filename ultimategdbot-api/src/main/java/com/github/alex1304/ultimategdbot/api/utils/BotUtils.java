package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.menu.InteractiveMenu;
import com.github.alex1304.ultimategdbot.api.utils.menu.PageNumberOutOfRangeException;
import com.github.alex1304.ultimategdbot.api.utils.menu.PaginationControls;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contains various utility methods.
 */
public class BotUtils {
	private BotUtils() {
	}
	
	/**
	 * Splits a message into several chunks which size is specified. If the chunk
	 * ends while the text is inside a codeblock or a blockquote, proper markdown is
	 * added to make the message continuous across chunks. This does not apply to
	 * inline markdown such as bold, italic or spoilers.
	 * 
	 * @param superLongMessage the message to split
	 * @param maxCharacters    the max characters that a single chunk may have
	 * @return a List which elements are the chunks in the correct order
	 */
	public static List<String> splitMessage(String superLongMessage, int maxCharacters) {
		var chunks = new ArrayList<String>();
		var currentChunk = new StringBuilder();
		var inCodeblock = false;
		var inBlockquote = false;
		for (var line : superLongMessage.lines().collect(Collectors.toList())) {
			inCodeblock = line.startsWith("```") && !line.substring(3).contains("```") ? !inCodeblock : inCodeblock;
			inBlockquote = inBlockquote || line.startsWith(">>> ");
			if (currentChunk.length() + line.length() + 1 >= maxCharacters) {
				if (inCodeblock) {
					currentChunk.append("```\n");
				}
				chunks.add(currentChunk.substring(0, Math.min(currentChunk.length(), maxCharacters)).toString());
				currentChunk.delete(0, currentChunk.length());
			} else {
				if (!chunks.isEmpty() && currentChunk.length() == 0) {
					if (inBlockquote) {
						currentChunk.append(">>> ");
					}
					if (inCodeblock) {
						currentChunk.append("```\n");
					}
				}
			}
			currentChunk.append(line);
			currentChunk.append('\n');
		}
		chunks.add(currentChunk.toString());
		return chunks;
	}
	
	/**
	 * Splits a message into several chunks. Each chunk can have a max size of
	 * {@link Message#MAX_CONTENT_LENGTH} - 10.
	 * 
	 * @param superLongMessage the message to split
	 * @return a List which elements are the chunks in the correct order
	 */
	public static List<String> splitMessage(String superLongMessage) {
		return splitMessage(superLongMessage, Message.MAX_CONTENT_LENGTH - 10);
	}
	
	/**
	 * Formats a Duration into a human readable String.
	 * 
	 * @param time the duration to format
	 * @return the formatted duration
	 */
	public static String formatDuration(Duration time) {
		var result = (time.toDaysPart() > 0 ? time.toDaysPart() + "d " : "")
				+ (time.toHoursPart() > 0 ? time.toHoursPart() + "h " : "")
				+ (time.toMinutesPart() > 0 ? time.toMinutesPart() + "min " : "")
				+ (time.toSecondsPart() > 0 ? time.toSecondsPart() + "s " : "")
				+ (time.toMillisPart() > 0 ? time.toMillisPart() + "ms " : "");
		return result.isEmpty() ? "0ms" : result.substring(0, result.length() - 1);
	}
	
	/**
	 * Sends an error report to the debug log channel.
	 * 
	 * @param header the first sentence to write on the report
	 * @param ctx    the context of the error
	 * @param error  the error itself
	 * @return a Flux of messages that were sent in the debug log channel. If the
	 *         error report exceeds {@link Message#MAX_CONTENT_LENGTH} characters, it will be split using
	 *         {@link #splitMessage(String)}, in this case the Flux may emit more
	 *         than one message instance.
	 */
	public static Flux<Message> debugError(String header, Context ctx, Throwable error) {
		Objects.requireNonNull(header, "header was null");
		Objects.requireNonNull(ctx, "ctx was null");
		Objects.requireNonNull(error, "error was null");
		var sb = new StringBuilder(header)
				.append("\nContext dump: `")
				.append(ctx)
				.append("`\nException thrown: `");
		var separator = "";
		for (var current = error ; current != null ; current = current.getCause()) {
			sb.append(separator)
					.append(current.getClass().getCanonicalName())
					.append(": ")
					.append(current.getMessage())
					.append("`\n");
			separator = "Caused by: `";
		}
		return ctx.getBot().getDebugLogChannel()
				.ofType(MessageChannel.class)
				.flatMapMany(c -> Flux.fromIterable(splitMessage(sb.toString()))
						.flatMap(c::createMessage));
	}
	
	// "◀", "▶"
	public static Mono<Void> sendPaginatedMessage(Context ctx, String text, PaginationControls controls, int pageLength) {
		Objects.requireNonNull(ctx);
		Objects.requireNonNull(text);
		Objects.requireNonNull(controls);
		if (text.length() <= pageLength) {
			return ctx.reply(text).then();
		}
		var parts = splitMessage(text, pageLength);
		return InteractiveMenu.createPaginated(new AtomicInteger(), controls, page -> {
					PageNumberOutOfRangeException.check(page, 0, parts.size() - 1);
					return new UniversalMessageSpec(parts.get(page), embed -> embed.addField("Page " + (page + 1) + "/" + parts.size(),
							"To go to a specific page, type `page <number>`, e.g `page 3`", true));
				})
				.open(ctx);
	}
	
	public static Mono<Void> sendPaginatedMessage(Context ctx, String text, PaginationControls controls) {
		return sendPaginatedMessage(ctx, text, controls, 800);
	}
	
	public static Mono<Void> sendPaginatedMessage(Context ctx, String text) {
		return sendPaginatedMessage(ctx, text, ctx.getBot().getDefaultPaginationControls(), 800);
	}
}
