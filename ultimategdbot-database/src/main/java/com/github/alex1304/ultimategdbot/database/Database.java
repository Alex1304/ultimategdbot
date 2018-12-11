package com.github.alex1304.ultimategdbot.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

/**
 * Utility methods for database fetching
 *
 * @author Alex1304
 */
public class Database {

	public static final String MAPPINGS_DIR = "./mappings";
	private static SessionFactory sessionFactory = null;
	private static Properties props = new Properties();

	/**
	 * Initializes the database and builds the session factory
	 */
	public static void configure() {
		var config = new Configuration();
		config.addProperties(props);
		config.addDirectory(new File(MAPPINGS_DIR));

		if (sessionFactory != null) {
			sessionFactory.close();
		}

		sessionFactory = config.buildSessionFactory();
	}

	public static void setProperties(Properties props) {
		Database.props = props;
	}

	public static void requireMappingFile(String filename) throws FileNotFoundException {
		var file = new File(MAPPINGS_DIR + "/" + filename);
		if (!file.exists()) {
			throw new FileNotFoundException("Mapping file " + filename + " could not be found.");
		}
	}

	/**
	 * Starts a new database session
	 * 
	 * @return Session
	 */
	public static Session newSession() {
		if (sessionFactory == null || sessionFactory.isClosed())
			throw new IllegalStateException("Database not configured");

		return sessionFactory.openSession();
	}

	/**
	 * Allows to find a database entity by its ID. Returns null if object is not
	 * found
	 * 
	 * @param entityClass - class of the entity
	 * @param key         - the ID
	 * @param             <T> - The entity type
	 * @param             <K> - the ID type
	 * @return T
	 * 
	 */
	public static <T, K extends Serializable> T findByID(Class<T> entityClass, K key) {
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

	/**
	 * Allows to find a database entity by its ID. If not found, a new entity with
	 * the given ID is instanciated using reflection and is returned instead. If a
	 * new entity has to be created, this method won't save it to the database. You
	 * would need to call {@link Database#save(Object)} on the object returned by
	 * this method in order to save it.
	 * 
	 * @param entityClass - class of the entity
	 * @param key         - the ID
	 * @param             <T> - The entity type
	 * @param             <K> - the ID type
	 * @return T
	 * 
	 */
	public static <T, K extends Serializable> T findByIDOrCreate(Class<T> entityClass, K key) {
		T result = null;
		Session s = newSession();

		try {
			result = s.load(entityClass, key);
		} catch (ObjectNotFoundException e) {
			try {
				result = entityClass.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e1) {
				throw new RuntimeException(e1);
			}
		} finally {
			s.close();
		}

		return result;
	}

	/**
	 * Makes a simple query and returns an list containing the results
	 * 
	 * @param query  - the HQL query
	 * @param params - the query params
	 * @param        <T> - the entity type
	 * @return List&lt;Object&gt;
	 */
	public static <T> List<T> query(Class<T> entityClass, String query, Object... params) {
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

	/**
	 * Saves an object in database
	 * 
	 * @param obj - the object to save
	 * @return boolean - whether it has saved without issues
	 */
	public static boolean save(Object obj) {
		return performTransaction(session -> session.saveOrUpdate(obj), false);
	}

	/**
	 * Deleted an object from database
	 * 
	 * @param obj - the object to save
	 * @return boolean - whether it has deleted without issues
	 */
	public static boolean delete(Object obj) {
		return performTransaction(session -> session.delete(obj), true);
	}

	/**
	 * Performs what is inside txConsumer with a transaction
	 * 
	 * @param txConsumer - what should be done with the session during the
	 *                   transaction
	 * @param flush      - whether to flush
	 * @return boolean - whether the transaction has been successful
	 */
	public static boolean performTransaction(Consumer<Session> txConsumer, boolean flush) {
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
