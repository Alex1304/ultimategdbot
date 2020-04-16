package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.Optional;

import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

/**
 * Generic DAO to retrieve configuration for a guild.
 *
 * @param <D> the type of object storing the configuration for a guild
 */
public interface GuildConfigDao<D extends GuildConfigData<D>> {
	/**
	 * Creates new, empty configuration data for this guild.
	 * 
	 * @param guildId the ID of the guild
	 */
	@SqlUpdate
	void create(long guildId);
	
	/**
	 * Resets settings for this guild.
	 * 
	 * @param guildId the ID of the guild
	 */
	@SqlUpdate
	void reset(long guildId);
	
	/**
	 * Updates the settings for a certain guild.
	 * 
	 * @param settings the settings for a guild
	 */
	@SqlUpdate
	void update(D data);
	
	/**
	 * Retrieves the settings for a particular guild, if present.
	 * 
	 * @param guildId the ID of the guild
	 * @return the settings for that guild, if present
	 */
	@SqlQuery
	Optional<D> get(long guildId);
	
	/**
	 * Retrieves the settings for a particular guild. If not present, empty settings
	 * are created and returned.
	 * 
	 * @param guildId the ID of the guild
	 * @return the settings for that guild
	 */
	@Transaction(TransactionIsolationLevel.SERIALIZABLE)
	default D getOrCreate(long guildId) {
		return get(guildId).orElseGet(() -> {
			create(guildId);
			return get(guildId).orElseThrow();
		});
	}
}
