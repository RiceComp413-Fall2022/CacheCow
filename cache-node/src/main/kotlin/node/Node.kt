package node

import NodeId
import ScalableDistributedCache
import cache.local.ILocalCache
import cache.distributed.DistributedCache
import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.CacheInfo
import cache.local.LocalCache
import cache.local.LocalScalableCache
import receiver.Receiver
import receiver.ReceiverUsageInfo
import receiver.ScalableReceiver
import sender.*

/**
 * Node class that intermediates between the receiver, sender, local cache, and
 * distributed cache.
 */

class Node(nodeId: NodeId, nodeList: MutableList<String>, port: Int, capacity: Int, scalable: Boolean) {

    /**
     * The distributed cache
     */
    private var distributedCache: IDistributedCache

    /**
     * The receiver, used to receive requests from users and other nodes
     */
    private val receiver: Receiver

    init {
        print("Initializing node $nodeId on port $port with cache capacity $capacity\n")
        if (scalable) {
            distributedCache = ScalableDistributedCache(nodeId, nodeList)
            receiver = ScalableReceiver(port, nodeId, nodeList.size, distributedCache as IScalableDistributedCache)
        } else {
            distributedCache = DistributedCache(nodeId, nodeList)
            receiver = Receiver(port, nodeId, nodeList.size, distributedCache)
        }
    }

    /**
     * Start the node
     */
    fun start() {
        receiver.start()
    }
}