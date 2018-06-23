package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;
import java.util.function.Function;

import org.hibernate.SessionFactory;

import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

/**
 * Entry point of the program
 *
 * @author Alex1304
 */
public class Main {
	
	// Global props
	
	private static IDiscordClient client;
	private static Properties botProps;
	private static GlobalSettings globalSettings;
	
	// Context props
	
	private static IGuild officialGuild = null;
	private static IUser owner = null;
	private static IRole moderatorRole = null;
	private static IRole betatesterRole = null;
	private static IChannel channelDebugLogs = null;
	
	// Database session factory
	
	private static SessionFactory dbSessionFactory;

	public static void main(String[] args) throws Exception {
		botProps = BotUtils.loadBotProperties();
		client = BotUtils.createClient(botProps.getProperty("ultimategdbot.client.token"));
		dbSessionFactory = BotUtils.initDatabase();
		globalSettings = BotUtils.initGlobalSettings();
		
		if (botProps == null || client == null || dbSessionFactory == null || globalSettings == null) {
			System.err.println("Something went wrong when initializing the bot.");
			System.err.println("botProps = " + botProps.toString());
			System.err.println("dbSessionFactory = " + dbSessionFactory.toString());
			System.err.println("globalSettings = " + globalSettings.toString());
			System.err.println("client = " + client.toString());
			System.exit(1);
		}
		
		client.getDispatcher().registerListener(new DiscordEvents());
		
		client.login();
	}
	
	/**
	 * Fetches context props from Discord. This should be called after the Ready
	 * event is received from Discord
	 * 
	 * @throws Exception
	 *             if something goes wrong when fetching props
	 */
	public static void resolveContextProps() throws Exception {
		Function<String, Exception> generateException = prop -> new Exception(
				"Unable to fetch Discord instance for property " + prop);
		
		
		officialGuild = client.getGuildByID(Long.parseLong(
				botProps.getProperty("ultimategdbot.hierarchy.official_guild_id")));
		
		if (officialGuild == null)
			throw generateException.apply("officialGuild");
		
		owner = client.fetchUser(Long.parseLong(
				botProps.getProperty("ultimategdbot.hierarchy.owner_id")));
		
		if (owner == null)
			throw generateException.apply("owner");
		
		moderatorRole = client.getRoleByID(Long.parseLong(
				botProps.getProperty("ultimategdbot.hierarchy.moderator_role_id")));

		if (moderatorRole == null)
			throw generateException.apply("moderatorRole");
		
		betatesterRole = client.getRoleByID(Long.parseLong(
				botProps.getProperty("ultimategdbot.hierarchy.betatester_role_id")));

		if (betatesterRole == null)
			throw generateException.apply("betatesterRole");
		
		channelDebugLogs = client.getChannelByID(globalSettings.getChannelDebugLogs());
		
		if (channelDebugLogs == null)
			throw generateException.apply("channelDebugLogs");
		
		System.out.println("Loaded context props:");
		System.out.println("officialGuild: " + officialGuild.getName());
		System.out.println("owner: " + BotUtils.formatDiscordUsername(owner));
		System.out.println("moderatorRole: " + moderatorRole.getName());
		System.out.println("betatesterRole: " + betatesterRole.getName());
		System.out.println("channelDebugLogs: " + channelDebugLogs.getName());
	}

	/**
	 * Gets the client
	 *
	 * @return IDiscordClient
	 */
	public static IDiscordClient getClient() {
		return client;
	}

	/**
	 * Gets the botProps
	 *
	 * @return Properties
	 */
	public static Properties getBotProps() {
		return botProps;
	}

	/**
	 * Gets the officialGuild
	 *
	 * @return IGuild
	 */
	public static IGuild getOfficialGuild() {
		return officialGuild;
	}

	/**
	 * Gets the owner
	 *
	 * @return IUser
	 */
	public static IUser getOwner() {
		return owner;
	}

	/**
	 * Gets the moderatorRole
	 *
	 * @return IRole
	 */
	public static IRole getModeratorRole() {
		return moderatorRole;
	}

	/**
	 * Gets the betatesterRole
	 *
	 * @return IRole
	 */
	public static IRole getBetatesterRole() {
		return betatesterRole;
	}

	/**
	 * Gets the dbSessionFactory
	 *
	 * @return SessionFactory
	 */
	public static SessionFactory getDbSessionFactory() {
		return dbSessionFactory;
	}

	/**
	 * Gets the channelDebugLogs
	 *
	 * @return IChannel
	 */
	public static IChannel getChannelDebugLogs() {
		return channelDebugLogs;
	}

}
