package com.cache.bigcache.sotrage;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @Author: fake1997
 * @date: 2022/4/12
 */
public interface IStorageBlock extends Comparable<IStorageBlock>, Closeable {

    /**
     * Retrieves the payload associated with the pointer and always update the access time
     *
     * @param pointer
     * @return the byte[]
     * @throws IOException
     */
    byte[] retrieve(Pointer pointer) throws IOException;

    /**
     * Removes the payload and marks the used space as dirty
     *
     * @param pointer
     * @return
     * @throws IOException
     */
    byte[] remove(Pointer pointer) throws IOException;

    /**
     * Removes the payload without returning the payload
     *
     * @param pointer
     * @throws IOException
     */
    void removeLight(Pointer pointer) throws IOException;

    /**
     * stores the payload
     * @param payload
     * @return
     * @throws IOException
     */
    Pointer store(byte[] payload) throws IOException;

    /**
     * Update the payload by marking exSpace as dirty
     *
     * @param pointer
     * @param payload
     * @return
     * @throws IOException
     */
    Pointer update(Pointer pointer, byte[] payload) throws IOException;

    /**
     *
     * @return the total size of the dirty space
     */
    long getDirty();

    /**
     * calculates and returns total size of the used space
     *
     * @return
     */
    long getUsed();

    /**
     *
     * @return
     */
    long getCapacity();

    /**
     *
     * @return
     */
    double getDirtyRatio();

    /**
     *
     * @return
     */
    int getIndex();

    /**
     *
     */
    void free();
}