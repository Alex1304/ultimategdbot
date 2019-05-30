package com.github.alex1304.ultimategdbot.api.database;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.Session;

import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.utils.GuildSettingsValueConverter;

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
	
	public D getRaw(Session s, long guildId) {
		return valueGetter.apply(findOrCreate(s, guildId));
	}
	
	public void setRaw(Session s, D value, long guildId) {
		var entity = findOrCreate(s, guildId);
		valueSetter.accept(entity, value);
		s.saveOrUpdate(entity);
	}
	
	public Mono<String> getAsString(Session s, long guildId) {
		return Mono.fromCallable(() -> getRaw(s, guildId))
				.flatMap(raw -> valueToString.apply(raw, guildId))
				.defaultIfEmpty(GuildSettingsValueConverter.NONE_VALUE);
	}
	
	public Mono<Void> setFromString(Session s, String strValue, long guildId) {
		if (strValue == null) {
			strValue = GuildSettingsValueConverter.NONE_VALUE;
		}
		return stringToValue.apply(strValue, guildId)
				.flatMap(raw -> Mono.fromRunnable(() -> setRaw(s, raw, guildId)));
	}
	
	@Deprecated
	public Mono<D> valueFromDatabase(Database db, long guildId) {
		return db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId).map(valueGetter::apply);
	}

	@Deprecated
	public Mono<String> valueFromDatabaseAsString(Database db, long guildId) {
		return valueFromDatabase(db, guildId).flatMap(val -> valueToString.apply(val, guildId));
	}

	@Deprecated
	public Mono<Void> valueToDatabase(Database db, D value, long guildId) {
		return db.findByIDOrCreate(entityClass, guildId, GuildSettings::setGuildId)
				.doOnNext(entity -> valueSetter.accept(entity, value))
				.flatMap(db::save);
	}

	@Deprecated
	public Mono<Void> valueAsStringToDatabase(Database db, String strVal, long guildId) {
		return stringToValue.apply(strVal, guildId).flatMap(value -> valueToDatabase(db, value, guildId));
	}
	
	private E findOrCreate(Session s, long guildId) {
		var entity = s.get(entityClass, guildId);
		if (entity == null) {
			try {
				entity = entityClass.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Unable to create entity using reflection", e);
			}
		}
		return entity;
	}
}
