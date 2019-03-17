package com.github.alex1304.ultimategdbot.core.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.guildsettings.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class ContextImpl implements Context {
	
	private final Command originalCommand;
	private final MessageCreateEvent event;
	private final List<String> args;
	private final Bot bot;
	private final Map<String, Object> variables;
	private final String prefixUsed;

	public ContextImpl(Command originalCommand, MessageCreateEvent event, List<String> args, Bot bot, String prefixUsed) {
		this.originalCommand = Objects.requireNonNull(originalCommand);
		this.event = Objects.requireNonNull(event);
		this.args = Objects.requireNonNull(args);
		this.bot = Objects.requireNonNull(bot);
		this.variables = new ConcurrentHashMap<>();
		this.prefixUsed = Objects.requireNonNull(prefixUsed);
	}

	@Override
	public Command getCommand() {
		return originalCommand;
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
			if (ce.getStatus().code() != 403) {
				return;
			}
			var author = event.getMessage().getAuthor();
			if (!author.isPresent()) {
				return;
			}
			author.get().getPrivateChannel()
					.flatMap(pc -> pc.createMessage("I was unable to send a reply to your command in <#"
							+ event.getMessage().getChannelId().asString()
							+ ">. Make sure that I have permissions to talk and send embeds there.\nError response: `"
							+ ce.getErrorResponse() + "`"))
					.doOnError(__ -> {})
					.subscribe();
		});
	}

	@Override
	public String getPrefixUsed() {
		return prefixUsed;
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
	public Mono<Map<Plugin, Map<String, String>>> getGuildSettings() {
		if (!event.getGuildId().isPresent()) {
			return Mono.error(new UnsupportedOperationException("Cannot perform this operation outside of a guild"));
		}
		var result = new TreeMap<Plugin, Map<String, String>>(Comparator.comparing(Plugin::getName));
		var entriesForEachPlugin = new TreeMap<String, String>();
		// Flux.fromIterable(..).flatMap(...)       // Is used as a reactive way to iterate a collection, flatMap being used as a forEach
		//         .takeLast(1).doOnNext(__ -> ...) // Allows to execute an action after the iteration is done.
		return Flux.fromIterable(bot.getGuildSettingsEntries().entrySet())
				.concatMap(guildSettingsEntriesByPlugin -> Flux.fromIterable(guildSettingsEntriesByPlugin.getValue().entrySet())
						.concatMap(entry -> entry.getValue().valueFromDatabaseAsString(bot.getDatabase(), event.getGuildId().get().asLong())
								.doOnNext(strVal -> entriesForEachPlugin.put(entry.getKey(), strVal)))
						.takeLast(1)
						.doOnNext(__ -> {
							result.put(guildSettingsEntriesByPlugin.getKey(), new HashMap<>(entriesForEachPlugin));
							entriesForEachPlugin.clear(); // We are done with this plugin, clear the map so that it can be used for next plugins
						}))
				.then(Mono.just(Collections.unmodifiableMap(result)));
	}

	@Override
	public Mono<Void> setGuildSetting(String key, String val) {
		if (!event.getGuildId().isPresent()) {
			throw new UnsupportedOperationException("Cannot perform this operation outside of a guild");
		}
		for (var map : bot.getGuildSettingsEntries().values()) {
			var entry = map.get(key);
			if (entry != null) {
				return entry.valueAsStringToDatabase(bot.getDatabase(), val, event.getGuildId().get().asLong());
			}
		}
		return Mono.error(new NoSuchElementException());
	}
	
	public static Mono<String> findPrefixUsed(Bot bot, MessageCreateEvent event) {
		var guildIdOpt = event.getGuildId();
		var guildSettings = guildIdOpt.isPresent() ? bot.getDatabase().findByIDOrCreate(NativeGuildSettings.class, guildIdOpt.get().asLong(), (gs, gid) -> {
			gs.setGuildId(gid);
			gs.setPrefix(bot.getDefaultPrefix());
		}) : Mono.<NativeGuildSettings>empty();
		return bot.getDiscordClients().next()
				.map(DiscordClient::getSelfId)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(botId -> Tuples.of("<@" + botId.asString() + "> ", "<@!" + botId.asString() + "> ",
						BotUtils.removeQuotesUnlessEscaped(event.getMessage().getContent().orElse(""))))
				.filter(tuple -> !tuple.getT3().isEmpty())
				.flatMap(tuple -> guildSettings
						.map(gs -> Tuples.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), gs.getPrefix() == null ? bot.getDefaultPrefix() : gs.getPrefix()))
						.defaultIfEmpty(Tuples.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), bot.getDefaultPrefix())))
				.flatMap(tuple -> Flux.just(tuple.getT1(), tuple.getT2(), tuple.getT4())
							.filter(str -> str.equalsIgnoreCase(tuple.getT3().substring(0, Math.min(str.length(), tuple.getT3().length()))))
							.next());
	}
}
