package cache.local

import KeyVersionPair

/**
 * An interface specifying the behavior of a local data cache.
 */
interface ILocalCache {

    /**
     * Fetch a value from the local cache.
     *
     * @param kvPair The key-version pair to look up
     * @return The value, or null if the lookup fails
     */
    fun fetch(kvPair: KeyVersionPair) : ByteArray?

    /**
     * Store a value to the local cache.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @return True if the store succeeded, and false otherwise
     */
    fun store(kvPair: KeyVersionPair, value: ByteArray): Boolean

    /**
     * Check whether the local cache is full.
     *
     * @return True if the cache is full, and false otherwise
     */
    fun isFull(): Boolean

    fun getCacheInfo() : CacheInfo

}

/**
 * Encapsulates information about the cache in this node
 */
data class CacheInfo(val totalKeys: Int, val kvBytes: Int)