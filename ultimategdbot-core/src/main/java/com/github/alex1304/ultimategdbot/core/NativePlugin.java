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
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.GuildSettingsValueConverter;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.util.Snowflake;

public class NativePlugin implements Plugin {
	
	private Bot bot;
	private String aboutText;
	private final Set<Snowflake> unavailableGuildIds = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void setup(Bot bot, PropertyParser parser) {
		this.bot = bot;
		try {
			this.aboutText = Files.readAllLines(Paths.get(".", "config", "about.txt")).stream().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void onBotReady() {
		// Guild join
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(GuildCreateEvent.class))
				.filter(event -> {
					var id = event.getGuild().getId();
					if (unavailableGuildIds.contains(id)) {
						unavailableGuildIds.remove(id);
						return false;
					}
					return true;
				})
				.map(GuildCreateEvent::getGuild)
				.flatMap(guild -> bot.log(":inbox_tray: New guild joined: " + BotUtils.escapeMarkdown(guild.getName())
						+ " (" + guild.getId().asString() + ")"))
				.subscribe();
		// Guild leave
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(GuildDeleteEvent.class))
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
		.subscribe();
	}
	
	@Override
	public Set<Command> getProvidedCommands() {
		return Set.of(new HelpCommand(), new PingCommand(), new SetupCommand(), new SystemCommand(), new AboutCommand(aboutText),
				new BotAdminsCommand(), new TimeCommand(), new DelayCommand(), new SequenceCommand());
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
		var map = new HashMap<String, GuildSettingsEntry<?, ?>>();
		var valueConverter = new GuildSettingsValueConverter(bot);
		map.put("prefix", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getPrefix,
				NativeGuildSettings::setPrefix,
				(value, guildId) -> valueConverter.justCheck(value, guildId, x -> !x.isBlank(), "Cannot be blank"),
				valueConverter::noConversion
		));
		map.put("server_mod_role", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getServerModRoleId,
				NativeGuildSettings::setServerModRoleId,
				valueConverter::toRoleId,
				valueConverter::fromRoleId
		));
		return map;
	}
}
