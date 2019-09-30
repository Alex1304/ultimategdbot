package com.github.alex1304.ultimategdbot.api;

import static com.github.alex1304.ultimategdbot.api.utils.BotUtils.debugError;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.command.Command;
import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The command kernel coordinates the command providers from all plugins. It
 * also holds a blacklist to restrict the usage of commands from certain guilds,
 * channels or users. It listens to message create events and dispatch the to
 * the proper command providers to trigger the execution of commands.
 */
public class CommandKernel {
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot.commandkernel");

	private final Bot bot;
	private final Set<CommandProvider> providers = new HashSet<>();
	private final Set<Long> blacklist = new HashSet<>();
	private final ConcurrentHashMap<Long, String> guildPrefixCache = new ConcurrentHashMap<>();
	
	public CommandKernel(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
	}
	
	/**
	 * Adds a new command provider to this kernel.
	 * 
	 * @param provider the command provider to add
	 */
	public void addProvider(CommandProvider provider) {
		providers.add(Objects.requireNonNull(provider));
	}

	/**
	 * Processes a MessageCreateEvent. It first checks if neither the guild, the
	 * channel and the user is blacklisted, then proceeds to find the guild-specific
	 * prefix and trigger the command if it matches with one provided by one of the
	 * command providers.
	 * 
	 * @param event the {@link MessageCreateEvent} that was received
	 * @return a Mono that completes when the command has terminated. If the
	 *         blacklist check doesn't pass, a Mono completing immediately is
	 *         returned. Any errors that may occur when running the command are
	 *         forwarded through this Mono.
	 */
	public Mono<Void> processEvent(MessageCreateEvent event) {
		Objects.requireNonNull(event);
		var authorId = event.getMessage().getAuthor().map(User::getId);
		var guildId = event.getGuildId();
		var channelId = event.getMessage().getChannelId();
		if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
			return Mono.empty();
		}
		if (authorId.map(id -> blacklist.contains(id.asLong())).orElse(false)) {
			LOGGER.debug("Ignoring event due to AUTHOR being blacklisted: {}", event);
			return Mono.empty();
		}
		if (guildId.map(id -> blacklist.contains(id.asLong())).orElse(false)) {
			LOGGER.debug("Ignoring event due to GUILD being blacklisted: {}", event);
			return Mono.empty();
		}
		if (blacklist.contains(channelId.asLong())) {
			LOGGER.debug("Ignoring event due to CHANNEL being blacklisted: {}", event);
			return Mono.empty();
		}
		return findGuildSpecificPrefix(event)
				.flatMapMany(prefix -> Flux.fromIterable(providers)
						.flatMap(provider -> event.getMessage().getChannel()
								.flatMap(channel -> Mono.justOrEmpty(provider.provideFromEvent(bot, prefix, event, channel)))))
				.flatMap(executable -> executable.execute()
						.onErrorResume(e -> Mono.when(event.getMessage().getChannel()
								.flatMap(c -> c.createMessage(":no_entry_sign: Something went wrong. "
										+ "A crash report has been sent to the developer. Sorry for "
										+ "the inconvenience."))
								.onErrorResume(__ -> Mono.empty()),
						debugError("Something went wrong when executing a command", executable.getContext(), e),
						Mono.fromRunnable(() -> LOGGER.error("Something went wrong when executing a command. Context dump: "
								+ executable.getContext(), e)))))
				.then();
	}
	
	public void start() {
		bot.getDiscordClients()
				.flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
				.flatMap(event -> processEvent(event).onErrorResume(e -> Mono
						.fromRunnable(() -> LOGGER.error("An error occured when processing event " + event, e))))
				.retry()
				.repeat()
				.subscribe();
	}
	
	/**
	 * Gets a command instance corresponding to the given alias.
	 *  
	 * @param alias the alias of the command
	 * @return the corresponding command instance, or null if not found
	 */
	public Command getCommandByAlias(String alias) {
		for (var p : providers) {
			var cmd = p.getCommandByAlias(alias);
			if (cmd != null) {
				return cmd;
			}
		}
		return null;
	}
	
	/**
	 * Gets an unmodifiable set of IDs that are not allowed to perform operations on
	 * the command kernel.
	 * 
	 * @return an unmodifiable set of IDs
	 */
	public Set<Long> getBlacklist() {
		return Collections.unmodifiableSet(blacklist);
	}
	
	/**
	 * Blacklists a new ID.
	 * 
	 * @param id the ID of a user, a channel or a guild that won't be allowed to run
	 *           commands from this kernel.
	 */
	public void blacklist(long id) {
		blacklist.add(id);
	}

	/**
	 * Removes an ID from the blacklist.
	 * 
	 * @param id the ID of a user, a channel or a guild that will be allowed to run
	 *           commands from this kernel again.
	 */
	public void unblacklist(long id) {
		blacklist.remove(id);
	}

	/**
	 * Forces this command kernel to evict the prefix from cache for the specified
	 * guild.
	 * 
	 * @param guildId the guild id
	 */
	public void invalidateCachedPrefixForGuild(long guildId) {
		guildPrefixCache.remove(guildId);
		LOGGER.debug("Invalidated cached prefix for guild {}", guildId);
	}
	
	private Mono<String> findGuildSpecificPrefix(MessageCreateEvent event) {
		return Mono.justOrEmpty(event.getGuildId())
				.map(Snowflake::asLong)
				.flatMap(guildId -> Mono.justOrEmpty(guildPrefixCache.get(guildId))
						.switchIfEmpty(bot.getDatabase()
								.findByID(NativeGuildSettings.class, guildId)
								.switchIfEmpty(Mono.fromCallable(() -> {
											var gs = new NativeGuildSettings();
											gs.setGuildId(guildId);
											return gs;
										})
										.flatMap(gs -> bot.getDatabase().save(gs)
												.then(Mono.fromRunnable(() -> LOGGER.debug("Created guild settings: {}", gs)))
												.onErrorResume(e -> Mono.fromRunnable(() -> LOGGER
														.error("Unable to save guild settings for " + guildId, e)))
												.thenReturn(gs)))
								.flatMap(gs -> Mono.justOrEmpty(gs.getPrefix()))
								.defaultIfEmpty(bot.getDefaultPrefix())
								.map(String::strip)
								.doOnNext(prefix -> guildPrefixCache.put(guildId, prefix))));
	}
}
