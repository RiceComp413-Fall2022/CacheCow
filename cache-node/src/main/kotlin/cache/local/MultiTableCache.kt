package cache.local

import KeyVersionPair

/**
 * A memory cache implementation with multiple tables of data.
 *
 * This class is thread-safe!
 */
class MultiTableCache() : ILocalCache {
    // TODO: Ensure this class is thread safe!
    override fun fetch(kvPair: KeyVersionPair): ByteArray {
        TODO("Not yet implemented")
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCacheInfo(): CacheInfo {
        TODO("Not yet implemented")
    }

}