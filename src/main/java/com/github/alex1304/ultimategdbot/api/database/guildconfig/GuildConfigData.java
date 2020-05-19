package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.rest.util.Snowflake;

/**
 * Database entity representing settings for a guild.
 */
public interface GuildConfigData<D extends GuildConfigData<D>> {

	Snowflake guildId();
	
	GuildConfigurator<D> configurator(Bot bot);
}