package cache.local

import KeyVersionPair
import exception.KeyNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalCache(private var maxCapacity: Int = 100) : ILocalCache {

    private val cache: ConcurrentHashMap<KeyVersionPair, ByteArray> = ConcurrentHashMap<KeyVersionPair, ByteArray>(maxCapacity)

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = 0

    override fun fetch(kvPair: KeyVersionPair): ByteArray {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache[kvPair] != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
        } else {
            print("CACHE: Key not found\n")
        }

        return cache[kvPair] ?: throw KeyNotFoundException(kvPair.key)
    }
    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")
        if (cache.size > maxCapacity) {
            print("CACHE: Full\n")
            // TODO: Remove arbitrary element. Difficult without breaking concurrency.
            return
        }
        print("CACHE: Success\n")
        val prevVal = cache[kvPair]
        val prevKvByteSize = if(prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
        kvByteSize += (value.size + kvPair.key.length + 4) - prevKvByteSize
        cache[kvPair] = value
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        return cache.remove(kvPair)
    }

    override fun clear() {
        cache.clear()
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


