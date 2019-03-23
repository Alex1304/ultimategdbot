package com.github.alex1304.ultimategdbot.api.database;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Database;

import reactor.core.publisher.Mono;

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
	private final BiFunction<String, Long, Mono<D>> stringToValue;
	private final BiFunction<D, Long, Mono<String>> valueToString;
	
	public GuildSettingsEntry(Class<E> entityClass, Function<E, D> valueGetter,
			BiConsumer<E, D> valueSetter, BiFunction<String, Long, Mono<D>> stringToValue, BiFunction<D, Long, Mono<String>> valueToString) {
		this.entityClass = Objects.requireNonNull(entityClass);
		this.valueGetter = Objects.requireNonNull(valueGetter);
		this.valueSetter = Objects.requireNonNull(valueSetter);
		this.stringToValue = Objects.requireNonNull(stringToValue);
		this.valueToString = Objects.requireNonNull(valueToString);
	}

	public Class<E> getEntityClass() {
		return entityClass;
	}
	
	public Mono<D> valueFromDatabase(Database db, long guildId) {
		return db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId).map(valueGetter::apply);
	}
	
	public Mono<String> valueFromDatabaseAsString(Database db, long guildId) {
		return valueFromDatabase(db, guildId).flatMap(val -> valueToString.apply(val, guildId));
	}
	
	public Mono<Void> valueToDatabase(Database db, D value, long guildId) {
		return db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId)
				.doOnNext(entity -> valueSetter.accept(entity, value))
				.flatMap(db::save);
	}
	
	public Mono<Void> valueAsStringToDatabase(Database db, String strVal, long guildId) {
		return stringToValue.apply(strVal, guildId).flatMap(value -> valueToDatabase(db, value, guildId));
	}
}
