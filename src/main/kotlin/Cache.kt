import interfaces.ICache

/**
 * Concrete Memory Cache.
 */
class Cache : ICache {

    // TODO: Implement concrete hashmap cache.
    // TODO: Implement LRU Replacement?
    // TODO: Determine stored object type.

    override fun store(key: String) {
        print("Storing: $key\n")
    }

    override fun fetch(key: String): String {
        print("Fetching: $key\n")
        return "value"
    }

}