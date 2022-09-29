import interfaces.ICache
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete Memory Cache.
 */
class Cache(private var maxCapacity: Int = 100) : ICache {

    private val cache: ConcurrentHashMap<KeyVersionPair, String> = ConcurrentHashMap<KeyVersionPair, String>(maxCapacity)

    // TODO: Implement concrete hashmap cache.
    // TODO: Implement LRU Replacement?
    // TODO: Determine stored object type.

    override fun store(kvPair: KeyVersionPair, value: String) {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")
        if (cache.size < maxCapacity) {
            print("CACHE: Success\n")
            cache[kvPair] = value
        } else {
            print("CACHE: Full\n")
        }
    }

    override fun fetch(kvPair: KeyVersionPair): String? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache[kvPair] != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
        } else {
            print("CACHE: Value not found\n")
        }
        return cache[kvPair]
    }

    override fun isFull(): Boolean {
        return this.cache.size >= this.maxCapacity
    }
}