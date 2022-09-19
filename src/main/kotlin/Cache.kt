import interfaces.ICache
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete Memory Cache.
 */
class Cache(private var maxCapacity: Int = 100) : ICache {

    private val cache: ConcurrentHashMap<String, String> = ConcurrentHashMap<String, String>(maxCapacity)

    // TODO: Implement concrete hashmap cache.
    // TODO: Implement LRU Replacement?
    // TODO: Determine stored object type.

    override fun store(key: String, value: String) {
        print("Storing: $key\n")
        if (cache.size < maxCapacity) {
            cache[key] = value;
        }
    }

    override fun fetch(key: String): String? {
        print("Fetching: $key\n")
        return cache[key]
    }
}