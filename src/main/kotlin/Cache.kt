import Interfaces.ICache

/**
 * Concrete Memory Cache.
 */
class Cache : ICache {

    override fun store(key: String) {
        print("Storing: $key\n")
    }

    override fun fetch(key: String) {
        print("Fetching: $key\n")
    }

}