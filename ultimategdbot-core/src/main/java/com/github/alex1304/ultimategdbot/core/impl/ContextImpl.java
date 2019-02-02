package com.github.alex1304.ultimategdbot.core.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.entity.GuildSettings;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class ContextImpl implements Context {
	
	private final MessageCreateEvent event;
	private final List<String> args;
	private final Bot bot;
	private final GuildSettings guildSettings; // null if DM message

	public ContextImpl(MessageCreateEvent event, List<String> args, Bot bot) {
		this.event = Objects.requireNonNull(event);
		this.args = Objects.requireNonNull(args);
		this.bot = Objects.requireNonNull(bot);
		var guildIdOpt = event.getGuildId();
		if (!guildIdOpt.isPresent()) {
			this.guildSettings = null;
			return;
		}
		var id = guildIdOpt.get().asLong();
		var guildSettings = bot.getDatabase().findByID(GuildSettings.class, id);
		if (guildSettings == null) {
			guildSettings = new GuildSettings();
			guildSettings.setGuildId(id);
			guildSettings.setPrefix(bot.getDefaultPrefix());
			bot.getDatabase().save(guildSettings);
		}
		this.guildSettings = guildSettings;
	}

	@Override
	public MessageCreateEvent getEvent() {
		return event;
	}

	@Override
	public List<String> getArgs() {
		return args;
	}

	@Override
	public Bot getBot() {
		return bot;
	}

	@Override
	public GuildSettings getGuildSettings() {
		return guildSettings;
	}

	@Override
	public Mono<Message> reply(String message) {
		return event.getMessage().getChannel().flatMap(c -> c.createMessage(message));
	}

	@Override
	public Mono<Message> reply(Consumer<? super MessageCreateSpec> spec) {
		return event.getMessage().getChannel().flatMap(c -> c.createMessage(spec));
	}
}
