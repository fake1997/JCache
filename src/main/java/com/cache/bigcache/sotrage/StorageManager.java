package com.cache.bigcache.sotrage;

import com.cache.bigcache.CacheConfig.StorageMode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StorageManager implements IStorageBlock{
    /** keep track of the number of blocks allocated */
    private final AtomicInteger blockCount = new AtomicInteger(0);

    /**
     * Directory for cache data store
     */
    private final String dir;

    /**
     * the capacity per block in bytes
     */
    private final int capacityPerBlock;

    /** the active storage block change lock. */
    private final Lock activateBlockChangeLock = new ReentrantLock();

    /**
     * a list of used storage blocks
     */
    private final Queue<IStorageBlock> usedBlocks = new ConcurrentLinkedDeque<>();

    /**
     * A queue of free storage blocks which is a priority queue and always return the block with smallest index (rewrite compareTo method)
     */
    private final Queue<IStorageBlock> freeBlocks = new PriorityBlockingQueue<>();

    /** current activate block for appending new cache data */
    private volatile IStorageBlock activateBlock;

    /** current storage mode */
    private final StorageMode storageMode;

    /** the number of memory blocks allow to be created */
    private int allowedOffHeapModeBlockCount;

    public static final int DEFAULT_CAPACITY_PER_BLOCK = 128 * 1024 * 1024; // 128MB

    public static final int DEFAULT_INITIAL_NUMBER_OF_BLOCKS = 8; // 1GB total

    public static final long DEFAULT_MAX_OFFHEAP_MEMORY_SIZE = 2 * 1024 * 2014 * 1024L; // Unit:GB

    public StorageManager(String dir, int capacityPerBlock, int initialNumberOfBlocks, StorageMode storageMode, long maxOffHeapMemorySize) throws IOException {
        if(storageMode != StorageMode.PureFile){
            this.allowedOffHeapModeBlockCount = (int)(maxOffHeapMemorySize / capacityPerBlock);
        }else {
            this.allowedOffHeapModeBlockCount = 0;
        }
        this.storageMode = storageMode;
        this.capacityPerBlock = capacityPerBlock;
        this.dir = dir;

        for(int i = 0; i < initialNumberOfBlocks; i++){
            IStorageBlock storageBlock = this.createNewBlock(i);
            freeBlocks.offer(storageBlock);
        }

        this.blockCount.set(initialNumberOfBlocks);
        this.activateBlock = freeBlocks.poll();
        this.usedBlocks.add(this.activateBlock);
    }

    private IStorageBlock createNewBlock(int index) throws IOException{
        if(this.allowedOffHeapModeBlockCount > 0){
            IStorageBlock block = new StorageBlock(this.dir, index, this.capacityPerBlock, this.storageMode);
            this.allowedOffHeapModeBlockCount--;
            return block;
        } else {
            return new StorageBlock(this.dir, index, this.capacityPerBlock, StorageMode.PureFile);
        }
    }

    @Override
    public byte[] retrieve(Pointer pointer) throws IOException {
        return pointer.getStorageBlock().retrieve(pointer);
    }

    @Override
    public byte[] remove(Pointer pointer) throws IOException {
        return pointer.getStorageBlock().remove(pointer);
    }

    @Override
    public void removeLight(Pointer pointer) throws IOException {
        pointer.getStorageBlock().removeLight(pointer);
    }

    @Override
    public Pointer store(byte[] payload) throws IOException {
        Pointer pointer = activateBlock.store(payload);
        if(pointer != null){
            return pointer;
        }
        activateBlockChangeLock.lock();
        try{
            pointer = activateBlock.store(payload);
            if(pointer != null){
                return pointer;
            }
            IStorageBlock freeBlock = this.freeBlocks.poll();
            if(freeBlock == null){
                freeBlock = this.createNewBlock(this.blockCount.getAndIncrement());
            }
            pointer = freeBlock.store(payload);
            this.activateBlock = freeBlock;
            this.usedBlocks.add(this.activateBlock);
            return pointer;
        } finally {
            activateBlockChangeLock.unlock();
        }
    }

    public Pointer storeExcluding(byte[] payload, StorageBlock exludingBlock) throws IOException{
        while(this.activateBlock == exludingBlock){
            activateBlockChangeLock.lock();
            try {
                if(this.activateBlock != exludingBlock){
                    break;
                }
                IStorageBlock freeBlock = this.freeBlocks.poll();
                if(freeBlock == null){
                    freeBlock = this.createNewBlock(this.blockCount.getAndIncrement());
                }
                this.activateBlock = freeBlock;
                this.usedBlocks.add(this.activateBlock);
            } finally {
                activateBlockChangeLock.unlock();
            }
        }
        return store(payload);
    }


    @Override
    public Pointer update(Pointer pointer, byte[] payload) throws IOException {
        Pointer updatePointer = pointer.getStorageBlock().update(pointer, payload);
        if(updatePointer != null){
            return updatePointer;
        }
        return store(payload);
    }

    @Override
    public long getDirty() {
        long dirtyStorage = 0;
        for(IStorageBlock block : usedBlocks){
            dirtyStorage += block.getDirty();
        }
        return dirtyStorage;
    }

    @Override
    public long getUsed() {
        long usedStorage = 0;
        for(IStorageBlock block : usedBlocks){
            usedStorage += block.getUsed();
        }
        return usedStorage;
    }

    @Override
    public long getCapacity() {
        long totalCapacity = 0;
        for(IStorageBlock block : usedBlocks){
            totalCapacity += block.getCapacity();
        }
        return totalCapacity;
    }

    @Override
    public double getDirtyRatio() {
        return (this.getDirty() * 1.0) / this.getCapacity();
    }

    @Override
    public int getIndex() {
        throw new IllegalStateException(" Not implemented!");
    }

    @Override
    public void free() {
        for(IStorageBlock block: usedBlocks){
            block.free();
            this.freeBlocks.offer(block);
        }
        usedBlocks.clear();
        this.activateBlock = freeBlocks.poll();
        this.usedBlocks.add(this.activateBlock);
    }

    @Override
    public void close() throws IOException {
        for(IStorageBlock block: usedBlocks){
            block.close();
        }

        for(IStorageBlock block:freeBlocks){
            block.close();
        }

        freeBlocks.clear();
    }

    public void clean(){
        synchronized (this){
            Iterator<IStorageBlock> it = usedBlocks.iterator();
            while(it.hasNext()){
                IStorageBlock storageBlock = it.next();
                if(storageBlock == activateBlock){
                    continue;
                }
                if(storageBlock.getUsed() == 0){
                    // we will not allocating memory from it any more and it is used by nobody?
                    storageBlock.free();
                    freeBlocks.add(storageBlock);
                    it.remove();
                }
            }

        }
    }

    @Override
    public int compareTo(IStorageBlock o) {
        throw new IllegalStateException("Not Implemented!");
    }

    public Queue<IStorageBlock> getUsedBlocks() {
        return usedBlocks;
    }

    public Queue<IStorageBlock> getFreeBlocks() {
        return freeBlocks;
    }

    public Set<IStorageBlock> getAllInUsedBlocks(){
        Set<IStorageBlock> allBlocks = new HashSet<>();
        allBlocks.addAll(usedBlocks);
        allBlocks.addAll(freeBlocks);
        return allBlocks;
    }

    public int getTotalBlockCount(){
        return this.getAllInUsedBlocks().size();
    }
}
