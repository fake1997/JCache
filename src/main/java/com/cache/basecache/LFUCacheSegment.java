package com.cache.basecache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class LFUCacheSegment<K, V> extends ReentrantLock implements IBaseCache<K, V> {
    private int maxCapacity;
    private int minFreq;
    private final Map<K, V> keyToVal;
    private final Map<K, Integer> keyToFreq;
    private final Map<Integer, Set<K>> freqToKey;

    public LFUCacheSegment(int maxCapacity) {
        this.minFreq = 0;
        this.maxCapacity = maxCapacity;
        keyToVal = new HashMap<>();
        keyToFreq = new HashMap<>();
        freqToKey = new HashMap<>();
    }

    private void increaseFreq(K key) {
        Integer freq = keyToFreq.get(key);
        keyToFreq.put(key, freq + 1);

        freqToKey.get(freq).remove(key);
        freqToKey.putIfAbsent(freq + 1, new LinkedHashSet<>());
        freqToKey.get(freq + 1).add(key);
        if (freqToKey.get(freq).isEmpty()) {
            freqToKey.remove(freq);
            if (minFreq == freq) {
                minFreq++;
            }
        }
    }

    @Override
    public V get(K key) {
        lock();
        try {
            V value = keyToVal.get(key);
            if (value != null) {
                increaseFreq(key);
            }
            return value;
        } finally {
            unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        lock();
        try {
            if (keyToVal.containsKey(key)) {
                keyToVal.put(key, value);
                increaseFreq(key);
            } else {
                if (keyToVal.size() >= maxCapacity) {
                    removeMinFreqKey();
                }
                keyToVal.put(key,value);
                keyToFreq.put(key, 1);
                freqToKey.putIfAbsent(1, new LinkedHashSet<>());
                freqToKey.get(1).add(key);
                minFreq = 1;
            }

        } finally {
            unlock();
        }
    }

    private void removeMinFreqKey() {
        LinkedHashSet<K> keyList = (LinkedHashSet<K>) freqToKey.get(minFreq);
        K deleteKey = keyList.iterator().next();
        keyList.remove(deleteKey);
        if(keyList.isEmpty()){
            // 不用管minfreq，因为一定是满了然后插入新的才会导致删除，所以会有一个minfreq等于1的
            freqToKey.remove(minFreq);
        }
        keyToVal.remove(deleteKey);
        keyToFreq.remove(deleteKey);
    }

    @Override
    public boolean contains(K key) {
        lock();
        try {
            return keyToFreq.containsKey(key);
        } finally {
            unlock();
        }
    }

    @Override
    public int size() {
        lock();
        try {
            return keyToVal.size();
        } finally {
            unlock();
        }
    }
}
