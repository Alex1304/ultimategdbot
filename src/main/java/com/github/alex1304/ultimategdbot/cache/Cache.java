package com.github.alex1304.ultimategdbot.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.utils.CacheItemSupplier;

/**
 * A cache store objects for later use. Objects stored there have a limited
 * lifetime. Writing and reading cache is thread-safe.
 *
 * @author Alex1304
 */
public class Cache {

	private static final long DEFAULT_LIFETIME = 3_600_000;

	private ConcurrentHashMap<String, CacheEntry> store;

	public Cache() {
		this.store = new ConcurrentHashMap<>();
	}

	/**
	 * Writes a new entry into the cache.
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param item
	 *            - the item to write
	 * @param lifetime
	 *            - how long the object is supposed to remain in cache (in
	 *            milliseconds)
	 */
	public void write(String entryName, Object item, long lifetime) {
		if (item == null)
			store.remove(entryName);
		else
			store.put(entryName, new CacheEntry(item, System.currentTimeMillis() + lifetime));
	}

	/**
	 * Writes a new entry into the cache. A default lifetime of 1 hour will be
	 * applied.
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param item
	 *            - the item to write
	 */
	public void write(String entryName, Object item) {
		write(entryName, item, DEFAULT_LIFETIME);
	}

	/**
	 * Reads the entry with the given name, returns null if the entry doesn't
	 * exist or if the item has expired.
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @return Object
	 */
	public Object read(String entryName) {
		CacheEntry entry = store.get(entryName);
		if (entry == null)
			return null;

		if (entry.getExpiryTimestamp() < System.currentTimeMillis()) {
			store.remove(entryName);
			return null;
		}

		return entry.getItem();
	}

	/**
	 * Attempts to read an entry from the cache, if not found then a supplier
	 * will be used to write a new entry. Any exception thrown inside the supplier will be forwarded
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param itemSupplier
	 *            - Supplier that provides the new object to write in cache
	 * @param lifetime
	 *            - the lifetime applied to the new object in case a new object
	 *            should be written
	 * @return Object
	 * @throws Exception forwards any exception thrown inside the supplier
	 */
	public Object readAndWriteIfNotExists(String entryName, CacheItemSupplier<Object> itemSupplier, long lifetime) throws Exception {
		Object obj = read(entryName);

		if (obj == null) {
			obj = itemSupplier.get();
			if (obj != null)
				write(entryName, obj, lifetime);
		}

		return obj;
	}

	/**
	 * Attempts to read an entry from the cache, if not found then a supplier
	 * will be used to write a new entry. A default lifetime of 1 hour will be
	 * applied. Any exception thrown inside the supplier will be forwarded
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param itemSupplier
	 *            - Supplier that provides the new object to write in cache
	 * 
	 * @return Object
	 */
	public Object readAndWriteIfNotExists(String entryName, CacheItemSupplier<Object> itemSupplier) throws Exception {
		return readAndWriteIfNotExists(entryName, itemSupplier, DEFAULT_LIFETIME);
	}

	/**
	 * Attempts to read an entry from the cache, if not found then a supplier
	 * will be used to write a new entry. Any exception thrown inside the supplier will be ignored
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param itemSupplier
	 *            - Supplier that provides the new object to write in cache
	 * @param lifetime
	 *            - the lifetime applied to the new object in case a new object
	 *            should be written
	 * @return Object
	 * @throws Exception forwards any exception thrown inside the supplier
	 */
	public Object readAndWriteIfNotExistsIgnoreExceptions(String entryName, CacheItemSupplier<Object> itemSupplier, long lifetime) {
		Object obj = read(entryName);

		if (obj == null) {
			obj = itemSupplier.getIgnoreExceptions();
			if (obj != null)
				write(entryName, obj, lifetime);
		}

		return obj;
	}

	/**
	 * Attempts to read an entry from the cache, if not found then a supplier
	 * will be used to write a new entry. A default lifetime of 1 hour will be
	 * applied. Any exception thrown inside the supplier will be ignored
	 * 
	 * @param entryName
	 *            - the unique identifier for the entry
	 * @param itemSupplier
	 *            - Supplier that provides the new object to write in cache
	 * 
	 * @return Object
	 */
	public Object readAndWriteIfNotExistsIgnoreExceptions(String entryName, CacheItemSupplier<Object> itemSupplier) {
		return readAndWriteIfNotExistsIgnoreExceptions(entryName, itemSupplier, DEFAULT_LIFETIME);
	}

	/**
	 * Clears cache
	 */
	public void clear() {
		store.clear();
	}

}
