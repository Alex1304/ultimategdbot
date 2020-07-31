package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import discord4j.common.util.Snowflake;

/**
 * Database entity representing settings for a guild.
 */
public interface GuildConfigData<D extends GuildConfigData<D>> {

	Snowflake guildId();
}