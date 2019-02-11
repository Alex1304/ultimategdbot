package com.github.alex1304.ultimategdbot.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.github.alex1304.ultimategdbot.api.Database;

public class DatabaseImpl implements Database {
	
	private final Properties props;
	private SessionFactory sessionFactory = null;
	private final Set<String> resourceNames;
	
	public DatabaseImpl(Properties props) {
		this.props = Objects.requireNonNull(props);
		this.resourceNames = new HashSet<>();
	}
	
	private void addNativeResources() {
		resourceNames.add("GuildSettings.hbm.xml");
	}

	@Override
	public void configure() {
		var config = new Configuration();
		config.addProperties(props);
		addNativeResources();
		for (var resource : resourceNames) {
			config.addResource("/" + resource);
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
	public <T, K extends Serializable> T findByID(Class<T> entityClass, K key) {
		T result = null;
		Session s = newSession();

		try {
			result = s.load(entityClass, key);
		} catch (ObjectNotFoundException e) {
		} finally {
			s.close();
		}

		return result;
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, String query, Object... params) {
		Session s = newSession();
		List<T> list = new ArrayList<>();

		try {
			Query<T> q = s.createQuery(query, entityClass);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i, params[i]);
			}
			list.addAll(q.getResultList());
		} finally {
			s.close();
		}

		return list;
	}

	@Override
	public boolean save(Object obj) {
		return performTransaction(session -> session.saveOrUpdate(obj), false);
	}

	@Override
	public boolean delete(Object obj) {
		return performTransaction(session -> session.delete(obj), true);
	}

	private Session newSession() {
		if (sessionFactory == null || sessionFactory.isClosed())
			throw new IllegalStateException("Database not configured");

		return sessionFactory.openSession();
	}

	private boolean performTransaction(Consumer<Session> txConsumer, boolean flush) {
		Session s = newSession();
		Transaction tx = null;
		boolean success = false;

		try {
			tx = s.beginTransaction();
			txConsumer.accept(s);
			if (flush)
				s.flush();
			tx.commit();
			success = true;
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		} finally {
			s.close();
		}

		return success;
	}
}
