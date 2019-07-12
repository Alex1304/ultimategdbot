package com.github.alex1304.ultimategdbot.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandKernel;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
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

class BotImpl implements Bot {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot");
	
	private final String token;
	private final String defaultPrefix;
	private final Flux<DiscordClient> discordClients;
	private final DiscordClient mainDiscordClient;
	private final DatabaseImpl database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake attachmentsChannelId;
	private final List<Snowflake> emojiGuildIds;
	private final Properties pluginsProps;
	private final CommandKernelImpl cmdKernel;
	private final Set<Plugin> plugins = new HashSet<>();
	private final Mono<ApplicationInfo> appInfo;
	private final boolean blockhoundMode;

	private BotImpl(String token, String defaultPrefix, Flux<DiscordClient> discordClients, DatabaseImpl database,
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
		this.emojiGuildIds = emojiGuildIds;
		this.pluginsProps = pluginsProps;
		this.cmdKernel = new CommandKernelImpl(this);
		this.appInfo = mainDiscordClient.getApplicationInfo()
				.cache(Duration.ofMinutes(30));
		this.blockhoundMode = blockhoundMode;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public String getDefaultPrefix() {
		return defaultPrefix;
	}
	
	@Override
	public DiscordClient getMainDiscordClient() {
		return mainDiscordClient;
	}

	@Override
	public Flux<DiscordClient> getDiscordClients() {
		return discordClients;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public int getReplyMenuTimeout() {
		return replyMenuTimeout;
	}

	@Override
	public Mono<Channel> getDebugLogChannel() {
		return mainDiscordClient.getChannelById(debugLogChannelId);
	}

	@Override
	public Mono<Channel> getAttachmentsChannel() {
		return mainDiscordClient.getChannelById(attachmentsChannelId);
	}

	@Override
	public Mono<Message> log(String message) {
		return log(mcs -> mcs.setContent(message));
	}

	@Override
	public Mono<Message> log(Consumer<MessageCreateSpec> spec) {
		return mainDiscordClient.getChannelById(debugLogChannelId)
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(spec));
	}

	@Override
	@Deprecated
	public Mono<Message> logStackTrace(Context ctx, Throwable t) {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw);
		pw.println(":no_entry_sign: **Something went wrong while executing a command.**");
		pw.println("__Context dump:__ `" + ctx + "`");
		pw.println("__Stack trace preview (see full trace in internal logs):__");
		t.printStackTrace(pw);
		var trace = sw.toString()
				.lines()
				.limit(10)
				.collect(Collectors.joining("\n"));
		return getDebugLogChannel()
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(trace));
	}

	@Override
	public Mono<String> getEmoji(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		return mainDiscordClient.getGuilds()
				.filter(g -> emojiGuildIds.stream().anyMatch(g.getId()::equals))
				.flatMap(Guild::getEmojis)
				.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal);
	}

	public static BotImpl buildFromProperties(Properties props, Properties pluginsProps) {
		var propParser = new PropertyParser(Main.PROPS_FILE.toString(), props);
		var token = propParser.parseAsString("token");
		var defaultPrefix = propParser.parseAsString("default_prefix");
		var database = new DatabaseImpl();
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
						.globalRateLimiter(new ClockRateLimiter(requestThroughput))
						.build())
				.build()
				.map(dcb -> dcb.setInitialPresence(presenceStatus)
						.setEventScheduler(useImmediateScheduler ? Schedulers.immediate() : null))
				.map(DiscordClientBuilder::build)
				.cache();

		return new BotImpl(token, defaultPrefix, discordClients, database, replyMenuTimeout, debugLogChannelId,
				attachmentsChannelId, emojiGuildIds, blockhoundMode, pluginsProps);
	}

	public void start() {
		if (blockhoundMode) {
			BlockHound.install();
			LOGGER.info("Initialized BlockHound");
		}
		var loader = ServiceLoader.load(Plugin.class);
		var parser = new PropertyParser(Main.PLUGINS_PROPS_FILE.toString(), pluginsProps);
		var commandsByAliases = new HashMap<String, Command>();
		var subCommands = new HashMap<Command, Map<String, Command>>();
		for (var plugin : loader) {
			try {
				LOGGER.info("Loading plugin: {}...", plugin.getName());
				plugin.setup(this, parser);
				database.addAllMappingResources(plugin.getDatabaseMappingResources());
				var cmdSet = new TreeSet<Command>(Comparator.comparing(cmd -> BotUtils.joinAliases(cmd.getAliases())));
				cmdSet.addAll(plugin.getProvidedCommands());
				for (var cmd : cmdSet) {
					for (var alias : cmd.getAliases()) {
						commandsByAliases.put(alias.toLowerCase(), cmd);
					}
					// Add all subcommands
					var subCmdDeque = new ArrayDeque<Command>();
					subCmdDeque.push(cmd);
					while (!subCmdDeque.isEmpty()) {
						var element = subCmdDeque.pop();
						var elementSubcmds = element.getSubcommands();
						if (subCommands.containsKey(element)) {
							continue;
						}
						var subCmdMap = new HashMap<String, Command>();
						for (var subcmd : elementSubcmds) {
							for (var alias : subcmd.getAliases()) {
								subCmdMap.put(alias.toLowerCase(), subcmd);
							}
						}
						subCommands.put(element, subCmdMap);
						subCmdDeque.addAll(elementSubcmds);
					}
					LOGGER.info("Loaded command: {} {}", cmd.getClass().getName(), cmd.getAliases());
				}
				plugins.add(plugin);
			} catch (RuntimeException e) {
				LOGGER.error("Failed to load plugin " + plugin.getName(), e);
			}
		}
		try {
			database.configure();
		} catch (MappingException e) {
			LOGGER.error("Oops! There is an error in the database mapping configuration!");
			throw e;
		}
		cmdKernel.addCommands(commandsByAliases);
		cmdKernel.addSubcommands(subCommands);
		cmdKernel.start();
		discordClients.flatMap(DiscordClient::login).blockLast();
	}
	
	@Override
	public CommandKernel getCommandKernel() {
		return cmdKernel;
	}

	@Override
	public Set<Plugin> getPlugins() {
		return Collections.unmodifiableSet(plugins);
	}

	@Override
	public Mono<ApplicationInfo> getApplicationInfo() {
		return appInfo;
	}
}
