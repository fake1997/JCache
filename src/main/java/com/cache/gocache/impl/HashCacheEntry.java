package com.cache.quickcache.impl;

import com.cache.quickcache.CacheEntry;
import com.cache.quickcache.ExpirationPolicy;
import com.cache.quickcache.TimeHelper;
import sun.misc.Unsafe;

import java.io.Serializable;
import java.lang.reflect.Field;

public class HashCacheEntry<K extends Serializable, V extends Serializable> implements CacheEntry<K,V> {
    final int hash;

    final K key;
    volatile V value;
    volatile long accessTime;
    volatile long writeTime;
    volatile long createTime;
    volatile long expireAfterAccess = -1L;
    volatile long expireAfterWrite = -1L;
    volatile long expireAfterCreate = -1L;

    transient volatile HashCacheEntry<K, V> next = null;
    transient volatile HashCacheEntry<K, V> previous = null;
    transient volatile HashCacheEntry<K, V> nextAccess = null;
    transient volatile HashCacheEntry<K, V> previousAccess = null;

    public HashCacheEntry(int hash, K key, V value, ExpirationPolicy expirationPolicy) {
        this(hash,key, value, expirationPolicy.getAfterAccess(), expirationPolicy.getAfterWrite(), expirationPolicy.getAfterCreate());
    }

    public HashCacheEntry(int hash,K key, V value, long expireAfterAccess, long expireAfterWrite, long expireAfterCreate) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.expireAfterAccess = expireAfterAccess;
        this.expireAfterWrite = expireAfterWrite;
        this.expireAfterCreate = expireAfterCreate;
        accessTime = writeTime = createTime = TimeHelper.nowMs();
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public void setValue(V value) {
        UNSAFE.putOrderedObject(this,valueOffset,value);
    }

    @Override
    public long getAccessTime() {
        return accessTime;
    }

    @Override
    public void setAccessTime(long time) {
        UNSAFE.putLong(this,accessTimeOffset,time);
    }

    @Override
    public long getWriteTime() {
        return writeTime;
    }

    @Override
    public void setWriteTime(long time) {
        UNSAFE.putLong(this,accessTimeOffset,time);
        UNSAFE.putLong(this,writeTimeOffset,time);
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(long time) {
        UNSAFE.putLong(this,accessTimeOffset,time);
        UNSAFE.putLong(this,writeTimeOffset,time);
        UNSAFE.putLong(this,createTimeOffset,time);
    }

    @Override
    public long getExpireAfterAccess() {
        return expireAfterAccess;
    }

    @Override
    public void setExpireAfterAccess(long time) {
        UNSAFE.putLong(this,expireAfterAccessOffset,time);
    }

    @Override
    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }

    @Override
    public void setExpireAfterWrite(long time) {
        UNSAFE.putLong(this,expireAfterWriteOffset,time);
    }

    @Override
    public long getExpireAfterCreate() {
        return expireAfterCreate;
    }

    @Override
    public void setExpireAfterCreate(long time) {
        UNSAFE.putLong(this,expireAfterCreateOffset,time);
    }

    public HashCacheEntry<K, V> getNext() {
        return next;
    }

    public void setNext(HashCacheEntry<K, V> next) {
        UNSAFE.putOrderedObject(this, nextOffset, next);
    }

    public HashCacheEntry<K, V> getPrevious() {
        return previous;
    }

    public void setPrevious(HashCacheEntry<K, V> previous) {
        UNSAFE.putOrderedObject(this, previousOffset, next);
    }

    public HashCacheEntry<K, V> getNextInAccessQueue() {
        return nextAccess;
    }

    public void setNextInAccessQueue(HashCacheEntry<K, V> next) {
        UNSAFE.putOrderedObject(this, nextAccessOffset,next);
    }

    public HashCacheEntry<K, V> getPreviousInAccessQueue() {
        return previousAccess;
    }

    public void setPreviousInAccessQueue(HashCacheEntry<K, V> previous) {
        UNSAFE.putOrderedObject(this, previousAccessOffset, previous);
    }

    static final Unsafe UNSAFE;
    static final long valueOffset;
    static final long accessTimeOffset;
    static final long writeTimeOffset;
    static final long createTimeOffset;
    static final long expireAfterAccessOffset;
    static final long expireAfterWriteOffset;
    static final long expireAfterCreateOffset;

    static final long nextOffset;
    static final long previousOffset;
    static final long nextAccessOffset;
    static final long previousAccessOffset;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            Class k = HashCacheEntry.class;
            valueOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("value"));
            accessTimeOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("accessTime"));
            writeTimeOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("writeTime"));
            createTimeOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("createTime"));
            expireAfterAccessOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("expireAfterAccess"));
            expireAfterWriteOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("expireAfterWrite"));
            expireAfterCreateOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("expireAfterCreate"));

            nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            previousOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("previous"));

            nextAccessOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("nextAccess"));
            previousAccessOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("previousAccess"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
