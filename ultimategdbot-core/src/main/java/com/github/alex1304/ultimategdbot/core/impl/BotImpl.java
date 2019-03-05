package com.github.alex1304.ultimategdbot.core.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.github.alex1304.ultimategdbot.api.utils.Utils;
import com.github.alex1304.ultimategdbot.core.handler.CommandHandler;
import com.github.alex1304.ultimategdbot.core.handler.Handler;
import com.github.alex1304.ultimategdbot.core.handler.ReplyMenuHandler;
import com.github.alex1304.ultimategdbot.core.main.Main;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.shard.ShardingClientBuilder;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class BotImpl implements Bot {
	
	private final String token;
	private final String defaultPrefix;
	private final Snowflake supportServerId;
	private final Snowflake moderatorRoleId;
	private final String releaseChannel;
	private final Flux<DiscordClient> discordClients;
	private final DatabaseImpl database;
	private final int replyMenuTimeout;
	private final Snowflake debugLogChannelId;
	private final Snowflake attachmentsChannelId;
	private final List<Snowflake> emojiGuildIds;
	private final Properties pluginsProps;
	
	private final CommandHandler cmdHandler;
	private final ReplyMenuHandler replyMenuHandler;
	private final Set<Handler> handlers;
	private final Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> guildSettingsEntries;
	
	private BotImpl(String token, String defaultPrefix, Snowflake supportServerId, Snowflake moderatorRoleId, String releaseChannel,
			Flux<DiscordClient> discordClients, DatabaseImpl database, int replyMenuTimeout, Snowflake debugLogChannelId, Snowflake attachmentsChannelId, List<Snowflake> emojiGuildIds, Properties pluginsProps) {
		this.token = token;
		this.defaultPrefix = defaultPrefix;
		this.supportServerId = supportServerId;
		this.moderatorRoleId = moderatorRoleId;
		this.releaseChannel = releaseChannel;
		this.discordClients = discordClients;
		this.database = database;
		this.replyMenuTimeout = replyMenuTimeout;
		this.debugLogChannelId = debugLogChannelId;
		this.attachmentsChannelId = attachmentsChannelId;
		this.emojiGuildIds = emojiGuildIds;
		this.pluginsProps = pluginsProps;
		
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
		return discordClients.flatMap(client -> client.getGuildById(supportServerId)).next();
	}

	@Override
	public Mono<Role> getModeratorRole() {
		return discordClients.flatMap(client -> client.getRoleById(supportServerId, moderatorRoleId)).next();
	}

	@Override
	public String getReleaseChannel() {
		return releaseChannel;
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
		return discordClients.flatMap(client -> client.getChannelById(debugLogChannelId)).next();
	}

	@Override
	public Mono<Channel> getAttachmentsChannel() {
		return discordClients.flatMap(client -> client.getChannelById(attachmentsChannelId)).next();
	}

	@Override
	public Mono<Message> log(String message) {
		return log(mcs -> mcs.setContent(message));
	}

	@Override
	public Mono<Message> log(Consumer<MessageCreateSpec> spec) {
		return discordClients.flatMap(client -> client.getChannelById(debugLogChannelId))
				.next()
				.ofType(MessageChannel.class)
				.flatMap(c -> c.createMessage(spec));
	}

	@Override
	public Flux<Message> logStackTrace(Context ctx, Throwable t) {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw);
		pw.println(":no_entry_sign: **An internal error occured while executing a command.**");
		pw.println("__User input:__ `" + ctx.getEvent().getMessage().getContent().orElseGet(() -> "(No content)") + "`");
		pw.println("__Full stack trace:__");
		t.printStackTrace(pw);
		var chunks = Utils.chunkMessage(sw.toString());
		var i = 0;
		for (var chunk : List.copyOf(chunks)) {
			if (i != 0) {
				chunks.set(i, "_   _ " + chunk.substring(1));
			}
			i++;
		}
		System.out.println(chunks);
		return Utils.sendMultipleSimpleMessagesToOneChannel(getDebugLogChannel(), chunks);
	}

	@Override
	public String getEmoji(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		return discordClients.flatMap(DiscordClient::getGuilds)
				.filter(g -> emojiGuildIds.stream().anyMatch(g.getId()::equals))
				.flatMap(Guild::getEmojis)
				.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal).block();
	}

	public static Bot buildFromProperties(Properties props, Properties hibernateProps, Properties pluginsProps) {
		var propParser = new PropertyParser(Main.PROPS_FILE.toString(), props);
		var token = propParser.parseAsString("token");
		var defaultPrefix = propParser.parseAsString("default_prefix");
		var supportServerId = propParser.parse("support_server_id", Snowflake::of);
		var moderatorRoleId = propParser.parse("moderator_role_id", Snowflake::of);
		var releaseChannel = propParser.parseAsString("release_channel");
		var discordClients = new ShardingClientBuilder(token).build()
				.map(dcb -> dcb.setEventScheduler(Schedulers.elastic()))
				.map(DiscordClientBuilder::build)
				.cache();
		var database = new DatabaseImpl(hibernateProps);
		var replyMenuTimeout = propParser.parseAsInt("reply_menu_timeout");
		var debugLogChannelId = propParser.parse("debug_log_channel_id", Snowflake::of);
		var attachmentsChannelId = propParser.parse("attachments_channel_id", Snowflake::of);
		var emojiGuildIds = propParser.parseAsList("emoji_guild_ids", ",", Snowflake::of);

		return new BotImpl(token, defaultPrefix, supportServerId, moderatorRoleId, releaseChannel, discordClients,
				database, replyMenuTimeout, debugLogChannelId, attachmentsChannelId, emojiGuildIds, pluginsProps);
	}

	@Override
	public void start() {
		var loader = ServiceLoader.load(Plugin.class);
		var parser = new PropertyParser(Main.PLUGINS_PROPS_FILE.toString(), pluginsProps);
		for (var plugin : loader) {
			try {
				System.out.printf("Loading plugin: %s...\n", plugin.getName());
				plugin.setup(parser);
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
			} catch (RuntimeException e) {
				System.out.println("WARNING: Failed to load plugin " + plugin.getName());
				e.printStackTrace();
			}
		}
		try {
			database.configure();
		} catch (MappingException e) {
			System.err.println("Oops! There is an error in the database mapping configuration!");
			throw e;
		}
		discordClients.flatMap(client -> client.getEventDispatcher().on(ReadyEvent.class))
				.map(readyEvent -> readyEvent.getGuilds().size())
				.flatMap(size -> discordClients.flatMap(client0 -> client0.getEventDispatcher().on(GuildCreateEvent.class))
						.take(size)
						.collectList())
				.subscribe(guildCreateEvents -> {
					System.out.println("Successfully connected to " + guildCreateEvents.size() + " guilds");
					handlers.forEach(Handler::listen);
				});
		discordClients.flatMap(DiscordClient::login).blockLast();
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
