package com.github.alex1304.ultimategdbot.api.guildsettings;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Database;

/**
 * Represents a guild configuration entry.
 *
 * @param <E> - the guild settings entity type
 * @param <D> - the database value type
 */
public class GuildSettingsEntry<E extends GuildSettings, D> {
	
	private final Class<E> entityClass;
	private final Function<E, D> valueGetter;
	private final BiConsumer<E, D> valueSetter;
	private final Function<String, D> stringToValue;
	private final Function<D, String> valueToString;
	
	public GuildSettingsEntry(Class<E> entityClass, Function<E, D> valueGetter,
			BiConsumer<E, D> valueSetter, Function<String, D> stringToValue, Function<D, String> valueToString) {
		this.entityClass = Objects.requireNonNull(entityClass);
		this.valueGetter = Objects.requireNonNull(valueGetter);
		this.valueSetter = Objects.requireNonNull(valueSetter);
		this.stringToValue = Objects.requireNonNull(stringToValue);
		this.valueToString = Objects.requireNonNull(valueToString);
	}

	public Class<E> getEntityClass() {
		return entityClass;
	}
	
	public D valueFromDatabase(Database db, long guildId) {
		var entity = db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId);
		return valueGetter.apply(entity);
	}
	
	public String valueFromDatabaseAsString(Database db, long guildId) {
		var val = valueFromDatabase(db, guildId);
		return valueToString.apply(val);
	}
	
	public void valueToDatabase(Database db, D value, long guildId) {
		var entity = db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId);
		valueSetter.accept(entity, value);
		db.save(value);
	}
	
	public void valueAsStringToDatabase(Database db, String strVal, long guildId) {
		valueToDatabase(db, stringToValue.apply(strVal), guildId);
	}
}
