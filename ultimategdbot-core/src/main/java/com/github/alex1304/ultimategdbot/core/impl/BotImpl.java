package com.github.alex1304.ultimategdbot.core.impl;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.MappingException;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;
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
	private final DatabaseImpl database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final List<Snowflake> emojiGuildIds;
	
	private final CommandHandler cmdHandler;
	private final ReplyMenuHandler replyMenuHandler;
	private final Set<Handler> handlers;
	private final Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> guildSettingsEntries;
	
	private BotImpl(String token, String defaultPrefix, Snowflake supportServerId, Snowflake moderatorRoleId, String releaseChannel,
			DiscordClient client, DatabaseImpl database, int replyMenuTimeout, Snowflake debugLogChannelId, List<Snowflake> emojiGuildIds) {
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
		this.guildSettingsEntries = new HashMap<>();
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
					.filter(g -> emojiGuildIds.stream().anyMatch(id -> g.getId().equals(id)))
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
		var propParser = new PropertyParser(props);
		var token = propParser.parseAsString("token");
		var defaultPrefix = propParser.parseAsString("default_prefix");
		var supportServerId =  propParser.parse("support_server_id", Snowflake::of);
		var moderatorRoleId = propParser.parse("moderator_role_id", Snowflake::of);
		var releaseChannel = propParser.parseAsString("release_channel");
		var builder = new DiscordClientBuilder(token);
		var database = new DatabaseImpl(hibernateProps);
		var replyMenuTimeout = propParser.parseAsInt("reply_menu_timeout");
		var debugLogChannelId = propParser.parse("debug_log_channel_id", Snowflake::of);
		var emojiGuildIds = propParser.parseAsList("emoji_guild_ids", ",", Snowflake::of);

		return new BotImpl(token, defaultPrefix, supportServerId, moderatorRoleId, releaseChannel, builder.build(),
				database, replyMenuTimeout, debugLogChannelId, emojiGuildIds);
	}

	@Override
	public void start() {
		var loader = ServiceLoader.load(Plugin.class);
		for (var plugin : loader) {
			System.out.printf("Loading plugin: %s...\n", plugin.getName());
			database.addAllMappingResources(plugin.getDatabaseMappingResources());
			guildSettingsEntries.put(plugin, plugin.getGuildConfigurationEntries(this));
			var cmdSet = plugin.getProvidedCommands();
			cmdHandler.getCommandsByPlugins().put(plugin, cmdSet);
			for (var cmd : cmdSet) {
				for (var alias : cmd.getAliases()) {
					cmdHandler.getCommands().put(alias, cmd);
				}
				// Add all subcommands
				var subCmdDeque = new ArrayDeque<Command>();
				subCmdDeque.push(cmd);
				while (!subCmdDeque.isEmpty()) {
					var element = subCmdDeque.pop();
					var elementSubcmds = element.getSubcommands();
					if (cmdHandler.getSubCommands().containsKey(element)) {
						continue;
					}
					var subCmdMap = new HashMap<String, Command>();
					for (var subcmd : elementSubcmds) {
						for (var alias : subcmd.getAliases()) {
							subCmdMap.put(alias, subcmd);
						}
					}
					cmdHandler.getSubCommands().put(element, subCmdMap);
					subCmdDeque.addAll(elementSubcmds);
				}
				System.out.printf("\tLoaded command: %s %s\n", cmd.getClass().getName(), cmd.getAliases());
			}
			cmdHandler.getPlugins().add(plugin);
		}
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
	public Map<Plugin, Set<Command>> getCommandsFromPlugins() {
		return Collections.unmodifiableMap(cmdHandler.getCommandsByPlugins());
	}

	@Override
	public Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> getGuildSettingsEntries() {
		return Collections.unmodifiableMap(guildSettingsEntries);
	}
}
