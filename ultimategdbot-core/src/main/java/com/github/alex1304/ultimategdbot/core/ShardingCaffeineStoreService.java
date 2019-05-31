package com.github.alex1304.ultimategdbot.core;

import java.io.Serializable;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;

import discord4j.core.shard.ShardAwareStore;
import discord4j.core.shard.ShardingStoreRegistry;
import discord4j.store.api.Store;
import discord4j.store.api.util.StoreContext;
import discord4j.store.caffeine.CaffeineStoreService;

public class ShardingCaffeineStoreService extends CaffeineStoreService {

	private final ShardingStoreRegistry registry;

	volatile Class<?> messageClass;
	volatile int shardId;

	public ShardingCaffeineStoreService(ShardingStoreRegistry registry) {
		this.registry = registry;
	}

	public ShardingCaffeineStoreService(ShardingStoreRegistry registry, Function<Caffeine<Object, Object>, Caffeine<Object, Object>> mapper) {
		super(mapper);
		this.registry = registry;
	}

	@Override
	public <K extends Comparable<K>, V extends Serializable> Store<K, V> provideGenericStore(Class<K> keyClass,
			Class<V> valueClass) {
		if (!registry.containsStore(valueClass)) {
			registry.putStore(valueClass, super.provideGenericStore(keyClass, valueClass));
		}
		return new ShardAwareStore<>(registry.getValueStore(keyClass, valueClass), registry.getKeyStore(valueClass, shardId));
	}

	@Override
	public void init(StoreContext context) {
		messageClass = context.getMessageClass();
		shardId = context.getShard();
	}
}
