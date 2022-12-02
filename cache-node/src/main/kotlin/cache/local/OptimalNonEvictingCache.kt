package cache.local

import KeyVersionPair
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimal cache that implements a concurrent hashtable but does not perform eviction.
 *
 * This is useful because it provides an optimal runtime for any cache that we implement.
 * This is a performance lower-bound that we can compare our cache against. This is a
 * lower-bound because we require all caches we design to implement eviction.
 *
 * Note: the usefulness of this lower-bound is dependent on the ConcurrentHashMap being
 * efficient. For the sake of this project, we assume it is efficient.
 */
class OptimalNonEvictingCache : ILocalCache {

    /**
     * Concurrent hash storage.
     */
    private val cache: ConcurrentHashMap<KeyVersionPair, ByteArray> = ConcurrentHashMap<KeyVersionPair, ByteArray>()

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        return cache[kvPair]
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        cache[kvPair] = value
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        return cache.remove(kvPair)
    }

    override fun clearAll(isClientRequest: Boolean) {
        cache.clear()
    }

    override fun getCacheInfo(): CacheInfo {
        TODO("Not yet implemented")
    }

}