package com.github.alex1304.ultimategdbot.api.database.guildconfig;

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
	 * Creates new configuration data for this guild with default values.
	 * 
	 * @param guildId the ID of the guild
	 */
	@SqlUpdate
	void create(long guildId);
	
	/**
	 * Resets data for this guild.
	 * 
	 * @param guildId the ID of the guild
	 */
	@SqlUpdate
	void reset(long guildId);
	
	/**
	 * Updates the data for a certain guild.
	 * 
	 * @param data the data for a guild
	 */
	@SqlUpdate
	void update(D data);
	
	/**
	 * Retrieves the data for a particular guild, if present.
	 * 
	 * @param guildId the ID of the guild
	 * @return the data for that guild, if present
	 */
	@SqlQuery
	Optional<D> get(long guildId);
	
	/**
	 * Retrieves the data for a particular guild. If not present, empty settings
	 * are created and returned.
	 * 
	 * @param guildId the ID of the guild
	 * @return the data for that guild
	 */
	@Transaction(TransactionIsolationLevel.SERIALIZABLE)
	default D getOrCreate(long guildId) {
		return get(guildId).orElseGet(() -> {
			create(guildId);
			return get(guildId).orElseThrow();
		});
	}
	
	/**
	 * Resets the data for a particular guild, then returns the data after reset.
	 * 
	 * @param guildId the ID of the guild
	 * @return the data for that guild after reset
	 */
	@Transaction(TransactionIsolationLevel.SERIALIZABLE)
	default D resetAndGet(long guildId) {
		reset(guildId);
		return get(guildId).orElseThrow();
	}
}
