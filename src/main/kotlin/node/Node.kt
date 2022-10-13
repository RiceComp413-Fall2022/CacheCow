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
 * Node class that contains the receiver, sender and memory cache.
 */
class Node(nodeId: NodeId, nodeCount: Int, capacity: Int) {

    private val localCache: ILocalCache
    private val distributedCache: IDistributedCache
    private val sender: ISender
    private val receiver: IReceiver

    init {
        print("Initializing node $nodeId on port ${7070 + nodeId} with cache capacity $capacity\n")
        localCache = LocalCache(capacity)
        sender = Sender(nodeId)
        distributedCache = DistributedCache(nodeId, nodeCount, localCache, sender)
        receiver = Receiver(nodeId, distributedCache)
    }

    fun start() {
        receiver.start()
    }

}