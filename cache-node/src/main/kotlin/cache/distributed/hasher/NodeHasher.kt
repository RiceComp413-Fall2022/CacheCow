package cache.distributed.hasher

import KeyVersionPair
import NodeId
import org.apache.commons.codec.digest.MurmurHash3

/**
 * A concrete node hasher that uses the MurmurHash3 hashing algorithm
 */
class NodeHasher(private val nodeCount: Int) : INodeHasher {

    override fun primaryHashValue(kvPair: KeyVersionPair): Int {
        val bytes = kvPair.key.encodeToByteArray()
        return MurmurHash3.hash32x86(bytes, 0, bytes.size, 0)
    }

    override fun nodeHashValue(nodeId: NodeId): Int {
        return MurmurHash3.hash32x86(byteArrayOf(nodeId.toByte()), 0, 1, 0)
    }

    override fun primaryHashNode(kvPair: KeyVersionPair): NodeId {
        return ((primaryHashValue(kvPair) % nodeCount) + nodeCount) % nodeCount
    }

}