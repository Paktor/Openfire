package org.jivesoftware.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class GuavaCache<K, V> implements Cache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(GuavaCache.class);

    private com.google.common.cache.Cache<K, V> internalCache;
    private String name;
    private long maxCacheSize;
    private long maxLifetime;
    private int segments;
    private AtomicLong weight = new AtomicLong(0);
    private InternalWeigher<K, V> weigher = new InternalWeigher<K, V>();

    public GuavaCache(String name, long maxSize, long maxLifetime, int segments) {
        this.name = name;
        initCache(maxSize, maxLifetime, segments);
    }

    private synchronized void initCache(long maxCacheSize, long maxLifetime, int segments) {
        final CacheBuilder<K, V> builder = (CacheBuilder<K, V>) CacheBuilder.newBuilder();
        builder.recordStats();
        builder.removalListener(new RemovalListener<K, V>() {
            @Override
            public void onRemoval(RemovalNotification<K, V> notification) {
                weight.addAndGet(-weigher.weigh(notification.getKey(), notification.getValue()));
            }
        });

        if (segments >= 1) {
            builder.concurrencyLevel(segments);
        }
        if (maxCacheSize > 0) {
            builder.maximumWeight(maxCacheSize);
            builder.weigher(weigher);
        }
        if (maxLifetime > 0) {
            builder.expireAfterAccess(maxLifetime, TimeUnit.MILLISECONDS);
        }
        internalCache = builder.build();
        this.maxCacheSize = maxCacheSize;
        this.maxLifetime = maxLifetime;
        this.segments = segments;
        weight.set(0);
        log.info("Cache '" + name + "' created: " + builder.toString());
    }

    private static class InternalWeigher<K, V> implements Weigher<K, V> {
        @Override
        public int weigh(K key, V value) {
            try {
                return CacheSizes.sizeOfAnything(value);
            } catch (CannotCalculateSizeException e) {
                log.warn(e.getMessage(), e);
                return 1;
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    @Override
    public void setMaxCacheSize(int maxSize) {
        boolean recreate = internalCache != null && maxSize != maxCacheSize;
        if (recreate) {
            CacheFactory.setMaxSizeProperty(name, maxCacheSize);
            initCache(maxSize, maxLifetime, segments);
        }
    }

    @Override
    public long getMaxLifetime() {
        return maxLifetime;
    }

    @Override
    public void setMaxLifetime(long maxLifetime) {
        boolean recreate = internalCache != null && maxLifetime != this.maxLifetime;
        if (recreate) {
            CacheFactory.setMaxLifetimeProperty(name, maxLifetime);
            initCache(maxCacheSize, maxLifetime, segments);
        }
    }

    @Override
    public int getCacheSize() {
        internalCache.cleanUp();
        return (int) weight.get();
    }

    @Override
    public long getCacheHits() {
        return internalCache.stats().hitCount();
    }

    @Override
    public long getCacheMisses() {
        return internalCache.stats().missCount();
    }

    @Override
    public int size() {
        internalCache.cleanUp();
        return (int) internalCache.size();
    }

    @Override
    public boolean isEmpty() {
        internalCache.cleanUp();
        return internalCache.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        internalCache.cleanUp();
        return internalCache.asMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        internalCache.cleanUp();
        return internalCache.asMap().containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }
        return internalCache.getIfPresent(key);
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            log.error("Null used as a key for " + name);
            return null;
        }

        V previous = internalCache.getIfPresent(key);

        int objectSize = weigher.weigh(key, value);

        // If the object is bigger than the entire cache, simply don't add it.
        if (maxCacheSize > 0 && objectSize > maxCacheSize * .90) {
            log.warn("Cache: " + name + " -- object with key " + key + " is too large to fit in cache. Size is " + objectSize);
            return value;
        }

        weight.addAndGet(objectSize);
        if (value != null) {
            internalCache.put(key, value);
        } else if (previous != null){
            internalCache.invalidate(key);
        }
        return previous;
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            return null;
        }
        V value = internalCache.getIfPresent(key);
        internalCache.invalidate(key);
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        internalCache.invalidateAll();
    }

    @Override
    public Set<K> keySet() {
        return internalCache.asMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return internalCache.asMap().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return internalCache.asMap().entrySet();
    }
}
