package com.github.alex1304.ultimategdbot.api.guildconf;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Database;

/**
 * Represents a guild configuration entry.
 *
 * @param <E> - the guild settings entity type
 * @param <E> - the database value type
 * @param <V> - the entry value type
 */
public class GuildConfEntry<E, D> {
	
	private final Class<E> entityClass;
	private final Function<E, D> valueGetter;
	private final BiConsumer<E, D> valueSetter;
	private final Function<String, D> stringToValue;
	private final Function<D, String> valueToString;
	
	public GuildConfEntry(Class<E> entityClass, Function<E, D> valueGetter,
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

	public Function<E, D> getValueGetter() {
		return valueGetter;
	}

	public BiConsumer<E, D> getValueSetter() {
		return valueSetter;
	}

	public Function<String, D> getStringToValue() {
		return stringToValue;
	}

	public Function<D, String> getValueToString() {
		return valueToString;
	}
	
//	public D getFromDatabase(Database db) {
//		
//	}
}
