package node

import NodeId
import cache.distributed.IDistributedCache
import cache.local.ILocalCache
import sender.ISender
import cache.distributed.DistributedCache
import cache.local.LocalCache
import receiver.IReceiver
import receiver.Receiver
import sender.Sender

/**
 * Node class that intermediates between the receiver, sender, local cache, and
 * distributed cache.
 */
class Node(nodeId: NodeId, nodeCount: Int, capacity: Int) {

    /**
     * The local cache
     */
    private val localCache: ILocalCache

    /**
     * The distributed cache
     */
    private val distributedCache: IDistributedCache

    /**
     * The sender, used to send requests to other nodes
     */
    private val sender: ISender

    /**
     * The receiver, used to receive requests from users and other nodes
     */
    private val receiver: IReceiver

    init {
        print("Initializing node $nodeId on port ${7070 + nodeId} with cache capacity $capacity\n")
        localCache = LocalCache(capacity)
        sender = Sender(nodeId)
        distributedCache = DistributedCache(nodeId, nodeCount, localCache, sender)
        receiver = Receiver(nodeId, distributedCache)
    }

    /**
     * Start the node
     */
    fun start() {
        receiver.start()
    }

}