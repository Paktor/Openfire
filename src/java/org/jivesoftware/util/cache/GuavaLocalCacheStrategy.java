package org.jivesoftware.util.cache;

import com.google.common.util.concurrent.Striped;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class GuavaLocalCacheStrategy extends DefaultLocalCacheStrategy {
    private static final Logger Log = LoggerFactory.getLogger(GuavaLocalCacheStrategy.class);

    private Striped<Lock> stripedLocks;

    public GuavaLocalCacheStrategy() {
        // stripes = client connections amount
        int stripes = JiveGlobals.getIntProperty("xmpp.client.processing.threads", 16);
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
        // use striped weak locks instead of default String.intern based locks
        return stripedLocks.get(key);
    }
}
