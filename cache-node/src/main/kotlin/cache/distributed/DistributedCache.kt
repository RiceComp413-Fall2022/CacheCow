package cache.distributed

import KeyVersionPair
import cache.distributed.hasher.NodeHasher
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.local.ILocalCache
import sender.ISender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, private val nodeCount: Int, private val cache: ILocalCache, var sender: ISender):
    IDistributedCache {

    /**
     * The node hasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(kvPair, primaryNodeId)
        }
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(kvPair, value, primaryNodeId)
        }
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.remove(kvPair)
        } else {
            sender.removeFromNode(kvPair,primaryNodeId)
        }
    }

    override fun clearAll() {
        for (primaryNodeId in 0 until nodeCount ) {
            if (primaryNodeId == nodeId) {
                cache.clearAll()
            } else {
                sender.clearNode(primaryNodeId)
            }
        }
    }
}