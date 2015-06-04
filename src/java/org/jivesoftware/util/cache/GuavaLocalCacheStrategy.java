package org.jivesoftware.util.cache;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class GuavaLocalCacheStrategy extends DefaultLocalCacheStrategy {

    @Override
    public Cache createCache(String name) {
        // Get cache configuration from system properties or default (hardcoded) values
        long maxSize = CacheFactory.getMaxCacheSize(name);
        long lifetime = CacheFactory.getMaxCacheLifetime(name);
        int concurrency = CacheFactory.getCacheConcurrency(name);
        // Create cache with located properties
        return new GuavaCache(name, maxSize, lifetime, concurrency);
    }
}
