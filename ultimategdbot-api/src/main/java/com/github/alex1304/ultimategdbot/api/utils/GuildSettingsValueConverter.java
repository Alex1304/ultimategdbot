package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;

public class GuildSettingsValueConverter {
	public static final String NONE_VALUE = "None";
	private final Bot bot;
	
	public GuildSettingsValueConverter(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
	}
	
	public String convertNonBlankStringToString(String str, long guildId) {
		if (str.isBlank()) {
			throw new IllegalArgumentException("Cannot be blank");
		}
		return str;
	}
	
	public String convertStringToString(String str, long guildId) {
		return str;
	}
	
	private <N extends Number> N convertStringToNumber(String str, long guildId, Function<String, N> converter) {
		return converter.apply(str);
	}
	
	public int convertStringToInt(String str, long guildId) {
		return convertStringToNumber(str, guildId, Integer::parseInt);
	}
	
	public long convertStringToLong(String str, long guildId) {
		return convertStringToNumber(str, guildId, Long::parseLong);
	}
	
	public float convertStringToFloat(String str, long guildId) {
		return convertStringToNumber(str, guildId, Float::parseFloat);
	}
	
	public double convertStringToDouble(String str, long guildId) {
		return convertStringToNumber(str, guildId, Double::parseDouble);
	}
	
	public short convertStringToShort(String str, long guildId) {
		return convertStringToNumber(str, guildId, Short::parseShort);
	}
	
	public byte convertStringToByte(String str, long guildId) {
		return convertStringToNumber(str, guildId, Byte::parseByte);
	}
	
	public String convertNumberToString(Number num, long guildId) {
		return String.valueOf(num);
	}
	
	public boolean convertStringToBoolean(String str, long guildId) {
		if (str.matches("yes|true|1|enable")) {
			return true;
		} else if (str.matches("no|false|0|disable")) {
			return false;
		} else {
			throw new IllegalArgumentException("Expected a boolean value (yes/no, true/false, 1/0 or enable/disable). Note that it's case sensitive.");
		}
	}
	
	public long convertStringToRoleID(String str, long guildId) {
		if (str.equalsIgnoreCase(NONE_VALUE)) {
			return 0;
		}
		Snowflake roleId;
		try {
			roleId = Snowflake.of(str);
		} catch (NumberFormatException e) {
			try {
				roleId = Snowflake.of(str.substring(3, str.length() - 1));
			} catch (NumberFormatException | StringIndexOutOfBoundsException e0) {
				try {
					roleId = bot.getDiscordClients().flatMap(client -> client.getGuildById(Snowflake.of(guildId)))
							.flatMap(g -> g.getRoles()
									.filter(r -> r.getName().equalsIgnoreCase(str))
									.map(Role::getId))
							.next()
							.blockOptional(Duration.ofSeconds(1)).get();
				} catch (RuntimeException e1) {
					throw new IllegalArgumentException("Could not convert '" + str + "' to a valid role");
				}
			}
		}
		return roleId.asLong();
	}
	
	public String convertRoleIDToString(long roleId, long guildId) {
		if (roleId == 0) {
			return NONE_VALUE;
		}
		String str;
		try {
			var role = bot.getDiscordClients().flatMap(client -> client.getRoleById(Snowflake.of(guildId), Snowflake.of(roleId))).next()
					.blockOptional(Duration.ofSeconds(1)).get();
			str = '@' + role.getName() + " (" + role.getId().asLong() + ")";
		} catch (RuntimeException e) {
			str = "_(unknown role of ID " + roleId + ")_";
		}
		return str;
	}
	
	private long convertStringToChannelID(String str, long guildId, Type channelType) {
		if (str.equalsIgnoreCase(NONE_VALUE)) {
			return 0;
		}
		Snowflake channelId;
		try {
			channelId = Snowflake.of(str);
		} catch (NumberFormatException e) {
			try {
				channelId = Snowflake.of(str.substring(2, str.length() - 1));
			} catch (NumberFormatException | StringIndexOutOfBoundsException e0) {
				try {
					channelId = bot.getDiscordClients().flatMap(client -> client.getGuildById(Snowflake.of(guildId)))
							.flatMap(g -> g.getChannels()
									.filter(c -> c.getType() == channelType)
									.filter(c -> c.getName().equalsIgnoreCase(str))
									.map(Channel::getId))
							.next()
							.blockOptional(Duration.ofSeconds(1)).get();
				} catch (RuntimeException e1) {
					throw new IllegalArgumentException("Could not convert '" + str + "' to a valid role");
				}
			}
		}
		return channelId.asLong();
	}
	
	public long convertStringToTextChannelID(String str, long guildId) {
		return convertStringToChannelID(str, guildId, Type.GUILD_TEXT);
	}
	
	public long convertStringToVoiceChannelID(String str, long guildId) {
		return convertStringToChannelID(str, guildId, Type.GUILD_VOICE);
	}
	
	public long convertStringToCategoryID(String str, long guildId) {
		return convertStringToChannelID(str, guildId, Type.GUILD_CATEGORY);
	}
	
	public String convertChannelIDToString(long roleId, long guildId) {
		if (roleId == 0) {
			return NONE_VALUE;
		}
		String str;
		try {
			var channel = bot.getDiscordClients().flatMap(client -> client.getChannelById(Snowflake.of(roleId)))
					.next()
					.ofType(GuildChannel.class)
					.blockOptional(Duration.ofSeconds(1)).get();
			str = channel.getType() == Type.GUILD_TEXT ? channel.getMention() : channel.getName() + " (" + channel.getId().asLong() + ")";
		} catch (RuntimeException e) {
			str = "_(unknown channel of ID " + roleId + ")_";
		}
		return str;
	}
	
}
