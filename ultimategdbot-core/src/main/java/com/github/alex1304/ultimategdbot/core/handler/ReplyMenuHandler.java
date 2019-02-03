package com.github.alex1304.ultimategdbot.core.handler;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.core.impl.ContextImpl;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class ReplyMenuHandler implements Handler {
	
	private final Bot bot;
	private final Map<String, ReplyMenu> openedReplyMenus;
	private final Map<ReplyMenu, Disposable> disposableMenus;
	
	private final class ReplyMenu {
		final Context ctx;
		final Message msg;
		final Map<String, Function<Context, Mono<Void>>> menuItems;
		ReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems) {
			this.ctx = ctx;
			this.msg = msg;
			this.menuItems = menuItems;
		}
		String toKey() {
			return msg.getChannelId().asString() + ctx.getEvent().getMessage().getAuthorId().get().asString();
		}
		void close() {
			msg.delete().subscribe();
			openedReplyMenus.remove(toKey());
			disposableMenus.remove(this).dispose();
		}
	}

	public ReplyMenuHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.openedReplyMenus = new ConcurrentHashMap<>();
		this.disposableMenus = new ConcurrentHashMap<>();
	}

	@Override
	public void prepare() {
	}

	@Override
	public void listen() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
				.filter(event -> event.getMessage().getContent().isPresent()
						&& event.getMessage().getAuthorId().isPresent()
						&& openedReplyMenus.containsKey(event.getMessage().getChannelId().asString()
								+ event.getMessage().getAuthorId().get().asString()))
				.map(event -> new ContextImpl(event, Arrays.asList(event.getMessage().getContent().get().split(" +")), bot))
				.subscribe(ctx -> {
					var replyMenu = openedReplyMenus.get(ctx.getEvent().getMessage().getChannelId().asString()
							+ ctx.getEvent().getMessage().getAuthorId().get().asString());
					var action = replyMenu.menuItems.get(ctx.getArgs().get(0));
					if (action == null) {
						return;
					}
					replyMenu.close();
					Command.invoke(action, ctx);
				});
	}
	
	public void open(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems) {
		if (!msg.getAuthorId().isPresent()) {
			return;
		}
		var rm = new ReplyMenu(ctx, msg, menuItems);
		var existing = openedReplyMenus.put(rm.toKey(), rm);
		if (existing != null) {
			existing.close();
		}
		disposableMenus.put(rm, Mono.delay(Duration.ofSeconds(10)).subscribe(__ -> rm.close()));
	}
}
