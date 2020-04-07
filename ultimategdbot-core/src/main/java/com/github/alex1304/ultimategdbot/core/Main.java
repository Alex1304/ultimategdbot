package com.github.alex1304.ultimategdbot.core;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.api.BotConfig;
import com.github.alex1304.ultimategdbot.api.SimpleBot;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;
import com.github.alex1304.ultimategdbot.api.util.menu.PaginationControls;

import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

class Main {
	
	private static final Logger LOGGER = Loggers.getLogger(Main.class);
	public static final Path PROPS_FILE = Paths.get(".", "config", "bot.properties");

	public static void main(String[] args) {
		try {
			var props = new Properties();
			try (var propsInput = Files.newBufferedReader(PROPS_FILE)) {
				props.load(propsInput);
			}
			var bot = SimpleBot.create(fromProperties(props));
			bot.start();
		} catch (Throwable e) {
			LOGGER.error("The bot could not be started. Make sure that all configuration files are present and have a valid content", e);
			System.exit(1);
		}
	}

	private static BotConfig fromProperties(Properties properties) {
		var propertyReader = PropertyReader.fromProperties(properties);
		var activity = propertyReader.readOptional("activity")
				.map(value -> {
					if (value.isEmpty() || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("null")) {
						return null;
					} else if (value.matches("playing:.+")) {
						return Activity.playing(value.split(":")[1]);
					} else if (value.matches("watching:.+")) {
						return Activity.watching(value.split(":")[1]);
					} else if (value.matches("listening:.+")) {
						return Activity.listening(value.split(":")[1]);
					} else if (value.matches("streaming:[^:]+:[^:]+")) {
						var split = value.split(":");
						return Activity.streaming(split[1], split[2]);
					}
					LOGGER.warn("activity: Expected one of: ''|'none'|'null', 'playing:<text>', 'watching:<text>', 'listening:<text>'"
							+ " or 'streaming:<url>' in lower case. Defaulting to no activity");
					return null;
				})
				.orElse(null);
		return BotConfig.builder(propertyReader.read("token"))
				.setCommandPrefix(propertyReader.readOptional("command_prefix").orElse(null))
				.setFlagPrefix(propertyReader.readOptional("flag_prefix").orElse(null))
				.setDebugLogChannelId(propertyReader.readOptional("debug_log_channel_id").map(Snowflake::of).orElse(null))
				.setEmojiGuildIds(propertyReader.readAsStream("emoji_guild_ids", ",").map(Snowflake::of).collect(toUnmodifiableSet()))
				.setStatus(propertyReader.readOptional("status").map(value -> {
					switch (value) {
						case "online": return activity != null ? Presence.online(activity) : Presence.online();
						case "idle": return activity != null ? Presence.idle(activity) : Presence.idle();
						case "dnd": return activity != null ? Presence.doNotDisturb(activity) : Presence.doNotDisturb();
						case "invisible": return Presence.invisible();
						default:
							LOGGER.warn("status: Expected one of 'online', 'idle', 'dnd', 'invisible'. Defaulting to 'online'.");
							return activity != null ? Presence.online(activity) : Presence.online();
					}
				}).orElse(activity != null ? Presence.online(activity) : Presence.online()))
				.setPaginationControls(new PaginationControls(
						propertyReader.readOptional("interactive_menu.controls.previous").orElse(PaginationControls.DEFAULT_PREVIOUS_EMOJI),
						propertyReader.readOptional("interactive_menu.controls.next").orElse(PaginationControls.DEFAULT_NEXT_EMOJI),
						propertyReader.readOptional("interactive_menu.controls.close").orElse(PaginationControls.DEFAULT_CLOSE_EMOJI)))
				.setInteractiveMenuTimeout(propertyReader.readOptional("interactive_menu.timeout_seconds").map(Integer::parseInt).map(Duration::ofSeconds).orElse(null))
				.setMessageCacheMaxSize(propertyReader.readOptional("message_cache_max_size").map(Integer::parseInt).orElse(BotConfig.DEFAULT_MESSAGE_CACHE_MAX_SIZE))
				.setRestTimeout(propertyReader.readOptional("rest.timeout_seconds").map(Integer::parseInt).map(Duration::ofSeconds).orElse(null))
				.setRestBufferSize(propertyReader.readOptional("rest.buffer_size").map(Integer::parseInt).orElse(BotConfig.DEFAULT_REST_BUFFER_SIZE))
				.build();
	}
}
