package com.github.alex1304.ultimategdbot.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.entity.GuildSettings;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public class ContextImpl implements Context {
	
	private final MessageCreateEvent event;
	private final List<String> args;
	private final Bot bot;
	private final GuildSettings guildSettings; // null if DM message
	private Map<String, Object> variables;

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
		this.variables = new ConcurrentHashMap<>();
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
		return reply(spec -> spec.setContent(message));
	}

	@Override
	public Mono<Message> reply(Consumer<? super MessageCreateSpec> spec) {
		return event.getMessage().getChannel().flatMap(c -> c.createMessage(spec)).doOnError(e -> {
			if (!(e instanceof ClientException)) {
				return;
			}
			var ce = (ClientException) e;
			event.getMessage().getAuthor().flatMap(a -> a.getPrivateChannel())
					.flatMap(pc -> pc.createMessage("I was unable to send a reply to your command in <#"
							+ event.getMessage().getChannelId()
							+ ">. Make sure that I have permissions to talk and send embeds there.\nError response: `"
							+ ce.getErrorResponse() + "`"))
					.doOnError(__ -> {})
					.subscribe();
		});
	}

	@Override
	public String getEffectivePrefix() {
		return guildSettings == null ? bot.getDefaultPrefix() : guildSettings.getPrefix();
	}

	@Override
	public void setVar(String name, Object val) {
		variables.put(name, val);
	}

	@Override
	public void setVarIfNotExists(String name, Object val) {
		if (variables.containsKey(name)) {
			return;
		}
		setVar(name, val);
	}

	@Override
	public <T> T getVar(String name, Class<T> type) {
		var val = variables.get(name);
		if (val == null || !type.isInstance(val)) {
			return null;
		}
		return type.cast(val);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getVarOrDefault(String name, T defaultVal) {
		var val = variables.getOrDefault(name, defaultVal);
		if (!defaultVal.getClass().isInstance(val)) {
			return defaultVal;
		}
		return (T) val;
	}

	@Override
	public Map<Plugin, Map<String, String>> getGuildSettings0() {
		var map = new HashMap<Plugin, Map<String, String>>();
		for (var plugin : bot.getPlugins()) {
			var entries = new HashMap<String, String>();
			plugin.getGuildConfigurationEntries().forEach((k, v) -> {
				//entries.put(k, v.get);
			});
		}
		return null;
	}

	@Override
	public void setGuildSetting(String key, String val) {
		// TODO Auto-generated method stub
		
	}
}
