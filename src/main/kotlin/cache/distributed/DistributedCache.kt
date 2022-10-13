package cache.distributed

import KeyVersionPair
import cache.distributed.hasher.NodeHasher
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.local.LocalCache
import sender.Sender

/**
 * Implementation of IReceiverService that fetches and stores cache data by delegating to
 * the primary node for that data.
 */
class DistributedCache(private val nodeId: NodeId, nodeCount: Int, private val cache: LocalCache, private val sender: Sender):
    IDistributedCache {

    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

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

}