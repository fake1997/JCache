package com.cache.bigcache;

import com.cache.bigcache.lock.StripedReadWriteLock;
import com.cache.bigcache.sotrage.Pointer;
import com.cache.bigcache.sotrage.StorageBlock;
import com.cache.bigcache.sotrage.StorageManager;
import com.cache.bigcache.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

public class BigCache<K> implements ICache<K> {
    /** the default purge interval which is 5 minutes */
    public static final long DEFAULT_PURGE_INTERVAL = 5 * 60 * 1000;

    /** the default merge interval which is 10 minutes. */
    public static final long DEFAULT_MERGE_INTERVAL = 10 * 60 * 1000;

    /** the default threshold for dirty block recycling */
    public static final double DEFAULT_DIRTY_RATIO_THRESHOLD = 0.5;

    /** the constant default concurrency level. The default is 2^8=256 concurrent level */
    public static final int DEFAULT_CONCURRENCY_LEVEL = 8;

    /** the length of value can't be greater than 4m */
    public static final int MAX_VALUE_LENGTH = 4 * 1024 * 1024;

    /** the hit counter */
    protected AtomicLong hitCounter = new AtomicLong(0);

    /** The miss counter. */
    protected AtomicLong missCounter = new AtomicLong();

    /** The get counter. */
    protected AtomicLong getCounter = new AtomicLong();

    /** The put counter. */
    protected AtomicLong putCounter = new AtomicLong();

    /** The delete counter. */
    protected AtomicLong deleteCounter = new AtomicLong();

    /** The # of purges due to expiration. */
    protected AtomicLong purgeCounter = new AtomicLong();

    /** The # of moves for dirty block recycle. */
    protected AtomicLong moveCounter = new AtomicLong();

    /** The total storage size we have used, including the expired ones which are still in the pointermap */
    protected AtomicLong usedSize = new AtomicLong();

    // 核心
    protected final ConcurrentHashMap<K, CacheValueWrapper> pointerMap = new ConcurrentHashMap<>();

    /** managing the storages */
    final StorageManager storageManager;

    private final StripedReadWriteLock readWriteLock;

    /** the times of merge procedure has run. */
    private final AtomicLong NO_OF_MERGE_RUN = new AtomicLong();

    /** the times of purge procedure has run. */
    private final AtomicLong NO_OF_PURGE_RUN = new AtomicLong();

    private String cacheDir;

    /** the thread poll which is used to clean the cache */
    private ScheduledExecutorService ses;

    /** dirty ratio which control block recycle */
    private final double dirtyRatioThreshold;

    public BigCache(String dir, CacheConfig config) throws IOException{
        this.cacheDir = dir;
        // 保证路径最后面有 '/'，因为后面创建文件时文件名是直接用 + 创建
        if(!this.cacheDir.endsWith(File.separator)){
            this.cacheDir += File.separator;
        }
        if(!FileUtil.isFilenameValid(this.cacheDir)){
            throw new IllegalArgumentException("Invalid cache data directory : " + this.cacheDir);
        }
        // 目录存在直接删除
        FileUtil.deleteDirectory(new File(this.cacheDir));

        this.storageManager = new StorageManager(this.cacheDir, config.getCapacityPerBlock(), config.getInitialNumberOfBlocks(), config.getStorageMode(), config.getMaxOffHeapMemorySize());
        this.readWriteLock = new StripedReadWriteLock(config.getConcurrencyLevel());

        // 2 threads. one for purge and one for merge?
        ses = new ScheduledThreadPoolExecutor(2);
        ses.scheduleWithFixedDelay(new CacheCleaner(this), config.getPurgeInterval(), config.getPurgeInterval(), TimeUnit.MILLISECONDS);
        ses.scheduleWithFixedDelay(new CacheMerger(this), config.getMergeInterval(), config.getMergeInterval(), TimeUnit.MILLISECONDS);

        dirtyRatioThreshold = config.getDirtyRatioThreshold();
    }

    @Override
    public void put(K key, byte[] value) throws IOException {
        this.put(key,value,-1);
    }

    @Override
    public void put(K key, byte[] value, long tti) throws IOException {
        putCounter.incrementAndGet();
        if(value == null || value.length > MAX_VALUE_LENGTH){
            throw new IllegalArgumentException("value is null or too large");
        }

        writeLock(key);
        try{
            CacheValueWrapper wrapper = pointerMap.get(key);
            Pointer newPointer;

            if(wrapper == null){
                wrapper = new CacheValueWrapper();
                newPointer = storageManager.store(value);
            } else {
                Pointer oldPointer = wrapper.getPointer();
                newPointer = storageManager.update(oldPointer, value);
                usedSize.addAndGet(oldPointer.getLength() * -1);
            }
            wrapper.setPointer(newPointer);
            wrapper.setTimeToIdle(tti);
            wrapper.setLastAccessTime(System.currentTimeMillis());
            usedSize.addAndGet(newPointer.getLength());
            pointerMap.put(key, wrapper);
        } finally {
            writeUnlock(key);
        }
    }

    @Override
    public byte[] get(K key) throws IOException {
        getCounter.incrementAndGet();
        readLock(key);
        try {
            CacheValueWrapper wrapper = pointerMap.get(key);
            if(wrapper == null){
                missCounter.incrementAndGet();
                return null;
            }
            synchronized (wrapper){
                if(!wrapper.isExpired()){
                    hitCounter.incrementAndGet();
                    wrapper.setLastAccessTime(System.currentTimeMillis());
                    return storageManager.retrieve(wrapper.getPointer());
                }else {
                    missCounter.incrementAndGet();
                    return null;
                }

            }
        } finally {
            readUnlock(key);
        }
    }

    @Override
    public byte[] delete(K key) throws IOException {
        deleteCounter.incrementAndGet();
        writeLock(key);
        try {
            CacheValueWrapper wrapper = pointerMap.get(key);
            if(wrapper != null){
                byte[] payload = storageManager.remove(wrapper.getPointer());
                pointerMap.remove(key);
                usedSize.addAndGet(-1*payload.length);
                return payload;
            }
        }finally {
            writeUnlock(key);
        }
        return null;
    }

    @Override
    public boolean contains(K key) throws IOException {
        return pointerMap.contains(key);
    }

    @Override
    public void clear() {
        this.storageManager.free();

        this.pointerMap.clear();
        this.usedSize.set(0);
    }

    @Override
    public double hitRate() {
        return 1.0 * hitCounter.get() / (hitCounter.get() + missCounter.get());
    }

    @Override
    public void close() throws IOException {
        this.clear();
        this.ses.shutdownNow();
        this.storageManager.close();
    }

    private ReadWriteLock getLock(K key) {
        return readWriteLock.getLock(Math.abs(key.hashCode()));
    }

    protected void readLock(K key){
        readWriteLock.readLock(Math.abs(key.hashCode()));
    }

    protected void readUnlock(K key){
        readWriteLock.readLock(Math.abs(key.hashCode()));
    }

    protected void writeLock(K key){
        readWriteLock.writeLock(Math.abs(key.hashCode()));
    }

    protected void writeUnlock(K key){
        readWriteLock.writeLock(Math.abs(key.hashCode()));
    }

    public long count(){
        return pointerMap.size();
    }

    public double hitRatio() {
        return 1.0 * hitCounter.get() / (hitCounter.get() + missCounter.get());
    }

    abstract static class CacheDaemonWorker<K> implements Runnable{
        private WeakReference<BigCache<K>> cacheHolder;
        private ScheduledExecutorService ses;

        CacheDaemonWorker(BigCache<K> cache){
            ses = cache.ses;
            this.cacheHolder = new WeakReference<BigCache<K>>(cache);
        }

        @Override
        public void run(){
            BigCache cache = cacheHolder.get();
            if(cache == null){
                // cache is recycled abnormally
                if(ses != null){
                    ses.shutdown();
                    ses = null;
                }
                return;
            }
            try {
                process(cache);
            }catch (IOException e){
                e.printStackTrace();
            }
            cache.storageManager.clean();
        }

        abstract void process(BigCache<K> cache) throws IOException;
    }

    /**
     * clean the expired cache
     * @param <K>
     */
    static class CacheCleaner<K> extends CacheDaemonWorker<K>{
        CacheCleaner(BigCache<K> cache) {
            super(cache);
        }

        @Override
        void process(BigCache<K> cache) throws IOException {
            Set<K> keys = cache.pointerMap.keySet();

            // store the expired keys according to their associated lock
            Map<ReadWriteLock, List<K>> expiredKeys = new HashMap<>();

            // find all the keys that may be expired. It's lock less as we will validate later
            for(K key : keys){
                CacheValueWrapper wrapper = cache.pointerMap.get(key);
                if(wrapper != null && wrapper.isExpired()){
                    ReadWriteLock lock = cache.getLock(key);
                    List<K> keyList = expiredKeys.get(lock);
                    if(keyList == null){
                        keyList = new ArrayList<>();
                        expiredKeys.put(lock, keyList);
                    }
                    keyList.add(key);
                }
            }

            // expired keys with write lock, this will complete quickly
            for(ReadWriteLock lock : expiredKeys.keySet()){
                List<K> keyList = expiredKeys.get(lock);
                if(keyList == null || keyList.isEmpty()){
                    continue;
                }
                lock.writeLock().lock();
                try{
                    for(K key : keyList){
                        CacheValueWrapper wrapper = cache.pointerMap.get(key);
                        if(wrapper != null && wrapper.isExpired()){
                            Pointer oldPointer = wrapper.getPointer();
                            cache.usedSize.addAndGet(-1 * oldPointer.getLength());
                            cache.storageManager.removeLight(oldPointer);
                            cache.pointerMap.remove(key);
                            cache.purgeCounter.incrementAndGet();
                        }
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
            cache.NO_OF_PURGE_RUN.incrementAndGet();
        }
    }

    /**
     * Get the latest stats of the cache.
     *
     * @return all stats.
     */
    public BigCacheStats getStats() {
        return new BigCacheStats(hitCounter.get(), missCounter.get(), getCounter.get(),
                putCounter.get(), deleteCounter.get(), purgeCounter.get(), moveCounter.get(),
                count(), storageManager.getUsed(), storageManager.getDirty(),
                storageManager.getCapacity(), storageManager.getUsedBlockCount(), storageManager.getFreeBlockCount(),
                storageManager.getTotalBlockCount());
    }

    static class CacheMerger<K> extends CacheDaemonWorker<K>{
        CacheMerger(BigCache<K> cache) {
            super(cache);
        }

        @Override
        void process(BigCache<K> cache) throws IOException {
            Set<K> keys = cache.pointerMap.keySet();

            // store the keys in dirty block according to the block index
            Map<Integer, List<K>> keysInDirtyBlock = new HashMap<>();

            // 先统计，后计算
            for(K key : keys){
                CacheValueWrapper wrapper = cache.pointerMap.get(key);
                StorageBlock sb;
                Pointer pointer;
                if(wrapper != null
                        && ((pointer = wrapper.getPointer()) != null)
                        && ((sb = pointer.getStorageBlock()) != null)
                        && (sb.getDirtyRatio() > cache.dirtyRatioThreshold)){
                    Integer index = sb.getIndex();
                    List<K> keyList = keysInDirtyBlock.get(index);
                    if(keyList == null){
                        keyList = new ArrayList<>();
                        keysInDirtyBlock.put(index, keyList);
                    }
                    keyList.add(key);
                }
            }

            for(List<K> keyList : keysInDirtyBlock.values()){
                if(keyList == null || keyList.isEmpty()){
                    continue;
                }

                for(K key: keyList){
                    cache.readLock(key);
                    try {
                        CacheValueWrapper wrapper = cache.pointerMap.get(key);
                        if(wrapper == null){
                            continue;
                        }

                        // wrapper is acessed/modified by reader and the merger use lock here
                        synchronized (this){
                            StorageBlock sb = wrapper.getPointer().getStorageBlock();
                            if(sb.getDirtyRatio() > cache.dirtyRatioThreshold){
                                byte[] payload = cache.storageManager.remove(wrapper.getPointer());
                                Pointer newPointer = cache.storageManager.storeExcluding(payload, sb);
                                wrapper.setPointer(newPointer);
                                cache.moveCounter.incrementAndGet();
                            }
                        }
                    }finally {
                        cache.readUnlock(key);
                    }

                }
            }
            cache.NO_OF_MERGE_RUN.incrementAndGet();
        }
    }
}
