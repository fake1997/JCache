package com.cache.bigcache;

import com.cache.bigcache.utils.FileUtil;
import com.cache.bigcache.utils.TestUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BigCacheTest {
    private static final double STRESS_FACTOR = Double.parseDouble(System.getProperty("STRESS_FACTOR", "1.0"));
    private static final String TEST_DIR = TestUtil.TEST_BASE_DIR + "function/bigcache/";

    private BigCache<String> cache;

    @Parameterized.Parameter(value = 0)
    public CacheConfig.StorageMode storageMode;

    @Parameterized.Parameters
    public static Collection<CacheConfig.StorageMode[]> data() throws IOException {
        CacheConfig.StorageMode[][] data = { { CacheConfig.StorageMode.PureFile },
                { CacheConfig.StorageMode.MemoryMappedPlusFile },
                { CacheConfig.StorageMode.OffHeapPlusFile } };
        return Arrays.asList(data);
    }

    @Test
    public void testBigCache() throws IOException {
        CacheConfig config = new CacheConfig();
        config.setStorageMode(storageMode);
        cache = new BigCache<String>(TEST_DIR, config);
        Set<String> rndStringSet = new HashSet<String>();
        for (int i = 0; i < 2000000 * STRESS_FACTOR; i++) {
            String rndString = TestUtil.randomString(64);
            rndStringSet.add(rndString);
            cache.put(rndString, rndString.getBytes());
            if ((i % 50000) == 0 && i != 0) {
                System.out.println(i + " rows written");
            }
        }

        for (String rndString : rndStringSet) {
            byte[] value = cache.get(rndString);
            assertNotNull(value);
            assertEquals(rndString, new String(value));
        }

        // delete
        for (String rndString : rndStringSet) {
            cache.delete(rndString);
        }

        for (String rndString : rndStringSet) {
            byte[] value = cache.get(rndString);
            assertNull(value);
        }
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testInvalidFileDir() {
//        String fakeDir = "dltestDB://bigcache_test/asdl";
//        CacheConfig config = new CacheConfig();
//        try {
//            cache = new BigCache<String>(fakeDir, config);
//        } catch (IOException e) {
//        }
//    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCacheConfig() {
        CacheConfig config = new CacheConfig().setCapacityPerBlock(12)
                .setConcurrencyLevel(2345)
                .setInitialNumberOfBlocks(27);
    }

    @After
    public void close() throws IOException {
        if (cache == null)
            return;
        try {
            cache.close();
            FileUtil.deleteDirectory(new File(TEST_DIR));
        } catch (IllegalStateException e) {
            System.gc();
            try {
                FileUtil.deleteDirectory(new File(TEST_DIR));
            } catch (IllegalStateException e1) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e2) {
                }
                FileUtil.deleteDirectory(new File(TEST_DIR));
            }
        }
    }
}
