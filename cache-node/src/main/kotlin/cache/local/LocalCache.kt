package cache.local

import KeyVersionPair
import java.util.concurrent.ConcurrentHashMap

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalCache(private var maxCapacity: Int = 100) : ILocalCache {

    private val cache: ConcurrentHashMap<KeyVersionPair, String> = ConcurrentHashMap<KeyVersionPair, String>(maxCapacity)

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
}