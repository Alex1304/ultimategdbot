package com.github.alex1304.ultimategdbot.api.guildconfig;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.rest.util.Snowflake;

/**
 * Database entity representing settings for a guild.
 */
public interface GuildConfigData<G extends GuildConfigData<G>> {

	Snowflake getGuildId();
	
	GuildConfigurator<G> configurator(Bot bot);
}