package com.cache.bigcache;

import com.cache.bigcache.sotrage.Pointer;

public class CacheValueWrapper {
    protected Pointer pointer;

    protected long lastAccessTime = -1L;

    /** time to idel in millisecond */
    protected long timeToIdle = -1L;

    public long getLastAccessTime(){
        return lastAccessTime;
    }

    public CacheValueWrapper(){
        // Non argument constructor
    }

    public CacheValueWrapper(Pointer pointer, long lastAccessTime, long timeToIdle) {
        this.pointer = pointer;
        this.lastAccessTime = lastAccessTime;
        this.timeToIdle = timeToIdle;
    }

    public void setLastAccessTime(long accessTime){
        if(lastAccessTime < 0){
            lastAccessTime = accessTime;
            return;
        }
        if(lastAccessTime >= accessTime){
            return;
        }
        if(isExpired()){
            return;
        }
        lastAccessTime = accessTime;
    }

    public Pointer getPointer() {
        return pointer;
    }

    public void setPointer(Pointer pointer) {
        this.pointer = pointer;
    }

    public long getTimeToIdle() {
        return timeToIdle;
    }

    public void setTimeToIdle(long timeToIdle) {
        this.timeToIdle = timeToIdle;
    }

    public boolean isExpired(){
        if(timeToIdle <= 0 ){
            return false;
        }
        if(lastAccessTime < 0){
            return false;
        }
        return System.currentTimeMillis() - lastAccessTime > timeToIdle;
    }
}
