package com.github.alex1304.ultimategdbot.core;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.cache.Cache;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.SnowflakeType;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

/**
 * Represents the bot itself. Encapsulates the client and provides other useful methods.
 *
 * @author Alex1304
 */
public class UltimateGDBot {
	
	private static UltimateGDBot instance = null;
	
	private IDiscordClient client;
	private Properties props;
	private Cache cache;
	private GlobalSettings globals;
	private Map<String, Module> modules;
	private Map<String, Boolean> startedModules;

	/**
	 * Load properties and builds Discord client
	 * 
	 * @throws Exception if something goes wrong in building process
	 */
	private UltimateGDBot() throws Exception {
		this.props = new Properties();
		props.load(Main.class.getResourceAsStream("/ultimategdbot.properties"));

		if (props.values().stream().anyMatch(x -> x.toString().isEmpty())) {
			throw new Exception("Some properties are missing."
					+ "Make sure you defined all of them in ultimategdbot.properties");
		}
		
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(props.getProperty("ultimategdbot.client.token"));
		clientBuilder.withRecommendedShardCount();
		this.client = clientBuilder.build();
		this.cache = new Cache();
		this.globals = BotUtils.initGlobalSettings();
		this.modules = new HashMap<>();
		this.startedModules = new HashMap<>();
	}
	
	/**
	 * Initializes the instance of the bot
	 * 
	 * @throws Exception propagates any exception from the bot's constructor
	 */
	public static void init() throws Exception {
		instance = new UltimateGDBot();
	}
	
	/**
	 * Gets the instance of the bot
	 * 
	 * @return UltimateGDBot
	 * @throws IllegalStateException if the bot instance hasn't been initialized
	 */
	private static UltimateGDBot instance() {
		if (instance == null)
			throw new IllegalStateException("Bot not initialized");
		return instance;
	}

	public static IDiscordClient client() {
		return instance().client;
	}
	
	public static String property(String prop) {
		return instance().props.getProperty(prop);
	}
	
	public static Cache cache() {
		return instance().cache;
	}
	
	public static IGuild officialGuild() {
		return (IGuild) BotUtils.resolveSnowflakeString(SnowflakeType.GUILD, property("ultimategdbot.hierarchy.official_guild_id"));
	}
	
	public static IUser owner() {
		return (IUser) BotUtils.resolveSnowflakeString(SnowflakeType.USER, property("ultimategdbot.hierarchy.owner_id"));
	}
	
	public static IRole moderatorRole() {
		return (IRole) BotUtils.resolveSnowflakeString(SnowflakeType.ROLE, property("ultimategdbot.hierarchy.moderator_role_id"));
	}
	
	public static IChannel channelDebugLogs() {
		return (IChannel) BotUtils.resolveSnowflake(SnowflakeType.CHANNEL, instance().globals.getChannelDebugLogs());
	}
	
	private static void log(String text) {
		RequestBuffer.request(() -> {
			channelDebugLogs().sendMessage(text);
		});
	}
	
	public static void log(String tag, String text) {
		String s = "[" + tag + "] " + text;
		
		log(s);
		
		@SuppressWarnings("resource")
		PrintStream logstream = tag.equalsIgnoreCase("error") ? System.err : System.out;
		
		logstream.println(s);
	}
	
	public static void logInfo(String text) {
		log("INFO", text);
	}
	
	public static void logWarning(String text) {
		log("WARNING", text);
	}
	
	public static void logSuccess(String text) {
		log("SUCCESS", text);
	}
	
	public static void logError(String text) {
		log("ERROR", text);
	}
	
	public static void addModule(String key, Module module) {
		instance().modules.put(key, module);
		instance().startedModules.put(key, false);
		instance().client.getDispatcher().registerListener(module);
	}
	
	public static void startModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			startModule(m.getKey());
	}
	
	public static void stopModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			stopModule(m.getKey());
	}
	
	public static void restartModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			restartModule(m.getKey());
	}
	
	public static void startModule(String key) {
		if (instance().modules.containsKey(key)) {
			instance().modules.get(key).start();
			instance().startedModules.put(key, true);
			logSuccess("Started module: `" + key + "`");
		}
	}
	
	public static void stopModule(String key) {
		if (instance().modules.containsKey(key)) {
			instance().modules.get(key).stop();
			instance().startedModules.put(key, false);
			logWarning("Stopped module: `" + key + "`");
		}
	}
	
	public static void restartModule(String key) {
		stopModule(key);
		startModule(key);
	}
	
	public static Module getModule(String key) throws ModuleUnavailableException {
		if (!instance().startedModules.containsKey(key) || !instance.startedModules.get(key))
			throw new ModuleUnavailableException();
		return instance().modules.get(key);
	}

	/**
	 * Gets a copy of the startedModules
	 *
	 * @return Map&lt;String,Boolean&gt;
	 */
	public static Map<String, Boolean> getStartedModules() {
		return new HashMap<>(instance().startedModules);
	}

}
