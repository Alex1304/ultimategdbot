package com.github.alex1304.ultimategdbot.api.utils.reply;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;

/**
 * Builds a reply menu.
 */
public class ReplyMenuBuilder {
	private final class Entry {
		final String view;
		final Function<Context, Mono<Void>> action;
		Entry(String view, Function<Context, Mono<Void>> action) {
			this.view = Objects.requireNonNull(view);
			this.action = Objects.requireNonNull(action);
		}
	}
	final Map<String, Entry> menuEntries;
	final Context ctx;
	private final boolean deleteOnReply, deleteOnTimeout;
	private String header;
	
	public ReplyMenuBuilder(Context ctx, boolean deleteOnReply, boolean deleteOnTimeout) {
		this.menuEntries = new LinkedHashMap<>();
		this.ctx = Objects.requireNonNull(ctx);
		this.deleteOnReply = deleteOnReply;
		this.deleteOnTimeout = deleteOnTimeout;
		this.header = "_ _";
	}

	public final void addItem(String key, String view, Function<Context, Mono<Void>> action) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(view);
		Objects.requireNonNull(action);
		menuEntries.put(key, new Entry(view, action));
	}
	
	private final void addItem(String key, String view) {
		addItem(key, view, __ -> Mono.empty());
	}
	
	public void setHeader(String header) {
		Objects.requireNonNull(header);
		if (header.isBlank()) {
			throw new IllegalArgumentException("Header must not be blank");
		}
		this.header = header;
	}
	
	public Mono<Message> build(String content, Consumer<EmbedCreateSpec> embed) {
		var closeView = "To close this menu, type `close`";
		var timeout = Duration.ofSeconds(ctx.getBot().getReplyMenuTimeout());
		if (deleteOnTimeout) {
			var time = BotUtils.formatTimeMillis(timeout);
			closeView = "This menu will close after " + time + " of inactivity, or type `close`";
		}
		addItem("close", closeView);
		var menuItems = new LinkedHashMap<String, Function<Context, Mono<Void>>>();
		return ctx.reply(mcs -> {
			var sb = new StringBuilder();
			menuEntries.forEach((k, v) -> {
				menuItems.put(k.toLowerCase(), v.action);
				if (!v.view.isEmpty()) {
					sb.append(v.view);
					sb.append('\n');
				}
			});
			if (content != null && !content.isBlank()) {
				mcs.setContent(content);
			}
			mcs.setEmbed(embed.andThen(ecs -> ecs.addField(header, sb.toString(), false)));
		})
		.doOnNext(message -> menuItems.put("close", ctx0 -> message.delete().onErrorResume(e -> Mono.empty())))
		.doOnNext(message -> ctx.getBot().getDiscordClients().flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
				.filter(event -> event.getMessage().getAuthor().isPresent())
				.filter(event -> event.getMessage().getAuthor().equals(ctx.getEvent().getMessage().getAuthor()))
				.filter(event -> event.getMessage().getChannelId().equals(ctx.getEvent().getMessage().getChannelId()))
				.map(event -> Tuples.of(event, BotUtils.parseArgs(event.getMessage().getContent().orElse(""))))
				.filter(TupleUtils.predicate((event, args) -> menuItems.containsKey(args.get(0))))
				.map(TupleUtils.function((event, args) -> Tuples.of(event, args, menuItems.get(args.get(0)))))
				.flatMap(TupleUtils.function((event, args, action) -> {
					var originalCommand = ctx.getCommand();
					var newCommand = Command.forkedFrom(originalCommand, action);
					var newCtx = new Context(newCommand, event, args, ctx.getBot(), "");
					return ctx.getBot().getCommandKernel().invokeCommand(newCommand, newCtx);
				}))
				.timeout(timeout)
				.then(deleteOnReply ? message.delete().onErrorResume(e -> Mono.empty()) : Mono.empty())
				.onErrorResume(TimeoutException.class, __ -> deleteOnTimeout ? message.delete().onErrorResume(e -> Mono.empty()) : Mono.empty())
				.retry()
				.subscribe());
	}
	
	public final Mono<Message> build(String content) {
		return build(content, ecs -> {});
	}
}