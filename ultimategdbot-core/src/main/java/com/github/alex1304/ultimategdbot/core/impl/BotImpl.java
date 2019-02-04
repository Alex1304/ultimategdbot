package com.github.alex1304.ultimategdbot.core.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.core.handler.CommandHandler;
import com.github.alex1304.ultimategdbot.core.handler.Handler;
import com.github.alex1304.ultimategdbot.core.handler.ReplyMenuHandler;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
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
	private final CommandHandler cmdHandler;
	private final ReplyMenuHandler replyMenuHandler;
	private final Set<Handler> handlers;
	
	private BotImpl(String token, String defaultPrefix, Snowflake supportServerId, Snowflake moderatorRoleId, String releaseChannel,
			DiscordClient client, Database database, int replyMenuTimeout) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.supportServerId = supportServerId;
		this.moderatorRoleId = moderatorRoleId;
		this.releaseChannel = releaseChannel;
		this.client = client;
		this.database = database;
		this.replyMenuTimeout = replyMenuTimeout;
		this.cmdHandler = new CommandHandler(this);
		this.replyMenuHandler = new ReplyMenuHandler(this);
		this.handlers = Set.of(cmdHandler, replyMenuHandler);
	}

	@Override
	public String getToken() {
		return token;
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

	public static Bot buildFromProperties(Properties props, Properties hibernateProps) {
		var token = Objects.requireNonNull(props.getProperty("token"));
		var defaultPrefix = Objects.requireNonNull(props.getProperty("default_prefix"));
		var supportServerId = Snowflake.of(Objects.requireNonNull(props.getProperty("support_server_id")));
		var moderatorRoleId = Snowflake.of(Objects.requireNonNull(props.getProperty("moderator_role_id")));
		var releaseChannel = Objects.requireNonNull(props.getProperty("release_channel"));
		var builder = new DiscordClientBuilder(token);
		var database = new DatabaseImpl(hibernateProps);
		var replyMenuTimeout = Integer.parseInt(Objects.requireNonNull(props.getProperty("reply_menu_timeout")));

		return new BotImpl(token, defaultPrefix, supportServerId, moderatorRoleId, releaseChannel, builder.build(), database, replyMenuTimeout);
	}

	@Override
	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	@Override
	public void start() {
		database.configure();
		handlers.forEach(Handler::prepare);
		client.getEventDispatcher().on(ReadyEvent.class).subscribe(__ -> handlers.forEach(Handler::listen));
		client.login().block();
	}

	@Override
	public Set<Command> getAvailableCommands() {
		return Collections.unmodifiableSet(cmdHandler.getAvailableCommands());
	}

	@Override
	public void openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout) {
		replyMenuHandler.open(ctx, msg, menuItems, deleteOnReply, deleteOnTimeout);
	}

	@Override
	public int getReplyMenuTimeout() {
		return replyMenuTimeout;
	}
}
