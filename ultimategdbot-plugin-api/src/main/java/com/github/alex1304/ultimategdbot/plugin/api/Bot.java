package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Objects;
import java.util.Properties;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself. It's built using a properties object.
 *
 * @author Alex1304
 *
 */
public class Bot {

	// Read from Properties
	private final String token;
	private final Snowflake officialGuildID;
	private final Snowflake moderatorRoleID;
	private final String prefix;
	private final String releaseChannel;

	// Internal attributes
	private final DiscordClient discordClient;

	private Bot(String token, Snowflake officialGuildID,
			Snowflake moderatorRoleID, String prefix, String releaseChannel,
			DiscordClient discordClient) {
		this.token = token;
		this.officialGuildID = officialGuildID;
		this.moderatorRoleID = moderatorRoleID;
		this.prefix = prefix;
		this.releaseChannel = releaseChannel;
		this.discordClient = discordClient;
	}

	/**
	 * Gets the token
	 *
	 * @return String
	 */
	public String getToken() {
		return token;
	}
	/**
	 * Gets the official guild of the bot
	 *
	 * @return Mono&lt;Guild&gt;
	 */
	public Mono<Guild> getOfficialGuild() {
		return discordClient.getGuildById(officialGuildID);
	}

	/**
	 * Gets the moderator role of the bot
	 *
	 * @return Mono&lt;Role&gt;
	 */
	public Mono<Role> getModeratorRole() {
		return discordClient.getRoleById(officialGuildID, moderatorRoleID);
	}

	/**
	 * Gets the fullPrefix
	 *
	 * @return String
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Gets the releaseChannel
	 *
	 * @return String
	 */
	public String getReleaseChannel() {
		return releaseChannel;
	}

	/**
	 * Gets the discordClient
	 *
	 * @return DiscordClient
	 */
	public DiscordClient getDiscordClient() {
		return discordClient;
	}

	/**
	 * Creates an instance of the bot using the given properties. The Discord client
	 * is built as well. The freshly built bot is returned.
	 * 
	 * @param props - The properties of the bot to build
	 * @return UltimateGDBot
	 * @throws IllegalStateException if an instance has already been created
	 * @throws NullPointerException  if a property is missing
	 * @throws NumberFormatException if a property isn't a number when a number is
	 *                               expected
	 */
	public static Bot buildFromProperties(Properties props) {
		var token = Objects.requireNonNull(props.getProperty("token"));
		var officialGuildID = Snowflake.of(Objects.requireNonNull(props.getProperty("official_guild_id")));
		var moderatorRoleID = Snowflake.of(Objects.requireNonNull(props.getProperty("moderator_role_id")));
		var prefix = Objects.requireNonNull(props.getProperty("prefix"));
		var releaseChannel = Objects.requireNonNull(props.getProperty("release_channel"));

		var builder = new DiscordClientBuilder(token);
		
		return new Bot(
				token,
				officialGuildID,
				moderatorRoleID,
				prefix,
				releaseChannel,
				builder.build()
		);
	}
}
