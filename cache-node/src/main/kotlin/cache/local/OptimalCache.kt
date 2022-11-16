package cache.local

import KeyVersionPair

/**
 * Optimal cache that does nothing except simply return.
 *
 * This is useful because it provides an optimal runtime for any cache that we implement.
 * This is a performance lower-bound that we can compare our cache against.
 *
 * This can be used to measure the overhead of our entire distributed system, separately
 * from the efficiency of our cache.
 */
class OptimalCache: ILocalCache {

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        return null
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        return
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        return null
    }

    override fun clearAll() {
        return
    }

    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(0, 0)
    }
}