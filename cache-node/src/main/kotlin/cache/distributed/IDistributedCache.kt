package cache.distributed

import KeyVersionPair

/**
 * An interface specifying the behavior of a distributed data cache.
 */
interface IDistributedCache {

    /**
     * Fetch a value from the distributed cache.
     *
     * @param kvPair The key-version pair to look up
     * @return The value if found
     */
    fun fetch(kvPair: KeyVersionPair): ByteArray?

    /**
     * Store a value to the distributed cache.
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
     * Clears the entire distributed memory cache.
     */
    fun clearAll()
}