package com.github.alex1304.ultimategdbot.core;

import java.io.Serializable;

import discord4j.core.shard.ShardAwareStore;
import discord4j.core.shard.ShardingStoreRegistry;
import discord4j.store.api.Store;
import discord4j.store.api.primitive.ForwardingStore;
import discord4j.store.api.primitive.LongObjStore;
import discord4j.store.api.service.StoreService;
import discord4j.store.api.util.StoreContext;
import reactor.core.publisher.Mono;

/**
 * Factory that delegates the creation of the store to a backing factory and then wraps it into a
 * {@link ShardAwareStore}.
 */
public class ShardAwareStoreService implements StoreService {

    private final ShardingStoreRegistry registry;
    private final StoreService backingStoreService;

    volatile Class<?> messageClass;
    volatile int shardId;

    public ShardAwareStoreService(ShardingStoreRegistry registry, StoreService backingStoreService) {
        this.registry = registry;
        this.backingStoreService = backingStoreService;
    }

    @Override
    public boolean hasGenericStores() {
        return backingStoreService.hasGenericStores();
    }

    @Override
    public <K extends Comparable<K>, V extends Serializable> Store<K, V> provideGenericStore(Class<K> keyClass,
            Class<V> valueClass) {
        if (!registry.containsStore(valueClass)) {
            registry.putStore(valueClass, backingStoreService.provideGenericStore(keyClass, valueClass));
        }
        return new ShardAwareStore<>(registry.getValueStore(keyClass, valueClass), registry.getKeyStore(valueClass, shardId));
    }

    @Override
    public boolean hasLongObjStores() {
        return true;
    }

    @Override
    public <V extends Serializable> LongObjStore<V> provideLongObjStore(Class<V> valueClass) {
        return new ForwardingStore<>(provideGenericStore(Long.class, valueClass));
    }

    @Override
    public void init(StoreContext context) {
        backingStoreService.init(context);
        messageClass = context.getMessageClass();
        shardId = context.getShard();
    }

    @Override
    public Mono<Void> dispose() {
        return backingStoreService.dispose();
    }
}