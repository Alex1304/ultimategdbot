package com.github.alex1304.ultimategdbot.api;

import java.io.Serializable;
import java.util.List;

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
	 * @param entityClass - class of the entity
	 * @param key         - the ID
	 * @param             <T> - The entity type
	 * @param             <K> - the ID type
	 * @return the entity found, or <code>null</code> if not found.
	 */
	<T, K extends Serializable> T findByID(Class<T> entityClass, K key);
	
	/**
	 * Makes a simple query to the database.
	 * 
	 * @param query  - the HQL query
	 * @param params - the query params
	 * @param        <T> - the entity type
	 * @return a list containing the results of the query
	 */
	<T> List<T> query(Class<T> entityClass, String query, Object... params);
	
	/**
	 * Saves an object in database
	 * 
	 * @param obj - the object to save
	 * @return whether it has saved without issues
	 */
	boolean save(Object obj);
	
	/**
	 * Deletes an object from database
	 * 
	 * @param obj - the object to save
	 * @return whether it has deleted without issues
	 */
	boolean delete(Object obj);
}
