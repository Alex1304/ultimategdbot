package com.github.alex1304.ultimategdbot.core.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.MappingException;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.core.handler.CommandHandler;
import com.github.alex1304.ultimategdbot.core.handler.Handler;
import com.github.alex1304.ultimategdbot.core.handler.ReplyMenuHandler;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotImpl implements Bot {
	
	private final String token;
	private final String defaultPrefix;
	private final Snowflake supportServerId;
	private final Snowflake moderatorRoleId;
	private final String releaseChannel;
	private final DiscordClient client;
	private final Database database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake[] emojiGuildIds;
	
	private final CommandHandler cmdHandler;
	private final ReplyMenuHandler replyMenuHandler;
	private final Set<Handler> handlers;
	
	private BotImpl(String token, String defaultPrefix, Snowflake supportServerId, Snowflake moderatorRoleId, String releaseChannel,
			DiscordClient client, Database database, int replyMenuTimeout, Snowflake debugLogChannelId, Snowflake[] emojiGuildIds) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.supportServerId = supportServerId;
		this.moderatorRoleId = moderatorRoleId;
		this.releaseChannel = releaseChannel;
		this.client = client;
		this.database = database;
		this.replyMenuTimeout = replyMenuTimeout;
		this.debugLogChannelId = debugLogChannelId;
		this.emojiGuildIds = emojiGuildIds;
		
		this.cmdHandler = new CommandHandler(this);
		this.replyMenuHandler = new ReplyMenuHandler(this);
		this.handlers = Set.of(cmdHandler, replyMenuHandler);
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
	public Mono<Guild> getSupportServer() {
		return client.getGuildById(supportServerId);
	}

	@Override
	public Mono<Role> getModeratorRole() {
		return client.getRoleById(supportServerId, moderatorRoleId);
	}

	@Override
	public String getReleaseChannel() {
		return releaseChannel;
	}

	@Override
	public DiscordClient getDiscordClient() {
		return client;
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
		return client.getChannelById(debugLogChannelId);
	}

	@Override
	public Mono<Message> log(String message) {
		return log(mcs -> mcs.setContent(message));
	}

	@Override
	public Mono<Message> log(Consumer<MessageCreateSpec> spec) {
		return client.getChannelById(debugLogChannelId)
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(spec));
	}

	@Override
	public Mono<String> getEmoji(String emojiName) {
		return Mono.<String>create(sink -> {
			client.getGuilds()
					.filter(g -> Arrays.stream(emojiGuildIds).anyMatch(id -> g.getId().equals(id)))
					.reduce(Flux.<GuildEmoji>empty(), (flux, g) -> flux.mergeWith(g.getEmojis()))
					.doOnSuccess(flux -> {
						if (flux == null) {
							sink.success();
						}
						flux.filter(em -> em.getName().equalsIgnoreCase(emojiName))
							.take(1)
							.map(GuildEmoji::asFormat)
							.singleOrEmpty()
							.doOnSuccess(result -> {
								if (result == null) {
									sink.success();
								}
								sink.success(result);
							})
							.doOnError(sink::error)
							.subscribe();
					})
					.doOnError(sink::error)
					.subscribe();
		}).defaultIfEmpty(":" + emojiName + ":");
	}

	public static Bot buildFromProperties(Properties props, Properties hibernateProps) {
		var token = parseProperty(props, "token", String::toString);
		var defaultPrefix = parseProperty(props, "default_prefix", String::toString);
		var supportServerId = parseProperty(props, "support_server_id", Snowflake::of);
		var moderatorRoleId = parseProperty(props, "moderator_role_id", Snowflake::of);
		var releaseChannel = parseProperty(props, "release_channel", String::toString);
		var builder = new DiscordClientBuilder(token);
		var database = new DatabaseImpl(hibernateProps);
		var replyMenuTimeout = parseProperty(props, "reply_menu_timeout", Integer::parseInt);
		var debugLogChannelId = parseProperty(props, "debug_log_channel_id", Snowflake::of);
		var emojiGuildIds = parseProperty(props, "emoji_guild_ids", value -> {
			var parts = value.split(",");
			var result = new Snowflake[parts.length];
			for (var i = 0 ; i < parts.length ; i++) {
				try {
					result[i] = Snowflake.of(parts[i]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The value '" + parts[i] + "' is not a valid ID for the property 'emoji_guild_ids'");
				}
			}
			return result;
		});

		return new BotImpl(token, defaultPrefix, supportServerId, moderatorRoleId, releaseChannel, builder.build(),
				database, replyMenuTimeout, debugLogChannelId, emojiGuildIds);
	}
	
	private static <P> P parseProperty(Properties props, String name, Function<String, P> parser) {
		var prop = props.getProperty(name);
		if (prop == null) {
			throw new IllegalArgumentException("The property '" + name + "' is missing");
		}
		try {
			return parser.apply(prop);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The property '" + name + "' was expected to be a numeric value");
		}
	}

	@Override
	public void start() {
		handlers.forEach(Handler::prepare);
		try {
			database.configure();
		} catch (MappingException e) {
			System.err.println("Oops! There is an error in the database mapping configuration!");
			throw e;
		}
		client.getEventDispatcher().on(ReadyEvent.class).subscribe(__ -> handlers.forEach(Handler::listen));
		client.login().block();
	}

	@Override
	public String openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout) {
		Objects.requireNonNull(ctx);
		Objects.requireNonNull(msg);
		Objects.requireNonNull(menuItems);
		return replyMenuHandler.open(ctx, msg, menuItems, deleteOnReply, deleteOnTimeout);
	}

	@Override
	public void closeReplyMenu(String identifier) {
		Objects.requireNonNull(identifier);
		replyMenuHandler.close(identifier);
	}

	@Override
	public Command getCommandForName(String name) {
		return cmdHandler.getCommandForName(name);
	}

	@Override
	public Set<Plugin> getPlugins() {
		return cmdHandler.getPlugins();
	}
}
