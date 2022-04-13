package com.cache.basecache;

public interface IBaseCache<K, V> {
    V get(K key);

    void put(K key, V value);

    boolean contains(K key);

    int size();
}
