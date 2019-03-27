package com.github.alex1304.ultimategdbot.api;

import java.io.Serializable;
import java.util.function.BiConsumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Manages interactions with the database.
 */
public interface Database {
	/**
	 * Initializes the database.
	 */
	public void configure();

	/**
	 * Allows to find a database entity by its ID.
	 * 
	 * @param entityClass class of the entity
	 * @param key         the ID
	 * @param             <T> The entity type
	 * @param             <K> the ID type
	 * @return a Mono emitting the entity, if found
	 */
	<T, K extends Serializable> Mono<T> findByID(Class<T> entityClass, K key);

	/**
	 * Allows to find a database entity by its ID. If not found, a new entity is
	 * created
	 * 
	 * @param entityClass class of the entity
	 * @param key         the ID
	 * @param keySetter   the biconsumer that assigns the ID to the newly created
	 *                    entity
	 * @param             <T> The entity type
	 * @param             <K> the ID type
	 * @return a Mono emitting the entity, if found
	 */
	<T, K extends Serializable> Mono<T> findByIDOrCreate(Class<T> entityClass, K key,
			BiConsumer<? super T, K> keySetter);

	/**
	 * Makes a simple query to the database.
	 * 
	 * @param query  the HQL query
	 * @param params the query params
	 * @param        <T> the entity type
	 * @return a Flux emitting the results of the query
	 */
	<T> Flux<T> query(Class<T> entityClass, String query, Object... params);

	/**
	 * Saves an object in database
	 * 
	 * @param obj the object to save
	 * @return a Mono that completes when it has saved
	 */
	Mono<Void> save(Object obj);

	/**
	 * Deletes an object from database
	 * 
	 * @param obj the object to save
	 * @return a Mono that completes when it has deleted
	 */
	Mono<Void> delete(Object obj);
}
