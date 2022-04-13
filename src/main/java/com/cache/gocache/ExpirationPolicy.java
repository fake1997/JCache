package com.cache.quickcache;

public class ExpirationPolicy {
    long afterAccess = -1L;
    long afterWrite = -1L;
    long afterCreate = -1L;

    public static ExpirationPolicy never() {
        return new ExpirationPolicy(-1L, -1L, -1L);
    }

    public static ExpirationPolicy afterAccess(long ms) {
        return new ExpirationPolicy(ms, -1L, -1L);
    }

    public static ExpirationPolicy afterWrite(long ms) {
        return new ExpirationPolicy(-1L, ms, -1L);
    }

    public static ExpirationPolicy afterCreate(long ms) {
        return new ExpirationPolicy(-1L, -1L, ms);
    }

    public static ExpirationPolicy afterAccessOrWrite(long access, long write) {
        return new ExpirationPolicy(access, write, -1L);
    }

    public static ExpirationPolicy after(long access, long write, long create) {
        return new ExpirationPolicy(access, write, create);
    }

    private ExpirationPolicy(long afterAccess, long afterWrite, long afterCreate) {
        this.afterAccess = afterAccess;
        this.afterWrite = afterWrite;
        this.afterCreate = afterCreate;
    }

    public long getAfterAccess() {
        return afterAccess;
    }

    public long getAfterWrite() {
        return afterWrite;
    }

    public long getAfterCreate() {
        return afterCreate;
    }
}
