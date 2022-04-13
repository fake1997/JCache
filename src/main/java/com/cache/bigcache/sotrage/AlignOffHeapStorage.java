package com.cache.bigcache.sotrage;

import java.io.IOException;
import jdk.internal.ref.Cleaner;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class AlignOffHeapStorage extends OffHeapStorage{
    public AlignOffHeapStorage(int capacity) {
        super(capacity, ByteBuffer.allocateDirect(capacity));
    }

    @Override
    public void close() throws IOException {
        if(!disposed.compareAndSet(false, true)){
            return;
        }
        if(buffer == null){
            return;
        }
        try {
            Field cleanerField = buffer.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            /**  注意导入的Cleaner位置 */
            Cleaner cleaner = (Cleaner) cleanerField.get(buffer);
            cleaner.clean();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
