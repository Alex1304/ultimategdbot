package com.github.alex1304.ultimategdbot.utils;

import java.util.Random;

import org.hibernate.Session;

import com.github.alex1304.ultimategdbot.core.Database;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IIDLinkedObject;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

/**
 * Utilitary methods for the bot
 *
 * @author Alex1304
 */
public class BotUtils {
	
	/**
	 * Initializes the global settings by fetching database
	 * 
	 * @return GlobalSettings
	 */
	public static GlobalSettings initGlobalSettings() {
		Session s = Database.newSession();
		GlobalSettings g = null;
		
		try {
			g = s.load(GlobalSettings.class, 1);
		} finally {
			s.close();
		}
		
		return g;
	}
	
	/**
	 * Resolves a snowflake ID into a Discord object
	 * 
	 * @param type
	 *            - the type of Discord object (guild, channel, etc)
	 * @param snowflake
	 *            - the snowflake
	 * @return IIDLinkedObject
	 */
	public static IIDLinkedObject resolveSnowflake(SnowflakeType type, long snowflake) {
		return (IIDLinkedObject) UltimateGDBot.cache().readAndWriteIfNotExists("discord.snowflake." + snowflake, () -> {
			return type.getFunc().apply(snowflake);
		});
	}

	/**
	 * Resolves a snowflake ID into a Discord object. If the string isn't a
	 * valid snowflake, null is returned.
	 * 
	 * @param type
	 *            - the type of Discord object (guild, channel, etc)
	 * @param snowflake
	 *            - the snowflake as String
	 * @return IIDLinkedObject
	 */
	public static IIDLinkedObject resolveSnowflakeString(SnowflakeType type, String snowflake) {
		try {
			return resolveSnowflake(type, Long.parseLong(snowflake));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Gets the settings stored for the given guild. If no settings are stored,
	 * an empty instance will be saved and returned
	 * 
	 * @param guild - the guild to fetch settings for
	 * @return GuildSettings
	 */
	public static GuildSettings getSettingsForGuild(IGuild guild) {
		Session s = Database.newSession();
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
