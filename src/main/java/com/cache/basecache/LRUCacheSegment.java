package com.cache.basecache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRUCacheSegment<K, V> implements IBaseCache<K, V> {
    private int maxCapacity;
    private Map<K, Node<K, V>> map;
    private Node<K, V> head, tail;

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    public LRUCacheSegment(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        map = new HashMap<>();
        head = tail = null;
    }

    @Override
    public V get(K key) {
        readLock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                return null;
            }
            removeNode(node);
            offerTail(node);
            return node.value;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        writeLock.lock();
        try {
            if (map.containsKey(key)) {
                Node<K, V> node = map.get(key);
                node.value = value;
                removeNode(node);
                offerTail(node);
            } else {
                if(map.size() >= maxCapacity){
                    map.remove(head.key);
                    removeNode(head);
                }
                Node<K,V> node = new Node<>(key, value);
                offerTail(node);
                map.put(key, node);
            }
        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public boolean contains(K key) {
        readLock.lock();
        try {
            return map.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return map.size();
        } finally {
            readLock.unlock();
        }
    }

    private static class Node<K, V> {
        private K key;
        private V value;
        private Node<K, V> prev, next;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private void offerTail(Node<K, V> node) {
        if (tail == null) {
            head = tail = node;
            return;
        }
        node.prev = tail.prev;
        node.next = tail.next;
        node.prev.next = node;
    }

    private void removeNode(Node<K, V> node) {
        if (node == null) {
            return;
        }

        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }
}
