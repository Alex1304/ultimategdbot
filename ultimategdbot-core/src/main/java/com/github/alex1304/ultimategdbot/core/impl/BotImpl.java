package com.github.alex1304.ultimategdbot.core.impl;

import java.util.Objects;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.core.handler.CommandHandler;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
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
	private final CommandHandler commandHandler;
	
	private BotImpl(String token, String defaultPrefix, Snowflake supportServerId, Snowflake moderatorRoleId, String releaseChannel,
			DiscordClient client, Database database) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.supportServerId = supportServerId;
		this.moderatorRoleId = moderatorRoleId;
		this.releaseChannel = releaseChannel;
		this.client = client;
		this.database = database;
		this.commandHandler = new CommandHandler(this);
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
		var officialGuildID = Snowflake.of(Objects.requireNonNull(props.getProperty("official_guild_id")));
		var moderatorRoleID = Snowflake.of(Objects.requireNonNull(props.getProperty("moderator_role_id")));
		var releaseChannel = Objects.requireNonNull(props.getProperty("release_channel"));
		var builder = new DiscordClientBuilder(token);
		var database = new DatabaseImpl(hibernateProps);

		return new BotImpl(token, defaultPrefix, officialGuildID, moderatorRoleID, releaseChannel, builder.build(), database);
	}

	@Override
	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	@Override
	public void start() {
		database.configure();
		commandHandler.loadCommands();
		client.getEventDispatcher().on(ReadyEvent.class).subscribe(__ -> commandHandler.listen());
		client.login().block();
	}
}
