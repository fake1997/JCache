package com.cache.bigcache.sotrage;

import com.cache.bigcache.CacheConfig.StorageMode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageBlock implements IStorageBlock{
    /** the index of block among all free blocks in freeBlockQueue */
    private final int index;

    /** the capacity */
    private final int capacity;

    /** the underlying storage */
    private IStorage underlyingStorage; // 指向free block pool

    /** the offset within the storage block. */
    private final AtomicInteger currentOffset = new AtomicInteger(0);

    /** the dirty storage */
    private final AtomicInteger dirtyStorage = new AtomicInteger(0);

    /** the used storage */
    private final AtomicInteger usedStorage = new AtomicInteger(0);

    public StorageBlock(String dir, int index, int capacity, StorageMode storageMode) throws IOException{
        this.index = index;
        this.capacity = capacity;
        switch (storageMode){
            case PureFile -> {
                underlyingStorage = new FileChannelStorage(dir, index, capacity);
                break;
            }
            case MemoryMappedPlusFile -> {
                underlyingStorage = new MemoryMappedStorage(dir, index, capacity);
                break;
            }
            case OffHeapPlusFile -> {
                underlyingStorage = new OffHeapStorage(capacity);
                break;
            }
        }
    }

    /**
     * retrieve data from block
     *
     * @param pointer
     * @return
     * @throws IOException
     */
    @Override
    public byte[] retrieve(Pointer pointer) throws IOException {
        byte[] payload = new byte[pointer.getLength()];
        underlyingStorage.get(pointer.getPosition(),payload);
        return payload;
    }

    @Override
    public byte[] remove(Pointer pointer) throws IOException {
        byte[] payload = retrieve(pointer);
        dirtyStorage.addAndGet(pointer.getLength());
        usedStorage.addAndGet(-1*pointer.getLength());
        return payload;
    }

    @Override
    public void removeLight(Pointer pointer) throws IOException {
        dirtyStorage.addAndGet(pointer.getLength());
        usedStorage.addAndGet(-1*pointer.getLength());
    }

    /**
     *
     * @param payload
     * @return
     * @throws IOException
     */
    protected Allocation allocate(byte[] payload) throws IOException{
        int payloadLength = payload.length;
        int allocationOffset = currentOffset.addAndGet(payloadLength);
        if(this.capacity < allocationOffset){
            return null;
        }
        Allocation allocation = new Allocation(allocationOffset, payloadLength);
        return allocation;
    }

    protected Pointer store(Allocation allocation, byte[] payload) throws IOException{
        Pointer pointer = new Pointer(allocation.getOffset(),allocation.getLength(),this);
        underlyingStorage.put(allocation.getOffset(), payload);
        usedStorage.addAndGet(payload.length);
        return pointer;
    }

    @Override
    public Pointer store(byte[] payload) throws IOException {
        Allocation allocation = allocate(payload);
        if(allocation == null) { return  null; }
        Pointer pointer = store(allocation, payload);
        return pointer;
    }

    @Override
    public Pointer update(Pointer pointer, byte[] payload) throws IOException {
        if(pointer.getLength() >= payload.length){
            dirtyStorage.addAndGet(pointer.getLength() - payload.length);
            usedStorage.addAndGet(-1* pointer.getLength());
            Allocation allocation = new Allocation(pointer.getPosition(), payload.length);
            return store(allocation, payload);
        } else{
            dirtyStorage.addAndGet(pointer.getLength());
            usedStorage.addAndGet(-1*pointer.getLength());
            return store(payload);
        }
    }

    @Override
    public long getDirty() {
        return this.dirtyStorage.get();
    }

    @Override
    public long getUsed() {
        return this.usedStorage.get();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public double getDirtyRatio() {
        return (this.getDirty() * 1.0) / capacity;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void free() {
        currentOffset.set(0);
        dirtyStorage.set(0);
        usedStorage.set(0);

        underlyingStorage.free();
    }

    @Override
    public void close() throws IOException {
        if(this.underlyingStorage != null) {
            try {
                underlyingStorage.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int compareTo(IStorageBlock o) {
        return this.index - o.getIndex();
    }

    // the class Allocation
    private class Allocation {
        private int offset;
        private int length;
        public Allocation(int offset, int length){
            this.offset = offset;
            this.length = length;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }
}
