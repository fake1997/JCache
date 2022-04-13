package com.cache.basecache;

public class Cache<K,V> implements IBaseCache<K,V> {
    private LRUCacheSegment<K,V>[] cacheSegments;

    public Cache(final int maxCapacity){
        int cores = Runtime.getRuntime().availableProcessors();
        int concurrency = Math.max(cores, 2);
        cacheSegments = new LRUCacheSegment[concurrency];
        int segmentCapacity = maxCapacity / concurrency;
        if(maxCapacity % concurrency != 0){
            segmentCapacity++;
        }
        for(int i = 0; i < concurrency; i++){
            cacheSegments[i] = new LRUCacheSegment<>(segmentCapacity);
        }
    }

    public Cache(final int concurrency, final int maxCapacity){
        int segmentCapacity = maxCapacity / concurrency;
        if(maxCapacity % concurrency != 0){
            segmentCapacity++;
        }
        for(int i = 0; i < concurrency; i++){
            cacheSegments[i] = new LRUCacheSegment<>(segmentCapacity);
        }
    }

    private int segmentIndex(K key){
        int hashCode = Math.abs(key.hashCode() * 31);
        return hashCode % cacheSegments.length;
    }

    @Override
    public V get(K key) {
        int index = segmentIndex(key);
        return cacheSegments[index].get(key);
    }

    @Override
    public void put(K key, V value) {
        int index= segmentIndex(key);
        cacheSegments[index].put(key, value);
    }

    @Override
    public boolean contains(K key) {
        int index = segmentIndex(key);
        return cacheSegments[index].contains(key);
    }

    @Override
    public int size() {
        int size = 0;
        for(IBaseCache baseCache : cacheSegments){
            size += baseCache.size();
        }
        return size;
    }
}
