package cache.local.multitable

import KeyVersionPair
import cache.local.CacheInfo
import cache.local.ILocalCache
import exception.InvalidInputException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A memory cache implementation with multiple tables of data.
 *
 * This class is thread-safe!
 */
class MultiTableCache(private val numTables: Int = 3) : ILocalCache {

    /** Entire Multi-table-wise Variables **/

    /**
     * Boolean of whether it is valid or not to query the table. Protected by multiTableLock.
     */
    @Volatile private var isValid = false

    /**
     * Lock for the entire multi table.
     */
    private val multiTableLock = ReentrantReadWriteLock()


    /** Tables-wise Variables **/

    /**
     * List of all tables that comprise the multi-table cache. Protected by tablesLock.
     */
    private val tables: Array<ITable> = Array(numTables) {Table()}

    /**
     * Table index of the least popular table. Protected by tablesLock.
     *
     * This is the base pointer for the circular array. (base + n - 1) % n is the index of
     * the most popular table.
     */
    @Volatile private var baseIndex: Int = 0

    /**
     * Lock for the collection of tables. Valid lock should be held before attempting to hold this.
     */
    private val tablesLock = ReentrantReadWriteLock()



    init {
        // Validate all internal tables. Then validate the multi-table cache itself.
        multiTableLock.write {
            for (index in 0 until numTables - 1) {
                val tableIndex = (baseIndex + index) % numTables
                tables[tableIndex].validate()
            }
            isValid = true
        }
    }

    // TODO: Ensure this class is thread safe!
    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        multiTableLock.read() {
            if (!isValid) return null

            // Query Element
            for (index in 1 until numTables) {
                val tableIndex = (baseIndex + numTables - index) % numTables
                val value = tables[tableIndex].fetch(kvPair) ?: continue

                // Promote Element (if not null)
                storeToTable(kvPair, value, tableIndex + 1)

                return value
            }

            return null // Element not found
        }
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        multiTableLock.read() {
            // TODO: Handle invalid
            //if (!isValid) return null // Stall till valid?

            // Store element
            for (index in 0 until numTables - 1) {
                val tableIndex = (baseIndex + numTables - index) % numTables
                when (tables[baseIndex].store(kvPair, value)) {
                    Status.INVALID -> {
                        throw InvalidInputException("Table is being cleared.")
                    }

                    Status.FULL -> {
                        // Clear table
                        rotateEvict()
                        // Store into next table
                        store(kvPair, value)
                    }

                    Status.MUTATION -> throw InvalidInputException(
                        "Key-Version pair already cached with a different value. " +
                                "Please update the version number.")

                    Status.SUCCESS -> return
                }
            }
        }
    }

    /**
     * Stores element to table specified by tableIndex
     */
    private fun storeToTable(kvPair: KeyVersionPair, value: ByteArray, tableIndex: Int) {
        multiTableLock.read() {
            // TODO: Handle invalid
            //if (!isValid) return null // Stall till valid?

            // Store element
            when (tables[tableIndex].store(kvPair, value)) {
                Status.INVALID -> {

                }
                Status.FULL -> {

                }

                Status.MUTATION -> throw InvalidInputException(
                    "Key-Version pair already cached with a different value. " +
                            "Please update the version number.")

                Status.SUCCESS -> return
            }
        }
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
//        multiTableLock.read() {
//            if (!isValid) return null
//
//            // Query Element
//            var value: ByteArray?
//            for (index in 0 until numTables - 1) {
//                val tableIndex = (baseIndex + numTables - index) % numTables
//                value = tables[tableIndex].fetch(kvPair)
//                if (value != null) break
//            }
//
//            //update(kvPair, null)
//            return null
//
//        }
        TODO("Not yet implemented")
    }

    override fun clearAll(isClientRequest: Boolean) {
        if (!invalidate()) return // Other process already clearing multi table

        // Clears all tables.
        for (index in 0 until numTables - 1) {
            val tableIndex = (baseIndex + index) % numTables

            if (!tables[baseIndex].invalidate()) continue // Other process already clearing table
            tables[tableIndex].clearAll() {}
            tables[baseIndex].validate()
        }

        validate()
    }

    /**
     * Performs eviction.
     *
     * Deletes an entire table and performs ordering rotation.
     */
    private fun rotateEvict() {
        if (!tables[baseIndex].invalidate()) return // Other process already clearing table

        tables[baseIndex].clearAll(){}
        tablesLock.write {
            baseIndex = (baseIndex + 1) % numTables // Shifts the least popular table to the most popular table.
        }

        // Validate
        tables[baseIndex].validate()
    }

    private fun invalidate(): Boolean {
        multiTableLock.write() {
            if (!isValid) return false
            isValid = false
            return true
        }
    }

    private fun validate(): Boolean {
        multiTableLock.write() {
            if (isValid) return false
            isValid = true
            return true
        }
    }

    fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCacheInfo(): CacheInfo {
        TODO("Not yet implemented")
    }

}