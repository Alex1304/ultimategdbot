package com.github.alex1304.ultimategdbot.api;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
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

/**
 * Represents the bot itself.
 */
public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot");
	
	private final String token;
	private final String defaultPrefix;
	private final Flux<DiscordClient> discordClients;
	private final DiscordClient mainDiscordClient;
	private final Database database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake attachmentsChannelId;
	private final Flux<GuildEmoji> emojis;
	private final Properties pluginsProps;
	private final CommandKernel cmdKernel;
	private final Set<Plugin> plugins = new HashSet<>();
	private final Mono<ApplicationInfo> appInfo;
	private final boolean blockhoundMode;

	private Bot(String token, String defaultPrefix, Flux<DiscordClient> discordClients, Database database,
			int replyMenuTimeout, Snowflake debugLogChannelId, Snowflake attachmentsChannelId,
			List<Snowflake> emojiGuildIds, boolean blockhoundMode, Properties pluginsProps) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.discordClients = discordClients;
		this.mainDiscordClient = discordClients.blockFirst();
		this.database = database;
		this.replyMenuTimeout = replyMenuTimeout;
		this.debugLogChannelId = debugLogChannelId;
		this.attachmentsChannelId = attachmentsChannelId;
		this.emojis = mainDiscordClient.getGuilds()
				.filter(g -> emojiGuildIds.stream().anyMatch(g.getId()::equals))
				.flatMap(Guild::getEmojis)
				.cache();
		this.pluginsProps = pluginsProps;
		this.cmdKernel = new CommandKernel(this);
		this.appInfo = mainDiscordClient.getApplicationInfo()
				.cache(Duration.ofMinutes(30));
		this.blockhoundMode = blockhoundMode;
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
	 * Gets the maximum time in seconds that the bot should wait for a reply when a
	 * reply menu is open.
	 * 
	 * @return the value as int (in seconds)
	 */
	public int getReplyMenuTimeout() {
		return replyMenuTimeout;
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

	public static Bot buildFromProperties(Properties props, Properties pluginsProps) {
		var propParser = new PropertyParser(props);
		var token = propParser.parseAsString("token");
		var defaultPrefix = propParser.parseAsString("default_prefix");
		var database = new Database();
		var replyMenuTimeout = propParser.parseAsInt("reply_menu_timeout");
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
		var requestThroughput = propParser.parseAsIntOrDefault("request_throughput", 48);
		var messageCacheMaxSize = propParser.parseAsIntOrDefault("message_cache_max_size", 50_000);
		var messageCacheTtl = Duration.ofMinutes(propParser.parseAsLongOrDefault("message_cache_ttl", 120));
		var disableVoiceStateCache = propParser.parseOrDefault("disable_voice_state_cache", Boolean::parseBoolean, false);
		var blockhoundMode = propParser.parseOrDefault("blockhound_mode", Boolean::parseBoolean, false);
		var useImmediateScheduler = propParser.parseOrDefault("use_immediate_scheduler", Boolean::parseBoolean, false);
		
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
						.globalRateLimiter(new ClockRateLimiter(requestThroughput, Duration.ofSeconds(1)))
						.build())
				.build()
				.map(dcb -> dcb.setInitialPresence(presenceStatus)
						.setEventScheduler(useImmediateScheduler ? Schedulers.immediate() : null))
				.map(DiscordClientBuilder::build)
				.cache();

		return new Bot(token, defaultPrefix, discordClients, database, replyMenuTimeout, debugLogChannelId,
				attachmentsChannelId, emojiGuildIds, blockhoundMode, pluginsProps);
	}

	public Mono<Void> start() {
		if (blockhoundMode) {
			BlockHound.install();
			LOGGER.info("Initialized BlockHound");
		}
		var loader = ServiceLoader.load(Plugin.class);
		var parser = new PropertyParser(pluginsProps);
		return Flux.fromIterable(loader)
				.flatMap(plugin -> plugin.setup(this, parser).thenReturn(plugin)
						.doOnError(e -> LOGGER.error("Failed to load plugin " + plugin.getName(), e)))
				.doOnNext(plugin -> database.addAllMappingResources(plugin.getDatabaseMappingResources()))
				.doOnNext(plugin -> LOGGER.info("Loaded plugin: {}", plugin.getName()))
				.doOnNext(plugins::add)
				.doOnNext(plugin -> cmdKernel.addProvider(plugin.getCommandProvider()))
				.doOnNext(plugin -> LOGGER.debug("Plugin {} is providing commands: {}", plugin.getName(), plugin.getCommandProvider()))
				.then(Mono.fromRunnable(database::configure))
				.then(Mono.fromRunnable(cmdKernel::start)
						.and(discordClients.flatMap(DiscordClient::login)));
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
}
