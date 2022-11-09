package cache.local

import KeyVersionPair
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalCache(private var maxCapacity: Int = 100) : ILocalCache {

    private val cache: ConcurrentHashMap<KeyVersionPair, ByteArray> = ConcurrentHashMap<KeyVersionPair, ByteArray>(maxCapacity)

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = AtomicInteger(0)

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache[kvPair] != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
        } else {
            print("CACHE: Key not found\n")
        }

        return cache[kvPair]
    }
    override fun store(kvPair: KeyVersionPair, value: ByteArray): Boolean {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")
        return if (cache.size < maxCapacity) {
            print("CACHE: Success\n")
            val prevVal = cache[kvPair]
            val prevKvByteSize = if(prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
            kvByteSize.set(
                kvByteSize.addAndGet((value.size + kvPair.key.length + 4) - prevKvByteSize))

            cache[kvPair] = value
            true
        } else {
            print("CACHE: Full\n")
            false
        }
    }

    override fun isFull(): Boolean {
        return this.cache.size >= this.maxCapacity
    }

    /**
     * Gets information about the cache at the current moment
     */
    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize.get())
    }
}


