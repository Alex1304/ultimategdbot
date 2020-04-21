package com.github.alex1304.ultimategdbot.core.database;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.BindPojo;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigDao;

import discord4j.rest.util.Snowflake;

public interface CoreConfigDao extends GuildConfigDao<CoreConfigData> {
	String TABLE = "core_config";

	@Override
	@SqlUpdate("INSERT INTO " + TABLE + "(guild_id) VALUES (?)")
	void create(long guildId);

	@Override
	@SqlUpdate("UPDATE " + TABLE + " SET "
			+ "prefix = DEFAULT(prefix), "
			+ "channel_changelog_id = DEFAULT(channel_changelog_id) "
			+ "WHERE guild_id = ?")
	void reset(long guildId);

	@Override
	@SqlUpdate("UPDATE " + TABLE + " SET prefix = :prefix, channel_changelog_id = :channelChangelogId WHERE guild_id = :guildId")
	void update(@BindPojo CoreConfigData data);

	@Override
	@SqlQuery("SELECT * FROM " + TABLE + " WHERE guild_id = ?")
	Optional<CoreConfigData> get(long guildId);
	
	@SqlQuery("SELECT guild_id, prefix FROM " + TABLE + " WHERE prefix IS NOT NULL AND prefix != '' AND prefix != ?")
	List<CoreConfigData> getAllNonDefaultPrefixes(String defaultPrefix);
	
	@SqlQuery("SELECT channel_changelog_id FROM " + TABLE + " WHERE channel_changelog_id IS NOT NULL")
	List<Snowflake> getAllChangelogChannels();
}
