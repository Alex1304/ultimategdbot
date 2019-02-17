package com.github.alex1304.ultimategdbot.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.guildsettings.NativeGuildSettings;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public class ContextImpl implements Context {
	
	private final MessageCreateEvent event;
	private final List<String> args;
	private final Bot bot;
	private final NativeGuildSettings guildSettings; // null if DM message
	private final Map<String, Object> variables;
	private String effectivePrefix;
	private boolean hasPrefixBeenChecked;

	public ContextImpl(MessageCreateEvent event, List<String> args, Bot bot) {
		this.event = Objects.requireNonNull(event);
		this.args = Objects.requireNonNull(args);
		this.bot = Objects.requireNonNull(bot);
		this.variables = new ConcurrentHashMap<>();
		var guildIdOpt = event.getGuildId();
		if (!guildIdOpt.isPresent()) {
			this.guildSettings = null;
			return;
		}
		var id = guildIdOpt.get().asLong();
		var guildSettings = bot.getDatabase().findByIDOrCreate(NativeGuildSettings.class, id, (gs, gid) -> {
			gs.setGuildId(gid);
			gs.setPrefix(bot.getDefaultPrefix());
		});
		this.guildSettings = guildSettings;
		this.effectivePrefix = null;
		this.hasPrefixBeenChecked = false;
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
			var author = event.getMessage().getAuthor();
			if (!author.isPresent()) {
				return;
			}
			author.get().getPrivateChannel()
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
		if (hasPrefixBeenChecked) {
			return effectivePrefix;
		}
		this.hasPrefixBeenChecked = true;
		var content = event.getMessage().getContent().orElse(null);
		if (content == null) {
			return null;
		}
		var botId = bot.getDiscordClient().getSelfId();
		var mention = "";
		var mentionNick = "";
		if (botId.isPresent()) {
			mention = "<@" + botId.get().asString() + "> ";
			mentionNick = "<@!" + botId.get().asString() + "> ";
		}
		final Predicate<String> isPrefix = str -> {
			var res = str.equalsIgnoreCase(content.substring(0, Math.min(str.length(), content.length())));
			if (res) {
				this.effectivePrefix = str;
			}
			return res;
		};
		if (!mention.isEmpty() && isPrefix.test(mention)) {
			return mention;
		} else if (!mentionNick.isEmpty() && isPrefix.test(mentionNick)) {
			return mentionNick;
		} else if (guildSettings != null && isPrefix.test(guildSettings.getPrefix())) {
			return guildSettings.getPrefix();
		} else if (isPrefix.test(bot.getDefaultPrefix())) {
			return bot.getDefaultPrefix();
		} else {
			return null;
		}
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
	public Map<Plugin, Map<String, String>> getGuildSettings() {
		if (!event.getGuildId().isPresent()) {
			throw new UnsupportedOperationException("Cannot perform this operation outside of a guild");
		}
		var map = new HashMap<Plugin, Map<String, String>>();
		bot.getGuildSettingsEntries().forEach((k, v) -> {
			var entries = new HashMap<String, String>();
			v.forEach((k0, v0) -> {
				entries.put(k0, v0.valueFromDatabaseAsString(bot.getDatabase(), event.getGuildId().get().asLong()));
			});
			map.put(k, entries);
		});
		return map;
	}

	@Override
	public void setGuildSetting(String key, String val) {
		if (!event.getGuildId().isPresent()) {
			throw new UnsupportedOperationException("Cannot perform this operation outside of a guild");
		}
		var found = false;
		for (var map : bot.getGuildSettingsEntries().values()) {
			var entry = map.get(key);
			if (entry != null) {
				found = true;
				entry.valueAsStringToDatabase(bot.getDatabase(), val, event.getGuildId().get().asLong());
				return;
			}
		}
		if (!found) {
			throw new NoSuchElementException();
		}
	}
}
