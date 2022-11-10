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
    fun fetch(kvPair: KeyVersionPair) : ByteArray

    /**
     * Store a value to the local cache.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     */
    fun store(kvPair: KeyVersionPair, value: ByteArray)

    /**
     * Removes a specified element from the cache.
     * @param kvPair The key-version pair to look up
     * @return the previous value associated with the key-version pair, or null if there
     * was no mapping for the key-version pair.
     */
    fun remove(kvPair: KeyVersionPair) : ByteArray?

    /**
     * Clears all elements from the cache.
     */
    fun clear()

    /**
     * Check whether the local cache is full.
     *
     * @return True if the cache is full, and false otherwise
     */
    fun isFull(): Boolean

    /**
     * @return Cache metrics and performance statistics.
     */
    fun getCacheInfo() : CacheInfo

}

/**
 * Encapsulates information about the cache in this node
 */
data class CacheInfo(val totalKeys: Int, val kvBytes: Int)