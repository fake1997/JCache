package com.cache.bigcache.lock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StripedReadWriteLock {
    private final ReentrantReadWriteLock[] locks;

    public StripedReadWriteLock(){
        this(4);
    }

    public StripedReadWriteLock(int storagePower) {
        if(!(storagePower >= 1 && storagePower <= 11)){
            throw new IllegalArgumentException("storage power must be in {1,2,...,11}.");
        }
        int lockSize = (int) Math.pow(2, storagePower);
        locks = new ReentrantReadWriteLock[lockSize];
        for(int i = 0; i < lockSize; i++){
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    public void readLock(int id){
        getLock(id).readLock().lock();
    }

    public void readUnlock(int id){
        getLock(id).readLock().unlock();
    }

    public void writeLock(int id){
        getLock(id).writeLock().lock();
    }

    public void writeUnlock(int id){
        getLock(id).writeLock().unlock();
    }

    public void writeLockForAll(){
        for(ReentrantReadWriteLock lock : locks){
            lock.writeLock().lock();
        }
    }

    public void writeUnlockForAll(){
        for(ReentrantReadWriteLock lock : locks){
            lock.writeLock().unlock();
        }
    }
    /**
     * find the lock associated with the id
     *
     * @param id
     * @return
     */
    public ReentrantReadWriteLock getLock(int id){
        // locks.length - 1 is a string of ones since lock.length is power of 2
        // thus ending cancels out the higher bits of id and leaves the lower bits
        // to determine the lock
        return locks[id & (locks.length - 1)];
    }
}
