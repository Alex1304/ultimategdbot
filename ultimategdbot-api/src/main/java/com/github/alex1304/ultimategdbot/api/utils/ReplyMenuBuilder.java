package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Context;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

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
	
	private final Map<String, Entry> menuEntries;
	private final Context ctx;
	private final boolean deleteOnReply, deleteOnTimeout;
	
	public ReplyMenuBuilder(Context ctx, boolean deleteOnReply, boolean deleteOnTimeout) {
		this.menuEntries = new LinkedHashMap<>();
		this.ctx = Objects.requireNonNull(ctx);
		this.deleteOnReply = deleteOnReply;
		this.deleteOnTimeout = deleteOnTimeout;
	}

	public void addItem(String key, String view, Function<Context, Mono<Void>> action) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(view);
		Objects.requireNonNull(action);
		menuEntries.put(key, new Entry(view, action));
	}
	
	private void addItem(String key, String view) {
		addItem(key, view, __ -> Mono.empty());
	}
	
	public void removeItem(String key) {
		menuEntries.remove(key);
	}
	
	public Mono<Message> build(String content, Consumer<EmbedCreateSpec> embed) {
		var closeView = "To close this menu, type `close`";
		if (deleteOnTimeout) {
			var timeout = Duration.ofSeconds(ctx.getBot().getReplyMenuTimeout());
			var time = (timeout.toMinutesPart() > 0 ? timeout.toMinutesPart() + "min " : "")
					+ (timeout.toSecondsPart() > 0 ? timeout.toSecondsPart() + "s " : "");
			closeView = "This menu will close after " + time + " of inactivity, or type `close`";
		}
		addItem("close", closeView);
		var menuItems = new LinkedHashMap<String, Function<Context, Mono<Void>>>();
		return ctx.reply(mcs -> {
			var sb = new StringBuilder();
			menuEntries.forEach((k, v) -> {
				menuItems.put(k, v.action);
				if (!v.view.isEmpty()) {
					sb.append(v.view);
					sb.append('\n');
				}
			});
			if (content != null && !content.isBlank()) {
				mcs.setContent(content);
			}
			mcs.setEmbed(embed.andThen(ecs -> ecs.addField("Actions:", sb.toString(), false)));
		}).doOnNext(message -> {
			var rmid = ctx.getBot().openReplyMenu(ctx, message, menuItems, deleteOnReply, deleteOnTimeout);
			menuItems.put("close", ctx0 -> {
				ctx0.getBot().closeReplyMenu(rmid);
				return Mono.empty();
			});
		});
	}
	
	public Mono<Message> build(String content) {
		return build(content, ecs -> {});
	}
}