package com.github.alex1304.ultimategdbot.utils;

import java.util.Properties;
import java.util.Random;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.github.alex1304.ultimategdbot.core.Main;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

/**
 * Utilitary methods for the bot
 *
 * @author Alex1304
 */
public class BotUtils {

	/**
	 * Builds the client using the ClientBuilder object
	 * 
	 * @param token
	 *            - the authentication token of the bot
	 * @return IDiscordClient
	 * @throws DiscordException
	 *             if building process fails
	 */
	public static IDiscordClient createClient(String token) throws DiscordException {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		clientBuilder.withRecommendedShardCount();
		return clientBuilder.build();
	}

	/**
	 * Loads the bot properties from the ultimategdbot.properties file
	 * 
	 * @return Properties
	 * @throws Exception
	 *             if something goes wrong when loading the file, or if some
	 *             properties are missing
	 */
	public static Properties loadBotProperties() throws Exception {
		Properties botProps = new Properties();
		botProps.load(Main.class.getResourceAsStream("/ultimategdbot.properties"));

		if (botProps.values().stream().anyMatch(x -> x.toString().isEmpty())) {
			throw new Exception("Some properties are missing."
					+ "Make sure you defined all of them in ultimategdbot.properties");
		}

		return botProps;
	}
	
	/**
	 * Initializes the database and builds the session factory
	 * 
	 * @return SessionFactory
	 */
	public static SessionFactory initDatabase() {
		Configuration config = new Configuration();
		config.addClass(GuildSettings.class);
		config.addClass(GlobalSettings.class);
		return config.buildSessionFactory();
	}
	
	/**
	 * Initializes the global settings by fetching database
	 * 
	 * @return GlobalSettings
	 */
	public static GlobalSettings initGlobalSettings() {
		Session s = Main.getDbSessionFactory().openSession();
		GlobalSettings g = null;
		
		try {
			g = s.load(GlobalSettings.class, 1);
		} finally {
			s.close();
		}
		
		return g;
	}
	
	/**
	 * Sends a log entry in the debug channel
	 * 
	 * @param text - the log content
	 */
	public static void log(String text) {
		RequestBuffer.request(() -> {
			try {
				Main.getChannelDebugLogs().sendMessage(text);
			} catch (DiscordException e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * Gets the settings stored for the given guild. If no settings are stored,
	 * an empty instance will be saved and returned
	 * 
	 * @param guild - the guild to fetch settings for
	 * @return GuildSettings
	 */
	public static GuildSettings getSettingsForGuild(IGuild guild) {
		Session s = Main.getDbSessionFactory().openSession();
		GuildSettings gs = null;
		
		try {
			gs = s.load(GuildSettings.class, guild.getLongID());
			if (gs == null) {
				gs = new GuildSettings();
				gs.setGuildID(guild.getLongID());
				s.save(gs);
				s.flush();
			}
		} finally {
			s.close();
		}
		
		return gs;
	}
	
	/**
	 * Gets the channel of a guild by the given String.
	 * 
	 * @param str
	 *            - The desired channel to look for. It can either be the name,
	 *            the ID or the mention of it.
	 * @param guild
	 *            - The guild in which the desired channel is supposed to be
	 * @return The desired channel, or null if the channel could not be found
	 */
	public static IChannel stringToChannel(String str, IGuild guild) {
		long channelID;

		try {
			channelID = Long.parseLong(str);
		} catch (NumberFormatException e) {
			try {
				channelID = Long.parseLong(str.substring(2, str.length() - 1));
			} catch (NumberFormatException | IndexOutOfBoundsException e2) {
				try {
					channelID = guild.getChannelsByName(str).get(0).getLongID();
				} catch (IndexOutOfBoundsException e3) {
					return null;
				}
			}
		}
		return guild.getChannelByID(channelID);
	}

	/**
	 * Gets the role of a guild by the given String.
	 * 
	 * @param str
	 *            - The desired role to look for. It can either be the name, the
	 *            ID or the mention of it.
	 * @param guild
	 *            - The guild in which the desired role is supposed to be
	 * @return The desired role, or null if the role could not be found
	 */
	public static IRole stringToRole(String str, IGuild guild) {
		long roleID;

		try {
			roleID = Long.parseLong(str);
		} catch (NumberFormatException e) {
			try {
				roleID = Long.parseLong(str.substring(3, str.length() - 1));
			} catch (NumberFormatException e2) {
				try {
					roleID = guild.getRolesByName(str).get(0).getLongID();
				} catch (IndexOutOfBoundsException e3) {
					return null;
				}
			}
		}
		return guild.getRoleByID(roleID);
	}
	
	/**
	 * Generates a random String made of alphanumeric characters.
	 * The length of the generated String is specified as an argument.
	 * @param n - the length of the generated String
	 * @return the generated random String
	 */
	public static String generateAlphanumericToken(int n) {
		if (n < 1)
			return null;
		
		final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		char[] result = new char[n];
		
		for (int i = 0 ; i < result.length ; i++)
			result[i] = alphabet.charAt(new Random().nextInt(alphabet.length()));
		
		return new String(result);
	}
	
	/**
	 * Formats the username of the user specified as argument with the format username#discriminator
	 * @param user - The user whom username will be formatted
	 * @return The formatted username as String.
	 */
	public static String formatDiscordUsername(IUser user) {
		return user.getName() + "#" + user.getDiscriminator();
	}

}
