package com.github.alex1304.ultimategdbot.utils;

import java.io.Serializable;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;

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
	 * @param <R>
	 *            - the ID type
	 * @return T
	 * 
	 */
	public static <T, K extends Serializable> T findByID(Class<T> entityClass, K key) {
		return (T) UltimateGDBot.cache().readAndWriteIfNotExistsIgnoreExceptions("database." + entityClass.getName() + "." + String.valueOf(key), () -> {
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
		});
	}

}
