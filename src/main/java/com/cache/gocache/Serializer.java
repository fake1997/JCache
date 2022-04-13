package com.cache.quickcache;

import java.io.Serializable;

public interface Serializer {
    /**
     * Serialize an object to a byte array.
     *
     * @param o To be serialized.
     * @return the result of serializing.
     * @throws Exception
     */
    public byte[] serialize(Object o) throws Exception;

    /**
     * Deserialize an object from a byte array.
     * And be able to automatically detect the class type to be deserialized.
     *
     * @param value
     * @return
     * @throws Exception
     */
    public Object deserialize(byte[] value) throws Exception;

    /**
     * Deserialize an object from a byte array.
     *
     * @param value
     * @param clazz
     * @param <K>
     * @return
     * @throws Exception
     */
    public <K extends Serializable> K deserialize(byte[] value, Class<K> clazz) throws Exception;
}
