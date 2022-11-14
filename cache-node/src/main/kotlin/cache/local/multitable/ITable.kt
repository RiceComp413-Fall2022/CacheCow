package cache.local.multitable

import KeyVersionPair

/**
 * Table implementations are expected to be thread-safe!
 */
interface ITable {

    /**
     * Invalidates the table. No operations will be able to work on the table while it is
     * invalid. This is useful to mark before clearing a table. All operations attempted
     * while the table is invalid will return null. Table information can always be read.
     * @return true if this process invalidated the table; false if was already invalidated
     */
    fun invalidate(): Boolean

    /**
     * Validates the table.s
     * @return true if this process validated the table; false if was already validated
     */
    fun validate(): Boolean

    /**
     * Fetch a value from the distributed cache.
     *
     * @param kvPair The key-version pair to look up
     * @return The value if found; null if the value is not found or table is marked invalid.
     */
    fun fetch(kvPair: KeyVersionPair): ByteArray?

    /**
     * Store a value to the distributed cache.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @return true if key-version was stored; false if table is marked invalid
     */
    fun store(kvPair: KeyVersionPair, value: ByteArray): Status

    /**
     * Removes a specified element from the cache.
     * @param kvPair The key-version pair to look up
     * @return previous value associated with the key-version pair, or null if there
     * was no mapping for the key-version pair or the table is marked invalid.
     */
    fun remove(kvPair: KeyVersionPair) : ByteArray?

    /**
     * Clears the entire distributed memory cache. Table should be invalid before it is
     * cleared via a call to invalidate(). This will NOT be called internally. The table
     * will fill up and never remove elements on its own.
     * @param cleanup runs when table is cleared, but before it is revalidated. Safe
     * operation to run before other operations are allowed to affect the table.
     */
    fun clearAll(cleanup: () -> Unit)

    /**
     * Obtains table performance statistics. Works even if the table itself is invalid.
     * @returns information about the table performance.
     */
    fun getTableInfo(): TableInfo

}

enum class Status {
    SUCCESS, FULL, INVALID, MUTATION
}

data class TableInfo(val capacity: Int, val memorySize: Int)