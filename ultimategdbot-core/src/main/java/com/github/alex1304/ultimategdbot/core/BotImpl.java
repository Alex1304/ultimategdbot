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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.hibernate.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandKernel;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
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
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.retry.Retry;
import reactor.util.function.Tuples;

class BotImpl implements Bot {
	
	private Logger logger;
	private final String token;
	private final String defaultPrefix;
	private final Flux<DiscordClient> discordClients;
	private final DiscordClient mainDiscordClient;
	private final DatabaseImpl database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake attachmentsChannelId;
	private final List<Snowflake> emojiGuildIds;
	private final String releaseVersion;
	private final String supportServerInviteLink;
	private final String authLink;
	private final Properties pluginsProps;
	private CommandKernelImpl cmdKernel;
	private final Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> guildSettingsEntries;

	private BotImpl(Logger logger, String token, String defaultPrefix, Flux<DiscordClient> discordClients, DatabaseImpl database,
			int replyMenuTimeout, Snowflake debugLogChannelId, Snowflake attachmentsChannelId,
			List<Snowflake> emojiGuildIds, String releaseVersion, String supportServerInviteLink, String authLink,
			Properties pluginsProps) {
		this.logger = logger;
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.discordClients = discordClients;
		this.mainDiscordClient = discordClients.blockFirst();
		this.database = database;
		this.replyMenuTimeout = replyMenuTimeout;
		this.debugLogChannelId = debugLogChannelId;
		this.attachmentsChannelId = attachmentsChannelId;
		this.emojiGuildIds = emojiGuildIds;
		this.releaseVersion = releaseVersion;
		this.supportServerInviteLink = supportServerInviteLink;
		this.authLink = authLink;
		this.pluginsProps = pluginsProps;
		this.cmdKernel = null;
		this.guildSettingsEntries = new HashMap<>();
	}

	@Override
	public String getReleaseVersion() {
		return releaseVersion;
	}

	@Override
	public String getSupportServerInviteLink() {
		return supportServerInviteLink;
	}

	@Override
	public String getAuthLink() {
		return authLink;
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
	public Logger getLogger() {
		return logger;
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
				.flatMap(c -> c.createMessage(spec))
				.doOnNext(message -> message.getContent().ifPresent(logger::info));
	}

	@Override
	public Mono<Message> logStackTrace(Context ctx, Throwable t) {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw);
		pw.println(":no_entry_sign: **Something went wrong while executing a command.**");
		pw.println("__User input:__ `" + ctx.getEvent().getMessage().getContent().orElseGet(() -> "(No content)") + "`");
		pw.println("__Stack trace preview (see full trace in internal logs):__");
		t.printStackTrace(pw);
		var trace = sw.toString();
		logger.error(trace);
		return getDebugLogChannel()
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(trace.substring(0, Math.min(trace.length(), 800))));
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
		var logger = LoggerFactory.getLogger("ultimategdbot");
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
			logger.error("presence_activity: Expected one of: ''|'none'|'null', 'playing:<text>', 'watching:<text>', 'listening:<text>'"
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
					logger.error("presence_status: Expected one of 'online', 'idle', 'dnd', 'invisible'. Defaulting to 'online'.");
					return Presence.online(activity);
			}
		}, Presence.online(activity));
		var discordClients = new ShardingClientBuilder(token).build()
				.map(dcb -> dcb.setInitialPresence(presenceStatus))
				.map(dcb -> dcb.setRouterOptions(RouterOptions.builder()
						.onClientResponse(ResponseFunction.emptyIfNotFound())
						.onClientResponse(ResponseFunction.retryOnceOnErrorStatus(500))
						.onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
						.onClientResponse(ResponseFunction.retryWhen(RouteMatcher.route(Routes.MESSAGE_CREATE),
                                Retry.onlyIf(ClientException.isRetryContextStatusCode(500))
										.exponentialBackoffWithJitter(Duration.ofSeconds(2), Duration.ofSeconds(10))))
						.build()))
				.map(DiscordClientBuilder::build)
				.cache();
		var releaseVersion = propParser.parseAsString("bot_release_version");
		var supportServerInviteLink = propParser.parseAsString("support_server_invite_link");
		var authLink = propParser.parseAsString("bot_auth_link");
		return new BotImpl(logger, token, defaultPrefix, discordClients, database, replyMenuTimeout, debugLogChannelId,
				attachmentsChannelId, emojiGuildIds, releaseVersion, supportServerInviteLink, authLink, pluginsProps);
	}

	public void start() {
		var loader = ServiceLoader.load(Plugin.class);
		var parser = new PropertyParser(Main.PLUGINS_PROPS_FILE.toString(), pluginsProps);
		var commandsByPlugins = new TreeMap<String, Set<Command>>();
		var commandsByAliases = new HashMap<String, Command>();
		var subCommands = new HashMap<Command, Map<String, Command>>();
		var successfullyLoadedPlugins = new HashSet<Plugin>();
		for (var plugin : loader) {
			try {
				logger.info("Loading plugin: {}...", plugin.getName());
				plugin.setup(this, parser);
				database.addAllMappingResources(plugin.getDatabaseMappingResources());
				guildSettingsEntries.put(plugin, plugin.getGuildConfigurationEntries());
				var cmdSet = new TreeSet<Command>(Comparator.comparing(cmd -> BotUtils.joinAliases(cmd.getAliases())));
				cmdSet.addAll(plugin.getProvidedCommands());
				commandsByPlugins.put(plugin.getName(), Collections.unmodifiableSet(cmdSet));
				for (var cmd : cmdSet) {
					for (var alias : cmd.getAliases()) {
						commandsByAliases.put(alias, cmd);
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
								subCmdMap.put(alias, subcmd);
							}
						}
						subCommands.put(element, subCmdMap);
						subCmdDeque.addAll(elementSubcmds);
					}
					logger.info("Loaded command: {} {}", cmd.getClass().getName(), cmd.getAliases());
				}
				successfullyLoadedPlugins.add(plugin);
			} catch (RuntimeException e) {
				logger.warn("Failed to load plugin {}", plugin.getName());
				e.printStackTrace();
			}
		}
		this.cmdKernel = new CommandKernelImpl(this, commandsByAliases, subCommands, commandsByPlugins);
		try {
			database.configure();
		} catch (MappingException e) {
			logger.error("Oops! There is an error in the database mapping configuration!");
			throw e;
		}
		discordClients.concatMap(client -> client.getEventDispatcher().on(ReadyEvent.class)
						.next()
						.map(readyEvent -> Tuples.of(client, readyEvent.getGuilds().size())))
				.concatMap(TupleUtils.function((client, guildCount) -> client.getEventDispatcher().on(GuildCreateEvent.class)
						.take(guildCount)
						.collectList()))
				.collectList()
				.doOnNext(guildCreateEvents -> {
					var sb = new StringBuilder("Bot started!\n");
					for (var shardGuildCreateEvents : guildCreateEvents) {
						shardGuildCreateEvents.stream().findAny().ifPresent(gce -> {
							var shardNum = gce.getClient().getConfig().getShardIndex();
							sb.append("> Shard ").append(shardNum).append(": successfully connected to ").append(shardGuildCreateEvents.size()).append(" guilds\n");
						});
					}
					sb.append("Serving " + guildCreateEvents.stream().mapToInt(List::size).sum() + " guilds across " + guildCreateEvents.size() + " shards!");
					logger.info(sb.toString());
					BotUtils.sendMultipleSimpleMessagesToOneChannel(getDebugLogChannel(), BotUtils.chunkMessage(sb.toString())).subscribe();
					successfullyLoadedPlugins.forEach(Plugin::onBotReady);
					cmdKernel.start();
				}).subscribe();
		discordClients.flatMap(DiscordClient::login).blockLast();
	}

	@Override
	public Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> getGuildSettingsEntries() {
		return Collections.unmodifiableMap(guildSettingsEntries);
	}

	@Override
	public CommandKernel getCommandKernel() {
		return cmdKernel;
	}
}
