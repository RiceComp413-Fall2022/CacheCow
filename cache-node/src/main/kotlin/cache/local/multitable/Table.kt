package cache.local.multitable

import KeyVersionPair
import exception.ProgramAssumptionException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Implementation for a single table of the multi-tabled cache.
 *
 * If table is invalid, operations will return with their respective failure cases:
 * null for objects, false for booleans. The exceptions to this are querying functions
 * like getTableInfo().
 *
 * This is thread-safe!
 */
class Table(private val maxCapacity: Int = 100, private val maxMemorySize: Int = 5000): ITable {

    /** Entire Table-wise Variables **/

    /**
     * Boolean of whether it is valid or not to query the table. Protected by tableLock.
     */
    @Volatile private var isValid = false

    /**
     * Lock for the entire table.
     */
    private val tableLock = ReentrantReadWriteLock()



    /** Cache-wise Variables **/

    /**
     * Hash table cache. Protected by cacheLock.
     */
    // TODO: Make this concurrent for safety?
    private val cache = HashMap<KeyVersionPair, ByteArray?>(maxCapacity)

    /**
     * Memory usage of user data. Protected by cacheLock.
     *
     * This does not account for the memory usage of internal data structures. Since there
     * is a max capacity for our hashmap, the internal memory usage should be bounded by
     * a constant. The user data memory usage is not bounded and should be managed.
     */
    private var memorySize = 0

    /**
     * Lock for the cache. Valid lock should be held before attempting to hold this.
     */
    private val cacheLock = ReentrantReadWriteLock()



    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("Table: fetch ${kvPair.key}, ${kvPair.version}\n")
        tableLock.read() {
            if (!isValid) return null

            cacheLock.read() {
                return cache[kvPair]
            }
        }
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray): Status {
        print("Table: store ${kvPair.key}, ${kvPair.version}, ${value}\n")
        tableLock.read() {
            if (!isValid) return Status.INVALID

            cacheLock.write() {
                val currentValue = cache[kvPair]
                if (currentValue != null) {  // Value already stored
                    return if (currentValue.contentEquals(value)) {
                        Status.SUCCESS // Already stored with correct value
                    } else {
                        Status.MUTATION // Already stored with different value
                    }
                }
                if (isFull()) return Status.FULL

                // Update memory usage
                memorySize += if (cache.containsKey(kvPair)) {
                    // updated memory = new value size - old value size
                    value.size - (cache[kvPair]?.size ?: 0)
                } else {
                    // updated memory = new key-value pair size + new value size
                    (kvPair.key.length + 4) + value.size
                }

                // Store data
                cache[kvPair] = value
                return Status.SUCCESS
            }
        }
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        tableLock.read() {
            if (!isValid) return null

            cacheLock.write() {
                val oldValue = cache[kvPair]
                cache[kvPair] = null // Invalidate item
                // TODO: Test whether it is better to invalidate item or delete it?
                memorySize -= oldValue?.size ?: 0 // Removed old value memory
                return oldValue
            }
        }
    }

//    override fun clearAll() {
//        print("Table: clearAll\n")
//        tableLock.read {
//            print("Table Valid: ${isValid}\n")
//            if (isValid) throw ProgramAssumptionException("Table should be invalidated before clearing.")
//
//            // Clear Cache
//            cacheLock.write() {
//                if (isFull()) {
//                    print("Table: Full, Cleared!\n")
//                    cache.clear()
//                    memorySize = 0
//                }
//            }
//        }
//    }

    override fun clearAll(cleanup: () -> Unit) {
        print("Table: clearAll\n")

        if (!invalidate()) return // Other process already clearing table

        // Clear Cache
        cacheLock.write() {
            if (isFull()) {
                print("Table: Full, Cleared!\n")
                cache.clear()
                memorySize = 0
            }
        }

        tableLock.read {
            if (isValid) throw ProgramAssumptionException("Table should be invalidated while clearing.")

        }

        cleanup()

        validate()
    }

    override fun getTableInfo(): TableInfo {
        // TODO: Should this perform when table is invalid?
        cacheLock.read() {
            return TableInfo(cache.size, memorySize)
        }
    }

    /**
     * Returns true if the table is full either due to capacity or memory. Works when the
     * table is invalid!
     *
     * The capacity size of the table is monotonically increasing, until the table is
     * marked invalid and cleared.
     *
     * Memory size refers to the size of user data. This is not monotonically increasing.
     * Therefore, one query may think the table is full and a later query may not think
     * the table is full. ClearAll() checks this before clearing the table.
     */
    private fun isFull(): Boolean {
        print("Table: full!\n")
        cacheLock.read() {
            return cache.size == maxCapacity || memorySize > maxMemorySize
        }
    }

    override fun invalidate(): Boolean {
        print("Table: invalidated!\n")
        tableLock.write() {
            if (!isValid) return false
            isValid = false
            return true
        }
    }

    override fun validate(): Boolean {
        print("Table: validated!\n")
        tableLock.write() {
            if (isValid) return false
            isValid = true
            return true
        }
    }

}