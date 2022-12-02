package cache.local

import KeyVersionPair
import exception.CacheFullException
import exception.KeyNotFoundException
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
        print("LOCAL CACHE: Attempting to fetch ${kvPair.key}\n")
        return cache[kvPair]
    }
    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        print("LOCAL CACHE: Attempting to store (${kvPair.key}, $value)\n")
        if (cache.size >= maxCapacity) {
            print("CACHE: Full\n")
            throw CacheFullException()
        }
        print("LOCAL CACHE: Success\n")
        val prevVal = cache[kvPair]
        val prevKvByteSize = if (prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
        kvByteSize.set(
            kvByteSize.addAndGet((value.size + kvPair.key.length + 4) - prevKvByteSize))
        cache[kvPair] = value
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        return cache.remove(kvPair)
    }

    override fun clearAll(isClientRequest: Boolean) {
        cache.clear()
    }

    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize.get())
    }
}


