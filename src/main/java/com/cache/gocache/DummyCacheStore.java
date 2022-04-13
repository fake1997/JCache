package com.cache.quickcache;

import java.io.Serializable;

public class DummyCacheStore<K extends Serializable, V extends Serializable> implements CacheStore<K,V> {
    @Override
    public void store(CacheEntry<K, V> entry) {
    }

    @Override
    public CacheEntry<K, V> load(K key) {
        return null;
    }

    @Override
    public CacheEntry<K, V> remove(K key) {
        return null;
    }

    @Override
    public void close() {
    }
}
