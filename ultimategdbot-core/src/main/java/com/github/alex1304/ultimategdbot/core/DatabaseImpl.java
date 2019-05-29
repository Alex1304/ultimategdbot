package com.github.alex1304.ultimategdbot.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.github.alex1304.ultimategdbot.api.Database;
import com.github.alex1304.ultimategdbot.api.DatabaseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

class DatabaseImpl implements Database {
	
	private SessionFactory sessionFactory = null;
	private final Set<String> resourceNames = new HashSet<>();
	private final Scheduler databaseScheduler = Schedulers.newElastic("database-elastic");

	@Override
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

	public void addAllMappingResources(Set<String> resourceNames) {
		this.resourceNames.addAll(Objects.requireNonNull(resourceNames));
	}

	@Override
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

	@Override
	public <T, K extends Serializable> Mono<T> findByIDOrCreate(Class<T> entityClass, K key, BiConsumer<? super T, K> keySetter) {
		Objects.requireNonNull(entityClass);
		Objects.requireNonNull(key);
		Objects.requireNonNull(keySetter);
		return findByID(entityClass, key).switchIfEmpty(Mono.fromCallable(() -> {
			T result = entityClass.getConstructor().newInstance();
			keySetter.accept(result, key);
			return result;
		}).subscribeOn(databaseScheduler)
				.onErrorMap(DatabaseException::new));
	}

	@Override
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

	@Override
	public Mono<Void> save(Object obj) {
		return performEmptyTransaction(session -> session.saveOrUpdate(obj));
	}

	@Override
	public Mono<Void> delete(Object obj) {
		return performEmptyTransaction(session -> session.delete(obj));
	}

	@Override
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
	
	@Override
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
	
	@Override
	public <V> Mono<V> performTransactionWhen(Function<Session, Mono<V>> txAsyncFunction) {
		return Mono.usingWhen(
						Mono.fromCallable(this::newSession).doOnNext(Session::beginTransaction),
						txAsyncFunction,
						this::commitAndClose,
						this::rollbackAndClose,
						this::rollbackAndClose)
				.subscribeOn(databaseScheduler);
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
