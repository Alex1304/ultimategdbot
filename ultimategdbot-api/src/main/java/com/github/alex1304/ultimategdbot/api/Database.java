package com.github.alex1304.ultimategdbot.api;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.reactivestreams.Publisher;

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
	 * created.
	 * 
	 * @param entityClass class of the entity
	 * @param key         the ID
	 * @param keySetter   the biconsumer that assigns the ID to the newly created
	 *                    entity
	 * @param             <T> The entity type
	 * @param             <K> the ID type
	 * @return a Mono emitting the entity, if found
	 * 
	 * @deprecated The use of this method is not as convenient as expected when it
	 *             was first designed. It is better to call findByID and then
	 *             process the case where it's not found via a switchIfEmpty
	 *             operator.
	 */
	@Deprecated
	<T, K extends Serializable> Mono<T> findByIDOrCreate(Class<T> entityClass, K key,
			BiConsumer<? super T, K> keySetter);

	/**
	 * Makes a simple query to the database.
	 * 
	 * @param entityClass the entity type to fetch
	 * @param query       the HQL query
	 * @param params      the query params
	 * @param             <T> the entity type
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
	
	/**
	 * Allows to perform more complex actions with the database, by having full
	 * control on the current transaction. This method does not return a value. If
	 * you need to perform a transaction that returns a value upon completion, the
	 * variant {@link #performTransaction(Function)} is preferred.
	 * 
	 * @param txConsumer the transaction consuming a Session object
	 * @return a Mono completing when the transaction terminates successfully
	 */
	Mono<Void> performEmptyTransaction(Consumer<Session> txConsumer);
	
	/**
	 * Allows to perform more complex actions with the database, by having full
	 * control on the current transaction. This method can return a value upon
	 * completion. If you don't need a return value for the transaction, the variant
	 * {@link #performEmptyTransaction(Consumer)} is preferred.
	 * 
	 * @param            <V> the type of the returned value
	 * @param txFunction the transaction accepting a Session object and returning a
	 *                   value
	 * @return a Mono completing when the transaction terminates successfully and
	 *         emitting a value.
	 */
	<V> Mono<V> performTransaction(Function<Session, V> txFunction);
	
	/**
	 * Allows to manipulate a Session in an asynchronous context. The session
	 * provides a Publisher which completion indicates that the transaction can be
	 * committed and the session closed.
	 * 
	 * @param                 <V> the type of value that the transaction may produce
	 * @param txAsyncFunction a function that manipulates a Session and returns a
	 *                        Publisher completing when the transaction is ready to
	 *                        be committed
	 * @return a Flux completing when the transaction terminates successfully and
	 *         may emit values that constitute the result of the transaction
	 */
	<V> Flux<V> performTransactionWhen(Function<Session, Publisher<V>> txAsyncFunction);
}
