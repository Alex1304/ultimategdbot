package com.github.alex1304.ultimategdbot.api;

import static com.github.alex1304.ultimategdbot.api.util.BotUtils.logCommandError;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.PermissionChecker;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * The command kernel coordinates the command providers from all plugins. It
 * also holds a blacklist to restrict the usage of commands from certain guilds,
 * channels or users. It listens to message create events and dispatch them to
 * the proper command providers to trigger the execution of commands.
 */
public class CommandKernel {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandKernel.class);

	private final Bot bot;
	private final Set<CommandProvider> providers = synchronizedSet(new HashSet<>());
	private final Set<Long> blacklist = synchronizedSet(new HashSet<>());
	private final ConcurrentHashMap<Long, String> prefixByGuild = new ConcurrentHashMap<>();
	private final PermissionChecker permissionChecker = new PermissionChecker();
	
	public CommandKernel(Bot bot) {
		this.bot = requireNonNull(bot);
	}
	
	/**
	 * Adds a new command provider to this kernel.
	 * 
	 * @param provider the command provider to add
	 */
	public void addProvider(CommandProvider provider) {
		providers.add(requireNonNull(provider));
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
		requireNonNull(event);
		var authorId = event.getMessage().getAuthor().map(User::getId);
		var guildId = event.getGuildId();
		var channelId = event.getMessage().getChannelId();
		if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
			return Mono.empty();
		}
		var prefix = guildId.map(Snowflake::asLong).map(prefixByGuild::get).orElse(bot.getConfig().getDefaultPrefix());
		return Flux.fromIterable(providers)
				.flatMap(provider -> event.getMessage().getChannel()
						.flatMap(channel -> provider.provideFromEvent(bot, prefix, event, channel)))
				.filter(executable -> {
					if (authorId.map(id -> blacklist.contains(id.asLong())).orElse(false)) {
						LOGGER.debug("Ignoring command due to AUTHOR being blacklisted: {}", executable);
						return false;
					}
					if (guildId.map(id -> blacklist.contains(id.asLong())).orElse(false)) {
						LOGGER.debug("Ignoring command due to GUILD being blacklisted: {}", executable);
						return false;
					}
					if (blacklist.contains(channelId.asLong())) {
						LOGGER.debug("Ignoring command due to CHANNEL being blacklisted: {}", executable);
						return false;
					}
					return true;
				})
				.flatMap(executable -> executable.execute()
						.onErrorResume(e -> logCommandError(LOGGER, executable.getContext(), e)))
				.then();
	}
	
	/**
	 * Starts listening to message create events.
	 */
	public void start() {
		bot.getGateway().on(MessageCreateEvent.class, this::processEvent).subscribe();
	}
	
	/**
	 * Gets the permission checker used to check permissions for commands provided
	 * by this kernel.
	 * 
	 * @return the permission checker
	 */
	public PermissionChecker getPermissionChecker() {
		return permissionChecker;
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
	 * Sets a prefix specific for the given guild. If one was already set for the
	 * same guild, it is overwritten.
	 * 
	 * @param guildId the guild id
	 * @param prefix  the new prefix. May be null, in which case the prefix is
	 *                removed.
	 */
	public void setPrefixForGuild(long guildId, @Nullable String prefix) {
		if (prefix == null) {
			prefixByGuild.remove(guildId);
			LOGGER.debug("Removed prefix for guild {}", guildId);
			return;
		}
		prefixByGuild.put(guildId, prefix);
		LOGGER.debug("Changed prefix for guild {}: {}", guildId, prefix);
	}
}
