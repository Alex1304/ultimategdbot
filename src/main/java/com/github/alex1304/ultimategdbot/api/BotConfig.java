package com.github.alex1304.ultimategdbot.api;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.menu.PaginationControls;

import discord4j.core.object.presence.Presence;
import discord4j.discordjson.json.gateway.StatusUpdate;
import discord4j.rest.util.Snowflake;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;

/**
 * Holds the bot's configuration.
 * 
 * @see BotConfig#builder(String)
 */
public class BotConfig {
	public static final String DEFAULT_COMMAND_PREFIX = "!";
	public static final String DEFAULT_FLAG_PREFIX = "-";
	public static final StatusUpdate DEFAULT_STATUS = Presence.online();
	public static final Duration DEFAULT_INTERACTIVE_MENU_TIMEOUT = Duration.ofMinutes(10);
	public static final int DEFAULT_MESSAGE_CACHE_MAX_SIZE = 2048;
	public static final Duration DEFAULT_REST_TIMEOUT = Duration.ofSeconds(20);
	public static final int DEFAULT_REST_BUFFER_SIZE = Queues.SMALL_BUFFER_SIZE;
	
	private final String token;
	private final String commandPrefix;
	private final String flagPrefix;
	private final Snowflake debugLogChannelId; // may be null
	private final Set<Snowflake> emojiGuildIds;
	private final StatusUpdate status;
	private final PaginationControls paginationControls;
	private final Duration interactiveMenuTimeout;
	private final int messageCacheMaxSize;
	private final Duration restTimeout;
	private final int restBufferSize;
	
	private BotConfig(String token, String commandPrefix, String flagPrefix, Snowflake debugLogChannelId,
			Set<Snowflake> emojiGuildIds, StatusUpdate status, PaginationControls paginationControls,
			Duration interactiveMenuTimeout, int messageCacheMaxSize, Duration restTimeout, int restBufferSize) {
		this.token = token;
		this.commandPrefix = commandPrefix;
		this.flagPrefix = flagPrefix;
		this.debugLogChannelId = debugLogChannelId;
		this.emojiGuildIds = emojiGuildIds;
		this.status = status;
		this.paginationControls = paginationControls;
		this.interactiveMenuTimeout = interactiveMenuTimeout;
		this.messageCacheMaxSize = messageCacheMaxSize;
		this.restTimeout = restTimeout;
		this.restBufferSize = restBufferSize;
	}

	/**
	 * Gets the bot token.
	 * 
	 * @return the bot token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Gets the prefix to use for commands.
	 * 
	 * @return the command prefix
	 */
	public String getCommandPrefix() {
		return commandPrefix;
	}

	/**
	 * Gets the prefix to use for command flags.
	 * 
	 * @return the flag prefix
	 */
	public String getFlagPrefix() {
		return flagPrefix;
	}

	/**
	 * Gets the channel ID where the bot will send debug log messages, if present.
	 * 
	 * @return the debug log channel ID, if present
	 */
	public Optional<Snowflake> getDebugLogChannelId() {
		return Optional.ofNullable(debugLogChannelId);
	}

	/**
	 * Gets the guild IDs where the bot will look for emojis.
	 * 
	 * @return the emoji guild IDs
	 */
	public Set<Snowflake> getEmojiGuildIds() {
		return emojiGuildIds;
	}

	/**
	 * Gets the initial bot's status.
	 * 
	 * @return the status
	 */
	public StatusUpdate getStatus() {
		return status;
	}

	/**
	 * Gets the pagination controls to use for interactive menus.
	 * 
	 * @return the pagination controls
	 */
	public PaginationControls getPaginationControls() {
		return paginationControls;
	}

	/**
	 * Gets the timeout after which interactive menus will automatically close.
	 * 
	 * @return the interactive menu timeout
	 */
	public Duration getInteractiveMenuTimeout() {
		return interactiveMenuTimeout;
	}

	/**
	 * Gets the maximum size of the message cache.
	 * 
	 * @return the message cache max size
	 */
	public int getMessageCacheMaxSize() {
		return messageCacheMaxSize;
	}

	/**
	 * Gets the timeout to apply on each Discord REST request.
	 * 
	 * @return the REST timeout
	 */
	public Duration getRestTimeout() {
		return restTimeout;
	}

	/**
	 * Gets the buffer size for Discord REST requests.
	 * 
	 * @return the REST buffer size
	 */
	public int getRestBufferSize() {
		return restBufferSize;
	}

	/**
	 * Creates a new builder of {@link BotConfig}.
	 * 
	 * @param token the bot token
	 * @return a new builder
	 * @throws NullPointerException if token is null
	 */
	public static Builder builder(String token) {
		requireNonNull(token, "token");
		return new Builder(token);
	}

	public static class Builder {
		private String token;
		private String commandPrefix;
		private String flagPrefix;
		private Snowflake debugLogChannelId;
		private Set<Snowflake> emojiGuildIds;
		private StatusUpdate status;
		private PaginationControls paginationControls;
		private Duration interactiveMenuTimeout;
		private int messageCacheMaxSize;
		private Duration restTimeout;
		private int restBufferSize;

		private Builder(String token) {
			this.token = token;
		}

		/**
		 * Sets the prefix to use for commands.
		 * 
		 * @param commandPrefix the command prefix, or null to use
		 *                      {@link BotConfig#DEFAULT_COMMAND_PREFIX}
		 * @return this builder
		 */
		public Builder setCommandPrefix(@Nullable String commandPrefix) {
			this.commandPrefix = requireNonNullElse(commandPrefix, DEFAULT_COMMAND_PREFIX);
			return this;
		}

		/**
		 * Sets the prefix to use for command flags.
		 * 
		 * @param flagPrefix the flag prefix, or null to use
		 *                   {@link BotConfig#DEFAULT_FLAG_PREFIX}
		 * @return this builder
		 */
		public Builder setFlagPrefix(@Nullable String flagPrefix) {
			this.flagPrefix = requireNonNullElse(flagPrefix, DEFAULT_FLAG_PREFIX);
			return this;
		}

		/**
		 * Sets the channel ID where the bot will send debug log messages.
		 * 
		 * @param debugLogChannelId the debug log channel ID, may be null
		 * @return this builder
		 */
		public Builder setDebugLogChannelId(@Nullable Snowflake debugLogChannelId) {
			this.debugLogChannelId = debugLogChannelId;
			return this;
		}

		/**
		 * Sets the guild IDs where the bot will look for emojis.
		 * 
		 * @param emojiGuildIds the emoji guild IDs
		 * @return this builder
		 * @throws NullPointerException if emojiGuildIds is null
		 */
		public Builder setEmojiGuildIds(Set<Snowflake> emojiGuildIds) {
			this.emojiGuildIds = requireNonNull(emojiGuildIds, "emojiGuildIds");
			return this;
		}

		/**
		 * Sets the initial bot's status.
		 * 
		 * @param status the status, or null to use {@link BotConfig#DEFAULT_STATUS}
		 * @return this builder
		 */
		public Builder setStatus(@Nullable StatusUpdate status) {
			this.status = requireNonNullElse(status, DEFAULT_STATUS);
			return this;
		}

		/**
		 * Sets the pagination controls to use for interative menus.
		 * 
		 * @param paginationControls the pagination controls, or null to use
		 *                           {@link PaginationControls#getDefault()}
		 * @return this builder
		 */
		public Builder setPaginationControls(@Nullable PaginationControls paginationControls) {
			this.paginationControls = requireNonNullElse(paginationControls, PaginationControls.getDefault());
			return this;
		}

		/**
		 * Sets the timeout after which interactive menus will automatically close.
		 * 
		 * @param interactiveMenuTimeout the interactive menu timeout, or null to use
		 *                               {@link BotConfig#DEFAULT_INTERACTIVE_MENU_TIMEOUT}
		 * @return this builder
		 */
		public Builder setInteractiveMenuTimeout(@Nullable Duration interactiveMenuTimeout) {
			this.interactiveMenuTimeout = requireNonNullElse(interactiveMenuTimeout, DEFAULT_INTERACTIVE_MENU_TIMEOUT);
			return this;
		}

		/**
		 * Sets the maximum size of the message cache.
		 * 
		 * @param messageCacheMaxSize the message cache max size
		 * @return this builder
		 * @throws IllegalArgumentException if messageCacheMaxSize is negative
		 */
		public Builder setMessageCacheMaxSize(int messageCacheMaxSize) {
			if (messageCacheMaxSize < 0) {
				throw new IllegalArgumentException("messageCacheMaxSize < 0");
			}
			this.messageCacheMaxSize = messageCacheMaxSize;
			return this;
		}

		/**
		 * Sets the timeout to apply on each Discord REST request.
		 * 
		 * @param restTimeout the REST timeout, or null to use
		 *                    {@link BotConfig#DEFAULT_REST_TIMEOUT}
		 * @return this builder
		 */
		public Builder setRestTimeout(@Nullable Duration restTimeout) {
			this.restTimeout = requireNonNullElse(restTimeout, DEFAULT_REST_TIMEOUT);
			return this;
		}

		/**
		 * Sets the buffer size for Discord REST requests.
		 * 
		 * @param restBufferSize the REST buffer size
		 * @return this builder
		 * @throws IllegalArgumentException if restBufferSize is zero or negative
		 */
		public Builder setRestBufferSize(int restBufferSize) {
			if (messageCacheMaxSize <= 0) {
				throw new IllegalArgumentException("restBufferSize <= 0");
			}
			this.restBufferSize = restBufferSize;
			return this;
		}

		/**
		 * Builds a {@link BotConfig} with the configured values.
		 * 
		 * @return a new {@link BotConfig}
		 */
		public BotConfig build() {
			return new BotConfig(token, commandPrefix, flagPrefix, debugLogChannelId, unmodifiableSet(emojiGuildIds),
					status, paginationControls, interactiveMenuTimeout, messageCacheMaxSize, restTimeout,
					restBufferSize);
		}
	}
}
