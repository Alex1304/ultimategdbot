package com.github.alex1304.ultimategdbot.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandErrorHandler;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.GuildSettingsValueConverter;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.object.util.Snowflake;

public class NativePlugin implements Plugin {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NativePlugin.class);
	
	private String aboutText;
	private final Set<Snowflake> unavailableGuildIds = Collections.synchronizedSet(new HashSet<>());
	private final AtomicInteger shardsNotReady = new AtomicInteger();
	private final Map<String, GuildSettingsEntry<?, ?>> configEntries = new HashMap<String, GuildSettingsEntry<?, ?>>();
	private final CommandErrorHandler cmdErrorHandler = new CommandErrorHandler();
	private final Set<Command> providedCommands = Set.of(new HelpCommand(this), new PingCommand(this), new SetupCommand(this),
			new SystemCommand(this), new AboutCommand(this), new BotAdminsCommand(this), new TimeCommand(this),
			new DelayCommand(this), new SequenceCommand(this));

	@Override
	public void setup(Bot bot, PropertyParser parser) {
		try {
			this.aboutText = Files.readAllLines(Paths.get(".", "config", "about.txt")).stream().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		initEventListeners(bot);
		// Guild settings
		var valueConverter = new GuildSettingsValueConverter(bot);
		configEntries.put("prefix", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getPrefix,
				NativeGuildSettings::setPrefix,
				(value, guildId) -> valueConverter.justCheck(value, guildId, x -> !x.isBlank(), "Cannot be blank"),
				valueConverter::noConversion
		));
		configEntries.put("server_mod_role", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getServerModRoleId,
				NativeGuildSettings::setServerModRoleId,
				valueConverter::toRoleId,
				valueConverter::fromRoleId
		));
	}
	
	private void initEventListeners(Bot bot) {
		// Initial Ready
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class).next()
					.map(readyEvent -> readyEvent.getGuilds().size())
					.flatMap(guildCount -> client.getEventDispatcher().on(GuildCreateEvent.class)
							.take(guildCount)
							.then(bot.log("Shard " + client.getConfig().getShardIndex() + " connected! Serving " + guildCount + " guilds."))))
			.then(bot.log("Bot ready!"))
			.subscribe(__ -> {
				// Guild join
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(GuildCreateEvent.class))
						.filter(event -> shardsNotReady.get() == 0)
						.filter(event -> !unavailableGuildIds.remove(event.getGuild().getId()))
						.map(GuildCreateEvent::getGuild)
						.flatMap(guild -> bot.log(":inbox_tray: New guild joined: " + BotUtils.escapeMarkdown(guild.getName())
								+ " (" + guild.getId().asString() + ")"))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing GuildCreateEvent on " + obj, error))
						.subscribe();
				// Guild leave
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(GuildDeleteEvent.class))
						.filter(event -> shardsNotReady.get() == 0)
						.filter(event -> {
							if (event.isUnavailable()) {
								unavailableGuildIds.add(event.getGuildId());
								return false;
							}
							unavailableGuildIds.remove(event.getGuildId());
							return true;
						})
						.map(event -> event.getGuild().map(guild -> BotUtils.escapeMarkdown(guild.getName())
								+ " (" + guild.getId().asString() + ")").orElse(event.getGuildId().asString() + " (no data)"))
						.flatMap(str -> bot.log(":outbox_tray: Guild left: " + str))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing GuildDeleteEvent on " + obj, error))
						.subscribe();
				// Resume on partial reconnections
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ResumeEvent.class)
						.flatMap(resumeEvent -> bot.log("Shard " + client.getConfig().getShardIndex()
								+ " successfully resumed session after websocket closure.")))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing ResumeEvent on " + obj, error))
						.subscribe();
				// Ready on full reconnections
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class)
						.doOnNext(readyEvent -> shardsNotReady.incrementAndGet())
						.map(readyEvent -> readyEvent.getGuilds().size())
						.flatMap(guildCount -> client.getEventDispatcher().on(GuildCreateEvent.class)
								.take(guildCount)
								.doAfterTerminate(() -> shardsNotReady.decrementAndGet())
								.then(bot.log("Shard " + client.getConfig().getShardIndex() + " reconnected (" + guildCount + " guilds)"))))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing ReadyEvent on " + obj, error))
						.subscribe();
			});
	}

	@Override
	public Set<Command> getProvidedCommands() {
		return providedCommands;
	}

	@Override
	public String getName() {
		return "Core";
	}

	@Override
	public Set<String> getDatabaseMappingResources() {
		return Set.of("/NativeGuildSettings.hbm.xml", "/BotAdmins.hbm.xml");
	}

	@Override
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries() {
		return configEntries;
	}

	@Override
	public CommandErrorHandler getCommandErrorHandler() {
		return cmdErrorHandler;
	}

	public String getAboutText() {
		return aboutText;
	}
}
