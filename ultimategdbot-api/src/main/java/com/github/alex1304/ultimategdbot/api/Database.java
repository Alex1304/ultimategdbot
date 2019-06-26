package com.github.alex1304.ultimategdbot.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Manages interactions with the database.
 */
public class Database {
	
	private SessionFactory sessionFactory = null;
	private final Set<String> resourceNames = new HashSet<>();
	private final Scheduler databaseScheduler = Schedulers.newElastic("database-elastic");

	/**
	 * Initializes the database.
	 */
	public void configure() {
		var config = new Configuration();
		for (var resource : resourceNames) {
			config.addResource(resource);
		}
		if (sessionFactory != null) {
			sessionFactory.close();
		}
		sessionFactory = config.buildSessionFactory();
	}

	/**
	 * Allows to find a database entity by its ID.
	 * 
	 * @param entityClass class of the entity
	 * @param key         the ID
	 * @param             <T> The entity type
	 * @param             <K> the ID type
	 * @return a Mono emitting the entity, if found
	 */
	public <T, K extends Serializable> Mono<T> findByID(Class<T> entityClass, K key) {
		Objects.requireNonNull(entityClass);
		Objects.requireNonNull(key);
		return Mono.fromCallable(() -> {
			try (var s = newSession()) {
				return s.get(entityClass, key);
			}
		}).subscribeOn(databaseScheduler)
				.onErrorMap(DatabaseException::new);
	}

	/**
	 * Makes a simple query to the database.
	 * 
	 * @param entityClass the entity type to fetch
	 * @param query       the HQL query
	 * @param params      the query params
	 * @param             <T> the entity type
	 * @return a Flux emitting the results of the query
	 */
	public <T> Flux<T> query(Class<T> entityClass, String query, Object... params) {
		Objects.requireNonNull(entityClass);
		Objects.requireNonNull(query);
		Objects.requireNonNull(params);
		return Mono.fromCallable(() -> {
			var list = new ArrayList<T>();
			try (var s = newSession()) {
				var q = s.createQuery(query, entityClass);
				for (int i = 0; i < params.length; i++) {
					q.setParameter(i, params[i]);
				}
				list.addAll(q.getResultList());
			}
			return list;
		}).subscribeOn(databaseScheduler)
				.flatMapMany(Flux::fromIterable)
				.onErrorMap(DatabaseException::new);
	}

	/**
	 * Saves an object in database
	 * 
	 * @param obj the object to save
	 * @return a Mono that completes when it has saved
	 */
	public Mono<Void> save(Object obj) {
		return performEmptyTransaction(session -> session.saveOrUpdate(obj));
	}

	/**
	 * Deletes an object from database
	 * 
	 * @param obj the object to save
	 * @return a Mono that completes when it has deleted
	 */
	public Mono<Void> delete(Object obj) {
		return performEmptyTransaction(session -> session.delete(obj));
	}

	/**
	 * Allows to perform more complex actions with the database, by having full
	 * control on the current transaction. This method does not return a value. If
	 * you need to perform a transaction that returns a value upon completion, the
	 * variant {@link #performTransaction(Function)} is preferred.
	 * 
	 * @param txConsumer the transaction consuming a Session object
	 * @return a Mono completing when the transaction terminates successfully
	 */
	public Mono<Void> performEmptyTransaction(Consumer<Session> txConsumer) {
		return Mono.<Void>fromCallable(() -> {
			Transaction tx = null;
			try (var s = newSession()) {
				tx = s.beginTransaction();
				txConsumer.accept(s);
				tx.commit();
			} catch (RuntimeException e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				throw e;
			}
			return null;
		}).subscribeOn(databaseScheduler)
				.onErrorMap(DatabaseException::new);
	}
	
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
	public <V> Mono<V> performTransaction(Function<Session, V> txFunction) {
		return Mono.fromCallable(() -> {
			V returnVal;
			Transaction tx = null;
			try (var s = newSession()) {
				tx = s.beginTransaction();
				returnVal = txFunction.apply(s);
				tx.commit();
			} catch (RuntimeException e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				throw e;
			}
			return returnVal;
		}).subscribeOn(databaseScheduler)
				.onErrorMap(DatabaseException::new);
	}

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
	public <V> Flux<V> performTransactionWhen(Function<Session, Publisher<V>> txAsyncFunction) {
		return Flux.usingWhen(
						Mono.fromCallable(this::newSession).doOnNext(Session::beginTransaction),
						txAsyncFunction,
						this::commitAndClose,
						this::rollbackAndClose,
						this::rollbackAndClose)
				.subscribeOn(databaseScheduler);
	}

	void addAllMappingResources(Set<String> resourceNames) {
		this.resourceNames.addAll(Objects.requireNonNull(resourceNames));
	}
	
	private Mono<Void> commitAndClose(Session session) {
		return Mono.<Void>fromRunnable(() -> {
			var tx = session.getTransaction();
			if (tx != null && tx.isActive()) {
				try {
					tx.commit();
				} finally {
					session.close();
				}
			}
		}).onErrorMap(DatabaseException::new);
	}
	
	private Mono<Void> rollbackAndClose(Session session) {
		return Mono.<Void>fromRunnable(() -> {
			var tx = session.getTransaction();
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} finally {
					session.close();
				}
			}
		}).onErrorMap(DatabaseException::new);
	}

	private Session newSession() {
		if (sessionFactory == null || sessionFactory.isClosed())
			throw new IllegalStateException("Database not configured");

		return sessionFactory.openSession();
	}
}
