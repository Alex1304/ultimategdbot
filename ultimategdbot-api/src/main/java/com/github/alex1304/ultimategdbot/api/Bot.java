package com.github.alex1304.ultimategdbot.api;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.database.BlacklistedIds;
import com.github.alex1304.ultimategdbot.api.utils.Markdown;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;
import com.github.alex1304.ultimategdbot.api.utils.menu.PaginationControls;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.object.data.stored.MessageBean;
import discord4j.core.object.data.stored.VoiceStateBean;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import discord4j.core.shard.ShardingClientBuilder;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.request.SemaphoreGlobalRateLimiter;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.api.noop.NoOpStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.retry.Retry;

/**
 * Represents the bot itself.
 */
public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot");
	
	private final String token;
	private final String defaultPrefix;
	private final String flagPrefix;
	private final Flux<DiscordClient> discordClients;
	private final DiscordClient mainDiscordClient;
	private final Database database;
	private final int interactiveMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake attachmentsChannelId;
	private final List<Snowflake> emojiGuildIds;
	private final Properties pluginsProps;
	private final CommandKernel cmdKernel;
	private final Set<Plugin> plugins = new HashSet<>();
	private final Set<Snowflake> unavailableGuildIds = Collections.synchronizedSet(new HashSet<>());
	private final AtomicInteger shardsNotReady = new AtomicInteger();
	private final Mono<ApplicationInfo> appInfo;
	private final boolean blockhoundMode;
	private final PaginationControls controls;
	private final boolean corePluginDisabled;
	private Flux<GuildEmoji> emojis;

	private Bot(String token, String defaultPrefix, String flagPrefix, Flux<DiscordClient> discordClients,
			Database database, int interactiveMenuTimeout, Snowflake debugLogChannelId, Snowflake attachmentsChannelId,
			List<Snowflake> emojiGuildIds, boolean blockhoundMode, Properties pluginsProps, PaginationControls controls, boolean corePluginDisabled) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.flagPrefix = flagPrefix;
		this.discordClients = discordClients;
		this.mainDiscordClient = discordClients.blockFirst();
		this.database = database;
		this.interactiveMenuTimeout = interactiveMenuTimeout;
		this.debugLogChannelId = debugLogChannelId;
		this.attachmentsChannelId = attachmentsChannelId;
		this.emojiGuildIds = emojiGuildIds;
		this.pluginsProps = pluginsProps;
		this.cmdKernel = new CommandKernel(this);
		this.appInfo = mainDiscordClient.getApplicationInfo()
				.cache(Duration.ofMinutes(30));
		this.blockhoundMode = blockhoundMode;
		this.controls = controls;
		this.corePluginDisabled = corePluginDisabled;
		installEmojis();
	}

	/**
	 * Get the bot token.
	 * 
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Gets the default prefix.
	 * 
	 * @return the default prefix
	 */
	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	/**
	 * Gets the flag prefix.
	 * 
	 * @return the flag prefix
	 */
	public String getFlagPrefix() {
		return flagPrefix;
	}

	/**
	 * Gets the discord client representing shard 0.
	 * 
	 * @return the discord client
	 */
	public DiscordClient getMainDiscordClient() {
		return mainDiscordClient;
	}

	/**
	 * Gets the Flux containing the discord client for each shard.
	 * 
	 * @return a Flux of discord client
	 */
	public Flux<DiscordClient> getDiscordClients() {
		return discordClients;
	}

	/**
	 * Gets the database of the bot.
	 * 
	 * @return the database
	 */
	public Database getDatabase() {
		return database;
	}

	/**
	 * Gets the maximum time in seconds that the bot should wait for a user
	 * interaction when an interactive menu is open.
	 * 
	 * @return the value as int (in seconds)
	 */
	public int getInteractiveMenuTimeout() {
		return interactiveMenuTimeout;
	}
	
	/**
	 * Gets the default emojis used for pagination controls configured for the bot.
	 * 
	 * @return a {@link PaginationControls} instance
	 */
	public PaginationControls getDefaultPaginationControls() {
		return controls;
	}

	/**
	 * Gets the channel where the bot sends messages for debugging purposes.
	 * 
	 * @return a Mono emitting the debug log channel
	 */
	public Mono<Channel> getDebugLogChannel() {
		return mainDiscordClient.getChannelById(debugLogChannelId);
	}

	/**
	 * Gets the channel where the bot can send attachments for its embeds.
	 * 
	 * @return a Mono emitting the attachments channel
	 */
	public Mono<Channel> getAttachmentsChannel() {
		return mainDiscordClient.getChannelById(attachmentsChannelId);
	}

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param message the message to send
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> log(String message) {
		return log(mcs -> mcs.setContent(message));
	}

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param spec the spec of the message to send
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> log(Consumer<MessageCreateSpec> spec) {
		return mainDiscordClient.getChannelById(debugLogChannelId)
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(spec));
	}
	
	/**
	 * Gathers all emojis from the configured emoji guilds and put them in cache.
	 * Subsequent calls of this method will remove the old cache and perform the
	 * installation again.
	 */
	public void installEmojis() {
		this.emojis = mainDiscordClient.getGuilds()
				.filter(g -> emojiGuildIds.stream().anyMatch(g.getId()::equals))
				.flatMap(Guild::getEmojis)
				.cache();
	}

	/**
	 * Gets all emojis installed. An emoji qualifies as "installed" if it is present
	 * in one of the emoji guilds configured in {@code bot.properties}.
	 * 
	 * @return a Flux emitting the installed emojis.
	 */
	public Flux<GuildEmoji> getInstalledEmojis() {
		return emojis;
	}

	/**
	 * Gets the String representation of an emoji installed on one of the emoji
	 * servers. If the emoji is not found, the returned value is the given name
	 * wrapped in colons.
	 * 
	 * @param emojiName the name of the emoji to look for
	 * @return a Mono emitting the emoji code corresponding to the given name
	 */
	public Mono<String> getEmoji(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		return emojis.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal);
	}

	/**
	 * Gets the command kernel of this bot.
	 * 
	 * @return the command kernel
	 */
	public CommandKernel getCommandKernel() {
		return cmdKernel;
	}

	/**
	 * Gets a Set containing all successfully loaded plugins.
	 * 
	 * @return a Set of Plugin
	 */
	public Set<Plugin> getPlugins() {
		return Collections.unmodifiableSet(plugins);
	}

	/**
	 * Get the application info of the bot
	 * 
	 * @return a Mono emitting the application info
	 */
	public Mono<ApplicationInfo> getApplicationInfo() {
		return appInfo;
	}
	
	/**
	 * Gets whether the core plugin is disabled.
	 * 
	 * @return a boolean
	 */
	public boolean isCorePluginDisabled() {
		return corePluginDisabled;
	}

	public static Bot buildFromProperties(Properties props, Properties pluginsProps) {
		var propParser = new PropertyParser(props);
		var token = propParser.parseAsString("token");
		var defaultPrefix = propParser.parseAsString("default_prefix");
		var flagPrefix = propParser.parseAsStringOrDefault("flag_prefix", "-");
		var database = new Database();
		var interactiveMenuTimeout = propParser.parseAsIntOrDefault("interactive_menu.timeout", 600);
		var controls = new PaginationControls(
				propParser.parseAsStringOrDefault("interactive_menu.previous_emoji", "◀"),
				propParser.parseAsStringOrDefault("interactive_menu.next_emoji", "▶"),
				propParser.parseAsStringOrDefault("interactive_menu.close_emoji", "🚫"));
		var debugLogChannelId = propParser.parse("debug_log_channel_id", Snowflake::of);
		var attachmentsChannelId = propParser.parse("attachments_channel_id", Snowflake::of);
		var emojiGuildIds = propParser.parseAsList("emoji_guild_ids", ",", Snowflake::of);
		var activity = propParser.parseOrDefault("presence_activity", value -> {
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
			LOGGER.error("presence_activity: Expected one of: ''|'none'|'null', 'playing:<text>', 'watching:<text>', 'listening:<text>'"
					+ " or 'streaming:<url>' in lower case. Defaulting to no activity");
			return null;
		}, null);
		var presenceStatus = propParser.parseOrDefault("presence_status", value -> {
			switch (value) {
				case "online": return Presence.online(activity);
				case "idle": return Presence.idle(activity);
				case "dnd": return Presence.doNotDisturb(activity);
				case "invisible": return Presence.invisible();
				default:
					LOGGER.warn("presence_status: Expected one of 'online', 'idle', 'dnd', 'invisible'. Defaulting to 'online'.");
					return Presence.online(activity);
			}
		}, Presence.online(activity));
		var requestParallelism = propParser.parseAsIntOrDefault("request_parallelism", 12);
		var messageCacheMaxSize = propParser.parseAsIntOrDefault("message_cache_max_size", 50_000);
		var messageCacheTtl = Duration.ofMinutes(propParser.parseAsLongOrDefault("message_cache_ttl", 120));
		var disableVoiceStateCache = propParser.parseOrDefault("disable_voice_state_cache", Boolean::parseBoolean, false);
		var blockhoundMode = propParser.parseOrDefault("blockhound_mode", Boolean::parseBoolean, false);
		var useImmediateScheduler = propParser.parseOrDefault("use_immediate_scheduler", Boolean::parseBoolean, false);
		var corePluginDisabled = propParser.parseOrDefault("disable_core_plugin", Boolean::parseBoolean, false);
		
		if (useImmediateScheduler) {
			LOGGER.info("Using immediate scheduler for Discord events. While it may improve performances, {} {}",
					"it may also cause errors if you use plugins that perform blocking calls. In that case,",
					"it is recommended to switch `use_immediate_scheduler` to false in bot.properties");
		}
		
		var discordClients = new ShardingClientBuilder(token)
				.setStoreService(MappingStoreService.create()
						.setMapping(new CaffeineStoreService(builder -> builder
								.maximumSize(messageCacheMaxSize)
								.expireAfterAccess(messageCacheTtl)), MessageBean.class)
						.setMapping(disableVoiceStateCache ? new NoOpStoreService() : new JdkStoreService(), VoiceStateBean.class)
						.setFallback(new JdkStoreService()))
				.setRouterOptions(RouterOptions.builder()
						.onClientResponse(ResponseFunction.emptyIfNotFound())
						.onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
						.globalRateLimiter(new SemaphoreGlobalRateLimiter(requestParallelism))
						.build())
				.build()
				.map(dcb -> dcb.setInitialPresence(presenceStatus)
						.setEventScheduler(useImmediateScheduler ? Schedulers.immediate() : null))
				.map(DiscordClientBuilder::build)
				.cache();

		return new Bot(token, defaultPrefix, flagPrefix, discordClients, database, interactiveMenuTimeout, debugLogChannelId,
				attachmentsChannelId, emojiGuildIds, blockhoundMode, pluginsProps, controls, corePluginDisabled);
	}

	public Mono<Void> start() {
		if (blockhoundMode) {
			BlockHound.install();
			LOGGER.info("Initialized BlockHound");
		}
		var loader = ServiceLoader.load(Plugin.class);
		var parser = new PropertyParser(pluginsProps);
		initEventListeners();
		database.addAllMappingResources(Set.of("/NativeGuildSettings.hbm.xml", "/BotAdmins.hbm.xml", "/BlacklistedIds.hbm.xml"));
		return Flux.fromIterable(loader)
				.flatMap(plugin -> plugin.setup(this, parser).thenReturn(plugin)
						.doOnError(e -> LOGGER.error("Failed to load plugin " + plugin.getName(), e)))
				.doOnNext(plugin -> database.addAllMappingResources(plugin.getDatabaseMappingResources()))
				.doOnNext(plugin -> LOGGER.info("Loaded plugin: {}", plugin.getName()))
				.doOnNext(plugins::add)
				.doOnNext(plugin -> cmdKernel.addProvider(plugin.getCommandProvider()))
				.doOnNext(plugin -> LOGGER.debug("Plugin {} is providing commands: {}", plugin.getName(), plugin.getCommandProvider()))
				.then(Mono.fromRunnable(database::configure))
				.then(database.query(BlacklistedIds.class, "from BlacklistedIds")
						.map(BlacklistedIds::getId)
						.doOnNext(cmdKernel::blacklist)
						.then())
				.then(Mono.fromRunnable(cmdKernel::start)
						.and(discordClients.flatMap(DiscordClient::login)));
	}
	
	@SuppressWarnings("deprecation")
	private void initEventListeners() {
		discordClients.flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class).next()
					.doOnNext(readyEvent -> readyEvent.getGuilds().stream()
							.map(ReadyEvent.Guild::getId)
							.forEach(unavailableGuildIds::add))
					.map(ReadyEvent::getGuilds)
					.flatMap(guilds -> client.getEventDispatcher().on(GuildCreateEvent.class)
							.doOnNext(guildCreateEvent -> unavailableGuildIds.remove(guildCreateEvent.getGuild().getId()))
							.take(guilds.size())
							.timeout(Duration.ofMinutes(2), Mono.empty())
							.then(Mono.defer(() -> log("Shard " + client.getConfig().getShardIndex() + " connected! Serving " + guilds.stream()
									.map(ReadyEvent.Guild::getId)
									.filter(id -> !unavailableGuildIds.contains(id))
									.count() + " guilds.")))))
			.then(Flux.fromIterable(plugins)
					.flatMap(plugin -> plugin.onBotReady(this)
							.onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.warn("onBotReady action failed for plugin " + plugin.getName(), e))))
					.then())
			.then(log("Bot ready!"))
			.doOnTerminate(() -> {
				// Guild join
				discordClients.flatMap(client -> client.getEventDispatcher().on(GuildCreateEvent.class))
						.filter(event -> shardsNotReady.get() == 0)
						.filter(event -> !unavailableGuildIds.remove(event.getGuild().getId()))
						.map(GuildCreateEvent::getGuild)
						.flatMap(guild -> log(":inbox_tray: New guild joined: " + Markdown.escape(guild.getName())
								+ " (" + guild.getId().asString() + ")"))
						.retryWhen(Retry.any().doOnRetry(retryCtx -> LOGGER.error("Error while procesing GuildCreateEvent", retryCtx.exception())))
						.subscribe();
				// Guild leave
				discordClients.flatMap(client -> client.getEventDispatcher().on(GuildDeleteEvent.class))
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
						.flatMap(str -> log(":outbox_tray: Guild left: " + str))
						.retryWhen(Retry.any().doOnRetry(retryCtx -> LOGGER.error("Error while procesing GuildDeleteEvent", retryCtx.exception())))
						.subscribe();
				// Resume on partial reconnections
				discordClients.flatMap(client -> client.getEventDispatcher().on(ResumeEvent.class)
								.flatMap(resumeEvent -> log("Shard " + client.getConfig().getShardIndex()
										+ ": session resumed after websocket disconnection.")))
						.retryWhen(Retry.any().doOnRetry(retryCtx -> LOGGER.error("Error while procesing ResumeEvent", retryCtx.exception())))
						.subscribe();
				// Ready on full reconnections
				discordClients.flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class)
								.doOnNext(readyEvent -> shardsNotReady.incrementAndGet())
								.map(readyEvent -> readyEvent.getGuilds().size())
								.flatMap(guildCount -> client.getEventDispatcher().on(GuildCreateEvent.class)
										.take(guildCount)
										.timeout(Duration.ofMinutes(2), Mono.error(new TimeoutException("Unable to load guilds of shard "
												+ client.getConfig().getShardIndex() + " in time")))
										.doAfterTerminate(() -> shardsNotReady.decrementAndGet())
										.then(log("Shard " + client.getConfig().getShardIndex() + " reconnected (" + guildCount + " guilds)"))))
						.retryWhen(Retry.any().doOnRetry(retryCtx -> LOGGER.error("Error while procesing ReadyEvent", retryCtx.exception())))
						.subscribe();
			})
			.subscribe();
	}
}
