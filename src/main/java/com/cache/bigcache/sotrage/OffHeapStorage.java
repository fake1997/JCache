package com.cache.bigcache.sotrage;

import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class OffHeapStorage implements IStorage {
    protected final AtomicBoolean disposed = new AtomicBoolean(false);
    protected ByteBuffer buffer;

    private static final Unsafe UNSAFE = getUnsafe();
    private static final long BYTE_ARRAY_OFFSET = (long) UNSAFE.arrayBaseOffset(byte[].class);

    private final long address;

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public OffHeapStorage(int capacity) {
        this.address = UNSAFE.allocateMemory(capacity);
    }

    public OffHeapStorage(int capacity, ByteBuffer byteBuffer) {
        this.buffer = ByteBuffer.allocateDirect(capacity);
        try {
            Method method = byteBuffer.getClass().getDeclaredMethod("address");
            method.setAccessible(true);
            this.address = (long) method.invoke(byteBuffer);
        } catch (Exception e) {
            throw new RuntimeException("Unable to allocate offheap memory using sum.misc.Unsafe on the platform", e);
        }
    }

    /**
     * @param position
     * @param dest
     * @throws IOException
     */
    @Override
    public void get(int position, byte[] dest) throws IOException {
        assert !disposed.get() : "disposed";
        assert position >= 0 : position;
        this.get(address + position, dest, BYTE_ARRAY_OFFSET, dest.length);
    }

    private void get(long baseAddress, byte[] dest, long destOffset, int length) {
        UNSAFE.copyMemory(null, baseAddress, dest, destOffset, length);
    }

    @Override
    public void put(int position, byte[] source) throws IOException {
        assert !disposed.get() : "disposed";
        assert position >= 0 : position;
        this.put(BYTE_ARRAY_OFFSET, source, address + position, source.length);
    }

    private void put(long sourceOffset, byte[] source, long baseAddress, int length) {
        UNSAFE.copyMemory(source, sourceOffset, null, baseAddress, length);
    }

    @Override
    public void free() {
        // do nothing
    }

    @Override
    public void close() throws Exception {
        if (!disposed.compareAndSet(false, true)) { //true
            return;
        }
        UNSAFE.freeMemory(address);
    }
}
