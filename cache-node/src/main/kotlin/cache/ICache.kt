package cache

import KeyVersionPair

interface ICache {

    /**
     * Fetches a value from the cache.
     *
     * @param kvPair The key-version pair to look up
     * @return The value if found
     */
    fun fetch(kvPair: KeyVersionPair): ByteArray?

    /**
     * Stores a value to the cache.
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
    fun remove(kvPair: KeyVersionPair): ByteArray?

    /**
     * Clears the entire cache.
     */
    fun clearAll(isClientRequest: Boolean)
}