/**
 * Class for determining the same node consistently for a given key,version pair
 */
class NodeHasher(nodeCount: Int) {

    private val nodeCount: Int

    init {
        this.nodeCount = nodeCount
    }

    /**
     * Get designated node for key, version pair
     */
    public fun hash(key: String, version: String): Int {
        return 0
    }
}