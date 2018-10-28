package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.core.CommandPluginLoader;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself. It's built using a properties object.
 *
 * @author Alex1304
 *
 */
public class UltimateGDBot {
	
	// Read from Properties
	private final String name;
	private final long clientID;
	private final String token;
	private final Snowflake ownerID;
	private final Snowflake officialGuildID;
	private final Snowflake moderatorRoleID;
	private final String gdUrl;
	private final long gdAccountID;
	private final String gdAccountPassword;
	private final String fullPrefix;
	private final String canonicalPrefix;
	private final String releaseChannel;
	private final Snowflake emojiGuild1;
	private final Snowflake emojiGuild2;
	
	// Internal attributes
	private final DiscordClient discordClient;
	private final CommandPluginLoader commandPluginLoader;
	
	private UltimateGDBot(String name, long clientID, String token, Snowflake ownerID, Snowflake officialGuildID,
			Snowflake moderatorRoleID, String gdUrl, long gdAccountID, String gdAccountPassword, String fullPrefix,
			String canonicalPrefix, String releaseChannel, Snowflake emojiGuild1, Snowflake emojiGuild2, DiscordClient discordClient, CommandPluginLoader commandPluginLoader) {
		this.name = name;
		this.clientID = clientID;
		this.token = token;
		this.ownerID = ownerID;
		this.officialGuildID = officialGuildID;
		this.moderatorRoleID = moderatorRoleID;
		this.gdUrl = gdUrl;
		this.gdAccountID = gdAccountID;
		this.gdAccountPassword = gdAccountPassword;
		this.fullPrefix = fullPrefix;
		this.canonicalPrefix = canonicalPrefix;
		this.releaseChannel = releaseChannel;
		this.emojiGuild1 = emojiGuild1;
		this.emojiGuild2 = emojiGuild2;
		this.discordClient = discordClient;
		this.commandPluginLoader = commandPluginLoader;
	}

	/**
	 * Gets the name
	 *
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the clientID
	 *
	 * @return long
	 */
	public long getClientID() {
		return clientID;
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
	 * Gets the owner of the bot
	 *
	 * @return Mono&lt;User&gt;
	 */
	public Mono<User> getOwner() {
		return discordClient.getUserById(ownerID);
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
	 * Gets the gdUrl
	 *
	 * @return String
	 */
	public String getGdUrl() {
		return gdUrl;
	}

	/**
	 * Gets the gdAccountID
	 *
	 * @return Snowflake
	 */
	public long getGdAccountID() {
		return gdAccountID;
	}

	/**
	 * Gets the gdAccountPassword
	 *
	 * @return String
	 */
	public String getGdAccountPassword() {
		return gdAccountPassword;
	}

	/**
	 * Gets the fullPrefix
	 *
	 * @return String
	 */
	public String getFullPrefix() {
		return fullPrefix;
	}

	/**
	 * Gets the canonicalPrefix
	 *
	 * @return String
	 */
	public String getCanonicalPrefix() {
		return canonicalPrefix;
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
	 * Gets the first emoji guild
	 *
	 * @return Mono&lt;Guild&gt;
	 */
	public Mono<Guild> getEmojiGuild1() {
		return discordClient.getGuildById(emojiGuild1);
	}

	/**
	 * Gets the second emoji guild
	 *
	 * @return Mono&lt;Guild&gt;
	 */
	public Mono<Guild> getEmojiGuild2() {
		return discordClient.getGuildById(emojiGuild2);
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
	 * Gets the commandPluginLoader
	 * 
	 * @return CommandPluginLoader
	 */
	public CommandPluginLoader getCommandPluginLoader() {
		return commandPluginLoader;
	}

	/**
	 * Creates an instance of the bot using the given properties. The Discord client is built as well
	 * 
	 * @param props - The properties of the bot to build
	 * @return UltimateGDBot
	 * @throws NullPointerException if a property is missing
	 * @throws NumberFormatException if a property isn't a number when a number is expected
	 */
	public static UltimateGDBot buildFromProperties(Properties props) {
		var name = Objects.requireNonNull(props.getProperty("ultimategdbot.name"));
		var clientID = Long.parseLong(Objects.requireNonNull(props.getProperty("ultimategdbot.client.id")));
		var token = Objects.requireNonNull(props.getProperty("ultimategdbot.client.token"));
		var ownerID = Snowflake.of(Objects.requireNonNull(props.getProperty("ultimategdbot.hierarchy.owner_id")));
		var officialGuildID = Snowflake.of(Objects.requireNonNull(props.getProperty("ultimategdbot.hierarchy.official_guild_id")));
		var moderatorRoleID = Snowflake.of(Objects.requireNonNull(props.getProperty("ultimategdbot.hierarchy.moderator_role_id")));
		var gdUrl = Objects.requireNonNull(props.getProperty("ultimategdbot.gd_client.url"));
		var gdAccountID = Long.parseLong(Objects.requireNonNull(props.getProperty("ultimategdbot.gd_client.id")));
		var gdAccountPassword = Objects.requireNonNull(props.getProperty("ultimategdbot.gd_client.password"));
		var fullPrefix = Objects.requireNonNull(props.getProperty("ultimategdbot.prefix.full"));
		var canonicalPrefix = Objects.requireNonNull(props.getProperty("ultimategdbot.prefix.canonical"));
		var releaseChannel = Objects.requireNonNull(props.getProperty("ultimategdbot.release.channel"));
		var emojiGuild1 = Snowflake.of(Objects.requireNonNull(props.getProperty("ultimategdbot.misc.emoji_guild_id.1")));
		var emojiGuild2 = Snowflake.of(Objects.requireNonNull(props.getProperty("ultimategdbot.misc.emoji_guild_id.2")));

		var builder = new DiscordClientBuilder(token);
		var commandPluginLoader = new CommandPluginLoader();
		
		var bot = new UltimateGDBot(
				name,
				clientID,
				token,
				ownerID,
				officialGuildID,
				moderatorRoleID,
				gdUrl,
				gdAccountID,
				gdAccountPassword,
				fullPrefix,
				canonicalPrefix,
				releaseChannel,
				emojiGuild1,
				emojiGuild2,
				builder.build(),
				commandPluginLoader
		);
		
		commandPluginLoader.bind(bot);
		
		return bot;
	}
}
