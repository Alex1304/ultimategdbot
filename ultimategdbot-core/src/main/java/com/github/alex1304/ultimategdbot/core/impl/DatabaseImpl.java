package com.github.alex1304.ultimategdbot.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.github.alex1304.ultimategdbot.api.Database;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DatabaseImpl implements Database {
	
	private final Properties props;
	private SessionFactory sessionFactory = null;
	private final Set<String> resourceNames;
	
	public DatabaseImpl(Properties props) {
		this.props = Objects.requireNonNull(props);
		this.resourceNames = new HashSet<>();
	}

	@Override
	public void configure() {
		var config = new Configuration();
		config.addProperties(props);
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
		return Mono.fromCallable(() -> {
			try (var s = newSession()) {
				return s.get(entityClass, key);
			}
		}).subscribeOn(Schedulers.elastic());
	}

	@Override
	public <T, K extends Serializable> Mono<T> findByIDOrCreate(Class<T> entityClass, K key, BiConsumer<? super T, K> keySetter) {
		return findByID(entityClass, key).switchIfEmpty(Mono.fromCallable(() -> {
			T result = entityClass.getConstructor().newInstance();
			keySetter.accept(result, key);
			save(result).block();
			return result;
		}).subscribeOn(Schedulers.elastic()).onErrorMap(e -> new RuntimeException("An error occured when creating a database entity", e)));
	}

	@Override
	public <T> Flux<T> query(Class<T> entityClass, String query, Object... params) {
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
		}).subscribeOn(Schedulers.elastic()).flatMapMany(Flux::fromIterable);
	}

	@Override
	public Mono<Void> save(Object obj) {
		return performTransaction(session -> session.saveOrUpdate(obj), false);
	}

	@Override
	public Mono<Void> delete(Object obj) {
		return performTransaction(session -> session.delete(obj), true);
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private Session newSession() {
		if (sessionFactory == null || sessionFactory.isClosed())
			throw new IllegalStateException("Database not configured");

		return sessionFactory.openSession();
	}

	private Mono<Void> performTransaction(Consumer<Session> txConsumer, boolean flush) {
		return Mono.<Void>fromCallable(() -> {
			Transaction tx = null;
			try (var s = newSession()) {
				tx = s.beginTransaction();
				txConsumer.accept(s);
				if (flush)
					s.flush();
				tx.commit();
			} catch (RuntimeException e) {
				if (tx != null)
					tx.rollback();
				throw e;
			}
			return null;
		}).subscribeOn(Schedulers.elastic()).onErrorMap(e -> new RuntimeException("Error while performing database transaction", e));
	}
}
