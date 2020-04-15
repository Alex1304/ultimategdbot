package com.github.alex1304.ultimategdbot.api.guildconfig;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * Represents a guild configuration entry.
 * 
 * @param <T> the type of value supported by this entry
 */
public interface ConfigEntry<T> {
	/**
	 * Gets the unique key of this entry.
	 * 
	 * @return the key
	 */
	String getKey();

	/**
	 * Gets a user-friendly description of this entry.
	 * 
	 * @return the description
	 */
	String getDescription();
	
	/**
	 * Tells whether this entry is read-only. If this method returns true, any call
	 * to {@link #setValue(Object)} will result in a Mono emitting
	 * {@link ReadOnlyConfigEntryException}.
	 * 
	 * @return true if read-only
	 */
	boolean isReadOnly();

	/**
	 * Gets the current value of this entry.
	 * 
	 * @return the value
	 */
	Mono<T> getValue();
	
	/**
	 * Sets a new value for this entry. The value is first validated using the
	 * validator held by this entry. If validation is successful, the value is set
	 * and the returned Mono completes. If the validation fails, the value is not
	 * set, and {@link ValidationException} is emitted instead.
	 * 
	 * @param newValue the new value for this entry, may be null
	 * @return a Mono completing when value is validated and set, or
	 *         {@link ValidationException} if validation fails
	 */
	Mono<Void> setValue(@Nullable T newValue);
	
	/**
	 * Accepts a visitor to visit the concrete type of this config entry.
	 * 
	 * @param <R>     the return value type of the visitor
	 * @param visitor the visitor to accept
	 * @return a Mono emitting the result of the visit
	 */
	<R> Mono<R> accept(ConfigEntryVisitor<R> visitor);
}
