package cache.distributed.hasher

import KeyVersionPair
import NodeId
import org.apache.commons.codec.digest.MurmurHash3

/**
 * A concrete node hasher that uses the MurmurHash3 hashing algorithm.
 */
class NodeHasher(private val nodeCount: Int, private val seed: Int = 0) : INodeHasher {

    override fun primaryHashValue(kvPair: KeyVersionPair): Int {
        val bytes = kvPair.key.encodeToByteArray()
        return MurmurHash3.hash32x86(bytes, 0, bytes.size, seed)
    }

    override fun nodeHashValue(nodeId: NodeId): Int {
        return MurmurHash3.hash32x86(byteArrayOf(nodeId.toByte()), 0, 1, seed)
    }

    override fun extendedNodeHashValue(nodeId: NodeId, index: Int): Int {
        val byteArray = byteArrayOf(nodeId.toByte(), index.toByte())
        return MurmurHash3.hash32x86(byteArray, 0, byteArray.size, seed)
    }

    override fun primaryHashNode(kvPair: KeyVersionPair): NodeId {
        return ((primaryHashValue(kvPair) % nodeCount) + nodeCount) % nodeCount
    }

}