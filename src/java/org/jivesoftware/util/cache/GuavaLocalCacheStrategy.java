package org.jivesoftware.util.cache;

import com.google.common.util.concurrent.Striped;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class GuavaLocalCacheStrategy extends DefaultLocalCacheStrategy {

    private static final Logger Log = LoggerFactory.getLogger(GuavaLocalCacheStrategy.class);

    private Striped<Lock> stripedLocks;

    public GuavaLocalCacheStrategy() {
        int stripes = JiveGlobals.getIntProperty("cache.clustering.guava.locks.stripes", 16);
        this.stripedLocks = Striped.lazyWeakLock(stripes);
        Log.info("Striped local locks activated with {} stripes", stripes);
    }

    @Override
    public Cache createCache(String name) {
        // Get cache configuration from system properties or default (hardcoded) values
        long maxSize = CacheFactory.getMaxCacheSize(name);
        long lifetime = CacheFactory.getMaxCacheLifetime(name);
        int concurrency = CacheFactory.getCacheConcurrency(name);
        // Create cache with located properties
        return new GuavaCache(name, maxSize, lifetime, concurrency);
    }

    @Override
    public Lock getLock(Object key, Cache cache) {
        if (key instanceof String && cache != null) {
            key = cache.getName() + key; // to lock specific cache not server-wide lock
        }
        // use striped weak locks instead of default String.intern based locks
        return stripedLocks.get(key);
    }

    @Override
    public void doClusterTask(ClusterTask task) {
    }

    @Override
    public void doClusterTask(ClusterTask task, byte[] nodeID) {
    }

    @Override
    public Collection<Object> doSynchronousClusterTask(ClusterTask task, boolean includeLocalMember) {
        return Collections.emptyList();
    }

    @Override
    public Object doSynchronousClusterTask(ClusterTask task, byte[] nodeID) {
        return null;
    }
}
