package com.github.alex1304.ultimategdbot.api.database;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.Session;

import com.github.alex1304.ultimategdbot.api.util.DatabaseInputFunction;
import com.github.alex1304.ultimategdbot.api.util.DatabaseOutputFunction;

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
	private final DatabaseInputFunction<D> stringToValue;
	private final DatabaseOutputFunction<D> valueToString;
	
	public GuildSettingsEntry(Class<E> entityClass, Function<E, D> valueGetter, BiConsumer<E, D> valueSetter,
			DatabaseInputFunction<D> stringToValue, DatabaseOutputFunction<D> valueToString) {
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
				.defaultIfEmpty("None");
	}
	
	public Mono<Void> setFromString(Session s, String strValue, long guildId) {
		if (strValue == null) {
			strValue = "None";
		}
		return stringToValue.apply(strValue, guildId)
				.doOnNext(raw -> setRaw(s, raw, guildId))
				.switchIfEmpty(Mono.fromRunnable(() -> setRaw(s, null, guildId)))
				.then();
	}
	
	private E findOrCreate(Session s, long guildId) {
		var entity = s.get(entityClass, guildId);
		if (entity == null) {
			try {
				entity = entityClass.getConstructor().newInstance();
				entity.setGuildId(guildId);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Unable to create entity using reflection", e);
			} catch (InvocationTargetException e) {
				var cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				}
				if (cause instanceof Error) {
					throw (Error) cause;
				}
				throw new RuntimeException(cause);
			}
		}
		return entity;
	}
}
