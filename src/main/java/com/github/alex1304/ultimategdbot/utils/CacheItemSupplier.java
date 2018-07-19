package com.github.alex1304.ultimategdbot.utils;

import java.util.function.Supplier;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

/**
 * Similar to {@link Supplier} but throws any exceptions
 * 
 * @param <T> - Object type the supplier is supposed to return
 *
 * @author Alex1304
 */
@FunctionalInterface
public interface CacheItemSupplier<T> {

	/**
	 * Executes the suplier and returns the object if successful, throws an exception otherwise
	 * 
	 * @return T
	 * @throws Exception
	 */
	public T get() throws Exception;
	
	/**
	 * Executes the suplier and returns the object if successful, returns null otherwise (no exception is thrown)
	 * 
	 * @return T
	 */
	default T getIgnoreExceptions() {
		try {
			return get();
		} catch (Exception e) {
			UltimateGDBot.logException(e);
			return null;
		}
	}
	
}
