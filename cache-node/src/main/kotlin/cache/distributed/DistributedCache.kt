package cache.distributed

import KeyVersionPair
import cache.distributed.hasher.NodeHasher
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.local.ILocalCache
import cache.local.LocalCache
import exception.KeyNotFoundException
import sender.ISender
import sender.Sender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, nodeCount: Int, private val cache: ILocalCache, var sender: ISender):
    IDistributedCache {

    /**
     * The INodeHasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        val value: ByteArray? = if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(
                kvPair,
                primaryNodeId
            )
        }
        if (value == null) {
            throw KeyNotFoundException(kvPair.key)
        }
        return value
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray, senderId: NodeId?) {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
        }
    }

    override fun testCopy() {
        TODO("Remove")
    }

    override fun bulkLocalStore(kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>) {
        TODO("Remove")
    }


}