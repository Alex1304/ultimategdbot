package com.github.alex1304.ultimategdbot.core.handler;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.utils.Utils;
import com.github.alex1304.ultimategdbot.core.impl.ContextImpl;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReplyMenuHandler implements Handler {
	
	private final Bot bot;
	private final Map<String, ReplyMenu> openedReplyMenus;
	private final Map<ReplyMenu, Disposable> disposableMenus;
	
	private final class ReplyMenu {
		final Context ctx;
		final Message msg;
		final Map<String, Function<Context, Mono<Void>>> menuItems;
		final boolean deleteOnReply;
		final boolean deleteOnTimeout;
		ReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout) {
			this.ctx = ctx;
			this.msg = msg;
			this.menuItems = menuItems;
			this.deleteOnReply = deleteOnReply;
			this.deleteOnTimeout = deleteOnTimeout;
		}
		String toKey() {
			return msg.getChannelId().asString() + ctx.getEvent().getMessage().getAuthor().get().getId().asString();
		}
		void complete() {
			if (deleteOnReply) {
				msg.delete().doOnError(__ -> {}).subscribe();
			}
			openedReplyMenus.remove(toKey());
			var d = disposableMenus.remove(this);
			if (d != null) {
				d.dispose();
			}
		}
		void timeout() {
			if (deleteOnTimeout) {
				msg.delete().doOnError(__ -> {}).subscribe();
			}
			openedReplyMenus.remove(toKey());
			disposableMenus.remove(this);
		}
	}

	public ReplyMenuHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.openedReplyMenus = new ConcurrentHashMap<>();
		this.disposableMenus = new ConcurrentHashMap<>();
	}

	@Override
	public void listen() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class).subscribeOn(Schedulers.elastic())
				.filter(event -> event.getMessage().getContent().isPresent()
						&& event.getMessage().getAuthor().isPresent()
						&& openedReplyMenus.containsKey(event.getMessage().getChannelId().asString()
								+ event.getMessage().getAuthor().get().getId().asString()))
				.map(event -> new ContextImpl(event, Utils.parseArgs(event.getMessage().getContent().get()), bot))
				.subscribe(ctx -> {
					var replyMenu = openedReplyMenus.get(ctx.getEvent().getMessage().getChannelId().asString()
							+ ctx.getEvent().getMessage().getAuthor().get().getId().asString());
					var replyItem = ctx.getArgs().get(0);
					var action = replyMenu.menuItems.get(replyItem);
					if (action == null) {
						return;
					}
					Command.invoke(action, ctx);
					replyMenu.complete();
					if (replyMenu.deleteOnReply) {
						ctx.getEvent().getMessage().delete().doOnError(__ -> {}).subscribe();
					}
				});
	}
	
	public String open(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout) {
		if (!msg.getAuthor().isPresent()) {
			return "";
		}
		var rm = new ReplyMenu(ctx, msg, menuItems, deleteOnReply, deleteOnTimeout);
		var key = rm.toKey();
		var existing = openedReplyMenus.get(key);
		if (existing != null) {
			existing.timeout();
		}
		openedReplyMenus.put(key, rm);
		disposableMenus.put(rm, Mono.delay(Duration.ofSeconds(bot.getReplyMenuTimeout())).subscribe(__ -> rm.timeout()));
		return key;
	}

	public void close(String identifier) {
		var rm = openedReplyMenus.get(identifier);
		if (rm == null) {
			return;
		}
		rm.msg.delete().doOnError(__ -> {}).subscribe();
		rm.complete();
	}
}
