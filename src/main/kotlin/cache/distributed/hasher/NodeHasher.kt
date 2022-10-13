package cache.distributed.hasher

import KeyVersionPair
import NodeId
import org.apache.commons.codec.digest.MurmurHash3

/**
 * Class for determining the same node consistently for a given key,version pair
 */
class NodeHasher(private val nodeCount: Int) : INodeHasher {

    /**
     * Get designated node for key-version pair
     * @param kvPair the key version pair
     * @return the id of the node designated for the key version pair
     */
    override fun primaryHash(kvPair: KeyVersionPair): NodeId {
        val bytes = kvPair.key.encodeToByteArray()
        return ((MurmurHash3.hash32x86(bytes, 0, bytes.size, 0) % nodeCount) + nodeCount) % nodeCount
    }

}