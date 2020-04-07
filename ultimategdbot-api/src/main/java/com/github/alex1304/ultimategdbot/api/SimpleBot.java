package com.github.alex1304.ultimategdbot.api;

import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.CommandKernel;
import com.github.alex1304.ultimategdbot.api.database.Database;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.User;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.ImmutableMessageCreateRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.discordjson.possible.Possible;
import discord4j.gateway.GatewayObserver;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.rest.util.Snowflake;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;

/**
 * Basic implementation of a Discord bot, configured with default Discord4J
 * settings.
 */
public class SimpleBot implements Bot {
	private static final Logger LOGGER = Loggers.getLogger(SimpleBot.class);
	
	private final BotConfig config;
	private final DiscordClient discordClient;
	private final Database database = new Database();
	private final CommandKernel cmdKernel = new CommandKernel(this);
	private final Set<Plugin> plugins = synchronizedSet(new HashSet<>());
	private final Mono<Snowflake> ownerId;
	
	private volatile GatewayDiscordClient gateway;

	private SimpleBot(BotConfig config, DiscordClient discordClient) {
		this.config = config;
		this.discordClient = discordClient;
		this.ownerId = discordClient.getApplicationInfo()
				.map(ApplicationInfoData::owner)
				.map(UserData::id)
				.map(Snowflake::of)
				.cache();
	}
	
	@Override
	public BotConfig config() {
		return config;
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public CommandKernel commandKernel() {
		return cmdKernel;
	}

	@Override
	public DiscordClient rest() {
		return discordClient;
	}

	@Override
	public GatewayDiscordClient gateway() {
		return gateway;
	}

	@Override
	public Set<Plugin> plugins() {
		return unmodifiableSet(plugins);
	}

	@Override
	public Mono<User> owner() {
		return ownerId.flatMap(gateway::getUserById);
	}

	@Override
	public Mono<Void> log(String message) {
		return Mono.justOrEmpty(config.getDebugLogChannelId())
				.map(discordClient::getChannelById)
				.flatMap(c -> c.createMessage(ImmutableMessageCreateRequest.builder().content(Possible.of(message)).build()))
				.onErrorResume(e -> Mono.fromRunnable(() -> LOGGER.warn("Failed to send a message to log channel: " + message, e)))
				.then();
	}

	@Override
	public Mono<String> emoji(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		if (gateway == null) {
			return Mono.just(defaultVal);
		}
		return Flux.fromIterable(config.getEmojiGuildIds())
				.flatMap(gateway::getGuildById)
				.flatMap(Guild::getEmojis)
				.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal);
	}

	@Override
	public void start() {
		gateway = discordClient.gateway()
				.setInitialStatus(shard -> config.getStatus())
				.setStoreService(MappingStoreService.create()
						.setMapping(new CaffeineStoreService(builder -> {
							var maxSize = config.getMessageCacheMaxSize();
							if (maxSize >= 1) {
								builder.maximumSize(maxSize);
							}
							return builder;
						}), MessageData.class)
						.setFallback(new JdkStoreService()))
				.setEventDispatcher(EventDispatcher.withLatestEvents(Queues.SMALL_BUFFER_SIZE))
				.setGatewayObserver((state, identifyOptions) -> {
					if (state == GatewayObserver.CONNECTED
							|| state == GatewayObserver.DISCONNECTED
							|| state == GatewayObserver.DISCONNECTED_RESUME
							|| state == GatewayObserver.RETRY_FAILED
							|| state == GatewayObserver.RETRY_RESUME_STARTED
							|| state == GatewayObserver.RETRY_STARTED
							|| state == GatewayObserver.RETRY_SUCCEEDED) {
						log("Shard " + identifyOptions.getShardIndex() + ": " + state).subscribe();
					}
				})
				.setDestroyHandler(__ -> log("Bot disconnected"))
				.setEntityRetrievalStrategy(EntityRetrievalStrategy.STORE)
				.setAwaitConnections(true)
				.login()
				.single()
				.block();
		
		var databaseMappingResources = synchronizedSet(new HashSet<String>());
		Flux.fromIterable(ServiceLoader.load(PluginBootstrap.class))
				.flatMap(pluginBootstrap -> pluginBootstrap.initPluginProperties()
						.defaultIfEmpty(PropertyReader.EMPTY)
						.flatMap(pluginProperties -> pluginBootstrap.setup(this, pluginProperties))
						.single()
						.doOnError(e -> LOGGER.error("Failed to setup plugin " + pluginBootstrap.getClass().getName(), e)))
				.doOnNext(plugins::add)
				.doOnNext(plugin -> databaseMappingResources.addAll(plugin.getDatabaseMappingResources()))
				.doOnNext(plugin -> cmdKernel.addProvider(plugin.getCommandProvider()))
				.doOnNext(plugin -> LOGGER.debug("Plugin {} is providing commands: {}", plugin.getName(), plugin.getCommandProvider()))
				.then(Mono.fromRunnable(() -> {
					database.configure(databaseMappingResources);
					cmdKernel.start();
				}))
				.thenEmpty(Flux.fromIterable(plugins).flatMap(Plugin::runOnReady))
				.then(gateway.onDisconnect())
				.block();
	}

	/**
	 * Creates a new {@link SimpleBot} using the given config.
	 * 
	 * @param config the bot config
	 * @return a new {@link SimpleBot}
	 */
	public static SimpleBot create(BotConfig config) {
		requireNonNull(config);
		
		var discordClient = DiscordClient.builder(config.getToken())
				.onClientResponse(ResponseFunction.emptyIfNotFound())
				.onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
				.onClientResponse(request -> response -> response.timeout(config.getRestTimeout()))
//				.setRequestQueueFactory(RequestQueueFactory.backedByProcessor(
//						() -> EmitterProcessor.create(config.getRestBufferSize(), false), FluxSink.OverflowStrategy.LATEST))
				.build();
		
		return new SimpleBot(config, discordClient);
	}
}
