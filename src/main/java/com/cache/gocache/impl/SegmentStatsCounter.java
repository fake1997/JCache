package com.cache.quickcache.impl;

import com.cache.quickcache.CacheStats;

public class SegmentStatsCounter {
    volatile long hitCount;
    volatile long missCount;

    volatile long memoryHitCount = 0L;
    volatile long memoryMissCount = 0L;

    volatile long createCount = 0L;
    volatile long updateCount = 0L;
    volatile long removeCount = 0L;

    volatile long evictCount = 0L;
    volatile long expireCount = 0L;

    volatile long evictStoreHitCount = 0L;
    volatile long evictStoreMissCount = 0L;

    volatile long behindStoreHitCount = 0L;
    volatile long behindStoreMissCount = 0L;

    volatile long size = 0L;
    volatile long memorySize = 0L;

    volatile long evictStoreExceptionCount = 0L;
    volatile long behindStoreExceptionCount = 0L;

    public void hits(int count) {
        hitCount += count;
    }

    public void misses(int count) {
        missCount += count;
    }

    public void memoryHits(int count) {
        memoryHitCount += count;
    }

    public void memoryMisses(int count) {
        memoryMissCount += count;
    }

    public void recordCreates(int count) {
        createCount += count;
    }

    public void recordUpdates(int count) {
        updateCount += count;
    }

    public void recordRemoves(int count) {
        removeCount+=count;
    }

    public void recordEvicts(int count) {
        evictCount += count;
    }

    public void recordExpires(int count) {
        expireCount+=count;
    }

    public void evictStoreHits(int count) {
        evictStoreHitCount+=count;
    }

    public void evictStoreMisses(int count) {
        evictStoreMissCount+=count;
    }

    public void behindStoreHits(int count) {
        behindStoreHitCount+=count;
    }

    public void behindStoreMisses(int count) {
        behindStoreMissCount+=count;
    }

    public void sizeIncrement() {
        size++;
    }

    public void sizeDecrement() {
        size--;
    }

    public void memorySizeIncrement() {
        memorySize++;
    }

    public void memorySizeDecrement() {
        memorySize--;
    }

    public void evictStoreException(int count){
        evictStoreExceptionCount += count;
    }

    public void behindStoreException(int count){
        behindStoreExceptionCount += count;
    }

    public CacheStats snapshot() {
        return new CacheStats(
                hitCount,
                missCount,
                memoryHitCount,
                memoryMissCount,
                createCount,
                updateCount,
                removeCount,
                evictCount,
                expireCount,
                evictStoreHitCount,
                evictStoreMissCount,
                behindStoreHitCount,
                behindStoreMissCount,
                size,
                memorySize
        );
    }
}
