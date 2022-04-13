package com.cache.bigcache;

import java.io.Closeable;
import java.io.IOException;

public interface ICache<K> extends Closeable {
    /**
     *
     * @param key
     * @param value
     * @throws IOException
     */
    void put(K key, byte[ ]value) throws IOException;

    /**
     * puts the value with specified key and time to idle in milliseconds
     *
     * @param key
     * @param value
     * @param tti
     * @throws IOException
     */
    void put(K key, byte[] value, long tti) throws IOException;

    /**
     *
     * @param key
     * @return
     * @throws IOException
     */
    byte[] get(K key) throws IOException;

    /**
     *
     * @param key
     * @return
     * @throws IOException
     */
    byte[] delete(K key) throws IOException;

    /**
     *
     * @param key
     * @return
     * @throws IOException
     */
    boolean contains(K key) throws IOException;

    /**
     *
     */
    void clear();

    /**
     *
     * @return
     */
    double hitRate();
}
