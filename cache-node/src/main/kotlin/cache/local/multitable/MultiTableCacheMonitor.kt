package cache.local.multitable

import KeyVersionPair
import cache.local.CacheInfo
import cache.local.ILocalCache
import cache.local.aggregateTableInfo
import exception.InvalidInput
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A memory cache implementation with multiple tables of data.
 * All operations are serial. This is a monitor!
 *
 * This class is thread-safe!
 */
class MultiTableCacheMonitor(private val numTables: Int = 3): ILocalCache {


    /**
     * Lock for the entire multi table.
     */
    private val multiTableLock = ReentrantReadWriteLock()


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



    init {
        // Validate all internal tables. Then validate the multi-table cache itself.
        multiTableLock.write {
            //print(numTables)
//            for (table in tables) {
//                //print("${table}\n")
//            }

            for (index in 0 until numTables) {
                val tableIndex = (baseIndex + index) % numTables
                tables[tableIndex].validate()
                //print("MultiTableCacheMonitor: Initialized table ${index}\n")
            }
            //print("MultiTableCacheMonitor: Initialized\n")
        }
    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        multiTableLock.read() {
            //print("MultiTableCacheMonitor: fetch ${kvPair.key}, ${kvPair.version}\n")
            val base = baseIndex

            // Query Element
            for (queryOffset in 0 until numTables) {
                val queryIndex = hotOffset(base, queryOffset) //TODO: Change baseIndex later!
                //print("Fetch: Query Table ${queryIndex} ")
                val value = tables[queryIndex].fetch(kvPair) ?: continue

                //print("Fetch: Finished Querying: ${value}\n")

                // Promote Element: only reaches here if value is not null
                if (queryOffset == 0) { // Already in the hottest table
                    //print("Fetch: Returning value ${value}\n")
                    return value
                }
                // TODO: Run this asynchronously
                for (promoteOffset in queryOffset - 1 downTo 0) {
                    val promoteIndex = hotOffset(base, promoteOffset)
                    //print("Fetch: Promote Table ${promoteIndex} ")
                    when (tables[promoteIndex].store(kvPair, value)) {
                        Status.SUCCESS -> return value
                        Status.MUTATION -> throw InvalidInput(
                            "Promotion: Key-Version pair already cached with a different value. " +
                                    "Please update the version number.")
                        Status.FULL, Status.INVALID -> continue
                    }
                }
                //print("Fetch: Returning value ${value}\n")
                return value
            }
            //print("Fetch: ERROR NOT FOUND!\n")
            return null // Element not found
        }
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        multiTableLock.write() {
            //print("MultiTableCacheMonitor: store ${kvPair.key}, ${kvPair.version}, ${value.toString()}\n")
            val base = baseIndex

            // Store element
            for (offset in 0 until numTables) {
                val storeIndex = coldOffset(base, offset)
                when (tables[storeIndex].store(kvPair, value)) {
                    Status.SUCCESS -> return
                    Status.MUTATION -> throw InvalidInput(
                        "Store: Key-Version pair already cached with a different value. " +
                                "Please update the version number.")
                    Status.INVALID -> continue
                    Status.FULL -> rotateEvict()
                }
            }
        }
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun clearAll() {
        multiTableLock.write {
            //print("MultiTableCacheMonitor: ClearAll\n")
            val base = baseIndex
            // Clears all tables.
            for (offset in 0 until numTables) {
                val clearIndex = coldOffset(base, offset)
                tables[clearIndex].clearAll() {}
            }
        }
    }

    /**
     * Performs eviction.
     *
     * Deletes an entire table and performs ordering rotation.
     */
    private fun rotateEvict() {
        multiTableLock.write {
            //print("MultiTableCacheMonitor: Rotate Evict\n")
            tables[baseIndex].clearAll() {
                baseIndex = (baseIndex + 1) % numTables // Shifts the least popular table to the most popular table.
            }
        }
    }

    /**
     * Index for table at given offset from the hottest table.
     *
     * For example,
     * offset 0: index of the hottest table
     * offset 1: index of the second-hottest table
     * ...
     * offset (numTables - 1): index of the cold table
     */
    private fun hotOffset(base: Int, offset: Int): Int {
        return (base + numTables - offset - 1) % numTables
    }

    /**
     * Index for table at given offset from the coldest table.
     *
     * For example,
     * offset 0: index of the coldest table
     * offset 1: index of the second-coldest table
     * ...
     * offset (numTables - 1): index of the hot table
     */
    private fun coldOffset(base: Int, offset: Int): Int {
        return (base + offset) % numTables
    }

    override fun getCacheInfo(): CacheInfo {
        val base = baseIndex
        var cacheInfo = CacheInfo(0, 0)
        for (offset in 0 until numTables) {
            val infoIndex = coldOffset(base, offset)
            val tableInfo = tables[infoIndex].getTableInfo()
            cacheInfo = aggregateTableInfo(cacheInfo, tableInfo)
        }
        return cacheInfo
    }

}