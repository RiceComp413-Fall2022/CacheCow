package cache.local

import KeyVersionPair
import java.util.concurrent.ConcurrentHashMap

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalCache(private var maxCapacity: Int = 100) : ILocalCache {

    private val cache: ConcurrentHashMap<KeyVersionPair, String> = ConcurrentHashMap<KeyVersionPair, String>(maxCapacity)

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = 0

    override fun fetch(kvPair: KeyVersionPair): String? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache[kvPair] != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
        } else {
            print("CACHE: Key not found\n")
        }

        return cache[kvPair]
    }
    override fun store(kvPair: KeyVersionPair, value: String): Boolean {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")
        if (cache.size < maxCapacity) {
            print("CACHE: Success\n")
            val prevVal = cache[kvPair]
            val prevKvByteSize = if(prevVal == null) 0 else (prevVal.length + kvPair.key.length + 4)
            kvByteSize += (value.length + kvPair.key.length + 4) - prevKvByteSize

            cache[kvPair] = value
            return true
        } else {
            print("CACHE: Full\n")
            return false
        }
    }

    override fun isFull(): Boolean {
        return this.cache.size >= this.maxCapacity
    }

    /**
     * Gets information about the cache at the current moment
     */
    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize)
    }
}


