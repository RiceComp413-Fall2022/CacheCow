package cache.distributed.hasher

import KeyVersionPair
import NodeId
import org.apache.commons.codec.digest.MurmurHash3

/**
 * A concrete node hasher that uses the MurmurHash3 hashing algorithm
 */
class NodeHasher(private val nodeCount: Int) : INodeHasher {

    override fun primaryHash(kvPair: KeyVersionPair): NodeId {
        val bytes = kvPair.key.encodeToByteArray()
        return ((MurmurHash3.hash32x86(bytes, 0, bytes.size, 0) % nodeCount) + nodeCount) % nodeCount
    }

}