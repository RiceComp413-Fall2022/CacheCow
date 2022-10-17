package cache.distributed

import KeyVersionPair
import cache.distributed.hasher.NodeHasher
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.local.LocalCache
import sender.Sender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, nodeCount: Int, private val cache: LocalCache, private val sender: Sender):
    IDistributedCache {

    /**
     * The INodeHasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String? {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(
                kvPair,
                primaryNodeId
            )
        }
    }

    override fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?): Boolean {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
        }
    }
}