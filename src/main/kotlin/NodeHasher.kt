/**
 * Class for determining the same node consistently for a given key,version pair
 */
class NodeHasher(nodeCount: Int) {

    private val nodeCount: Int

    init {
        this.nodeCount = nodeCount
    }

    /**
     * Get designated node for key-version pair
     * @param kvPair the key version pair
     * @return the id of the node designated for the key version pair
     */
    fun hash(kvPair: KeyVersionPair): NodeId {
        // TODO: how to handle collisions? Closed hashing? Do we do that here?
        //  Should we have access to sender and currend node here to check if the
        //  hashed node is full
        return kvPair.key.hashCode() % nodeCount    // TODO: fix this hash function
    }
}