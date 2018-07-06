package com.github.alex1304.ultimategdbot.utils;

import java.io.Serializable;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.github.alex1304.ultimategdbot.core.Database;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

/**
 * Utility methods for database fetching
 *
 * @author Alex1304
 */
public class DatabaseUtils {
	
	/**
	 * Allows to find a database entity by its ID. Returns null if object is not found
	 * 
	 * @param entityClass
	 *            - class of the entity
	 * @param key
	 *            - the ID
	 * @param <T>
	 *            - The entity type
	 * @param <K>
	 *            - the ID type
	 * @return T
	 * 
	 */
	public static <T, K extends Serializable> T findByID(Class<T> entityClass, K key) {
		T result = null;
		Session s = Database.newSession();
		
		try {
			result = s.load(entityClass, key);
		} catch (ObjectNotFoundException e) {
		}
		finally {
			s.close();
		}
		
		return result;
	}
	
	/**
	 * Saves an object in database
	 * 
	 * @param obj - the object to save
	 * @return boolean - whether it has saved without issues
	 */
	public static boolean save(Object obj) {
		Session s = Database.newSession();
		Transaction tx = null;
		boolean success = false;
		
		try {
			tx = s.beginTransaction();
			s.saveOrUpdate(obj);
			tx.commit();
			success = true;
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			UltimateGDBot.logException(e);
		} finally {
			s.close();
		}
		
		return success;
	}
	
	/**
	 * Deleted an object from database
	 * 
	 * @param obj - the object to save
	 * @return boolean - whether it has deleted without issues
	 */
	public static boolean delete(Object obj) {
		Session s = Database.newSession();
		Transaction tx = null;
		boolean success = false;
		
		try {
			tx = s.beginTransaction();
			s.delete(obj);
			s.flush();
			tx.commit();
			success = true;
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			UltimateGDBot.logException(e);
		} finally {
			s.close();
		}
		
		return success;
	}

}
