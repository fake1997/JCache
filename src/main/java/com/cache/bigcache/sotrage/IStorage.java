package com.cache.bigcache.sotrage;

import java.io.IOException;

public interface IStorage extends AutoCloseable{
    public static final String DATA_FILE_SUFFIX = ".data";

    /**
     *
     * @param position
     * @param dest
     * @throws IOException
     */
    void get(int position, byte[] dest) throws IOException;

    /**
     *
     * @param position
     * @param source
     * @throws IOException
     */
    void put(int position, byte[] source) throws IOException;

    /**
     * free the storage
     */
    void free();
}
