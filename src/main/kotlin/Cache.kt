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

    override fun store(key: KeyVersionPair, value: String) {
        print("Storing: $key\n")
        if (cache.size < maxCapacity) {
            cache[key] = value
        }
    }

    override fun fetch(key: KeyVersionPair): String? {
        print("Fetching: $key\n")
        return cache[key]
    }

    override fun isFull(): Boolean {
        return this.cache.size >= this.maxCapacity
    }
}