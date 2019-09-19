package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
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
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.annotated.AnnotatedCommandProvider;
import com.github.alex1304.ultimategdbot.api.database.BlacklistedIds;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.DatabaseInputFunction;
import com.github.alex1304.ultimategdbot.api.utils.DatabaseOutputFunction;
import com.github.alex1304.ultimategdbot.api.utils.Markdown;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent.Guild;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class NativePlugin implements Plugin {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NativePlugin.class);
	
	private volatile String aboutText;
	private final AnnotatedCommandProvider cmdProvider = new AnnotatedCommandProvider();
	private final Set<Snowflake> unavailableGuildIds = Collections.synchronizedSet(new HashSet<>());
	private final AtomicInteger shardsNotReady = new AtomicInteger();
	private final Map<String, GuildSettingsEntry<?, ?>> configEntries = new HashMap<String, GuildSettingsEntry<?, ?>>();

	@Override
	public Mono<Void> setup(Bot bot, PropertyParser parser) {
		return Mono.fromCallable(() -> Files.readAllLines(Paths.get(".", "config", "about.txt")).stream().collect(Collectors.joining("\n")))
				.doOnNext(aboutText -> this.aboutText = aboutText)
				.and(Mono.fromRunnable(() -> {
					cmdProvider.addAnnotated(new HelpCommand());
					cmdProvider.addAnnotated(new PingCommand());
					cmdProvider.addAnnotated(new SetupCommand());
					cmdProvider.addAnnotated(new SystemCommand());
					cmdProvider.addAnnotated(new AboutCommand(this));
					cmdProvider.addAnnotated(new BotAdminsCommand());
					cmdProvider.addAnnotated(new BlacklistCommand());
					cmdProvider.addAnnotated(new CacheInfoCommand());
					cmdProvider.addAnnotated(new BurstMessagesCommand());
					cmdProvider.addAnnotated(new TestMenuCommand());
					initEventListeners(bot);
					configEntries.put("prefix", new GuildSettingsEntry<>(
							NativeGuildSettings.class,
							NativeGuildSettings::getPrefix,
							NativeGuildSettings::setPrefix,
							DatabaseInputFunction.asIs().withInputCheck(x -> !x.isBlank(), "Cannot be blank"),
							DatabaseOutputFunction.stringValue()
					));
					configEntries.put("server_mod_role", new GuildSettingsEntry<>(
							NativeGuildSettings.class,
							NativeGuildSettings::getServerModRoleId,
							NativeGuildSettings::setServerModRoleId,
							DatabaseInputFunction.toRoleId(bot),
							DatabaseOutputFunction.fromRoleId(bot)
					));
				}));
	}
	
	@Override
	public Mono<Void> onBotReady(Bot bot) {
		return Mono.fromRunnable(() -> initBlacklist(bot));
	}
	
	private void initEventListeners(Bot bot) {
		// Initial Ready
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class).next()
					.doOnNext(readyEvent -> readyEvent.getGuilds().stream()
							.map(Guild::getId)
							.forEach(unavailableGuildIds::add))
					.map(ReadyEvent::getGuilds)
					.flatMap(guilds -> client.getEventDispatcher().on(GuildCreateEvent.class)
							.doOnNext(guildCreateEvent -> unavailableGuildIds.remove(guildCreateEvent.getGuild().getId()))
							.take(guilds.size())
							.timeout(Duration.ofMinutes(2), Mono.empty())
							.then(Mono.defer(() -> bot.log("Shard " + client.getConfig().getShardIndex() + " connected! Serving " + guilds.stream()
									.map(Guild::getId)
									.filter(id -> !unavailableGuildIds.contains(id))
									.count() + " guilds.")))))
			.then(Flux.fromIterable(bot.getPlugins())
					.flatMap(plugin -> plugin.onBotReady(bot))
					.onErrorContinue((error, obj) -> LOGGER.warn("onBotReady action failed for plugin " + ((Plugin) obj).getName(), error))
					.then())
			.then(bot.log("Bot ready!"))
			.subscribe(__ -> {
				// Guild join
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(GuildCreateEvent.class))
						.filter(event -> shardsNotReady.get() == 0)
						.filter(event -> !unavailableGuildIds.remove(event.getGuild().getId()))
						.map(GuildCreateEvent::getGuild)
						.flatMap(guild -> bot.log(":inbox_tray: New guild joined: " + Markdown.escape(guild.getName())
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
						.map(event -> event.getGuild().map(guild -> Markdown.escape(guild.getName())
								+ " (" + guild.getId().asString() + ")").orElse(event.getGuildId().asString() + " (no data)"))
						.flatMap(str -> bot.log(":outbox_tray: Guild left: " + str))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing GuildDeleteEvent on " + obj, error))
						.subscribe();
				// Resume on partial reconnections
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ResumeEvent.class)
						.flatMap(resumeEvent -> bot.log("Shard " + client.getConfig().getShardIndex()
								+ ": session resumed after websocket disconnection.")))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing ResumeEvent on " + obj, error))
						.subscribe();
				// Ready on full reconnections
				bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class)
						.doOnNext(readyEvent -> shardsNotReady.incrementAndGet())
						.map(readyEvent -> readyEvent.getGuilds().size())
						.flatMap(guildCount -> client.getEventDispatcher().on(GuildCreateEvent.class)
								.take(guildCount)
								.timeout(Duration.ofMinutes(2), Mono.empty())
								.doAfterTerminate(() -> shardsNotReady.decrementAndGet())
								.then(bot.log("Shard " + client.getConfig().getShardIndex() + " reconnected (" + guildCount + " guilds)"))))
						.onErrorContinue((error, obj) -> LOGGER.error("Error while procesing ReadyEvent on " + obj, error))
						.subscribe();
			});
	}
	
	private void initBlacklist(Bot bot) {
		bot.getDatabase().query(BlacklistedIds.class, "from BlacklistedIds")
			.map(BlacklistedIds::getId)
			.doOnNext(bot.getCommandKernel()::blacklist)
			.subscribe();
	}

	@Override
	public String getName() {
		return "Core";
	}

	@Override
	public Set<String> getDatabaseMappingResources() {
		return Set.of("/NativeGuildSettings.hbm.xml", "/BotAdmins.hbm.xml", "/BlacklistedIds.hbm.xml");
	}

	@Override
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries() {
		return configEntries;
	}

	@Override
	public CommandProvider getCommandProvider() {
		return cmdProvider;
	}

	public String getAboutText() {
		return aboutText;
	}
}
