import org.apache.commons.codec.digest.MurmurHash3

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
    fun primaryHash(kvPair: KeyVersionPair): NodeId {
        // TODO: how to handle collisions? Closed hashing? Do we do that here?
        //  Should we have access to sender and currend node here to check if the
        //  hashed node is full
        val bytes = encodeKeyAsBytes(kvPair.key)
        return ((MurmurHash3.hash32x86(bytes, 0, bytes.size, 0) % nodeCount) + nodeCount) % nodeCount
    }

    fun secondaryHash(kvPair: KeyVersionPair): NodeId {
        return (primaryHash(kvPair) + 1) % nodeCount
    }

    private fun encodeKeyAsBytes(key: String): ByteArray {
        return key.encodeToByteArray()
    }
}