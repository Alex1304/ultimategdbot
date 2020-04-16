package com.github.alex1304.ultimategdbot.core.database;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.BindPojo;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigDao;

public interface GuildPrefixDao extends GuildConfigDao<GuildPrefixData> {

	@Override
	@SqlUpdate("INSERT INTO guild_prefix VALUES (?, NULL)")
	void create(long guildId);

	@Override
	@SqlUpdate("UPDATE guild_prefix SET prefix = NULL WHERE guild_id = ?")
	void reset(long guildId);

	@Override
	@SqlUpdate("UPDATE guild_prefix SET prefix = :prefix WHERE guild_id = :guildId")
	void update(@BindPojo GuildPrefixData data);

	@Override
	@SqlQuery("SELECT * FROM guild_prefix WHERE guild_id = ?")
	Optional<GuildPrefixData> get(long guildId);
	
	@SqlQuery("SELECT DISTINCT * FROM guild_prefix WHERE prefix IS NOT NULL AND prefix != '' AND prefix != ?")
	List<GuildPrefixData> getAllNonDefault(String defaultPrefix);
}
