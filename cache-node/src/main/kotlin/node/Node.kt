package node

import NodeId
import ScalableDistributedCache
import cache.distributed.DistributedCache
import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import receiver.Receiver
import receiver.ScalableReceiver

/**
 * Node class that intermediates between the receiver, sender, local cache, and
 * distributed cache.
 */

class Node(nodeId: NodeId, nodeList: MutableList<String>, port: Int, scalable: Boolean) {

    /**
     * The distributed cache
     */
    private var distributedCache: IDistributedCache

    /**
     * The receiver, used to receive requests from users and other nodes
     */
    private val receiver: Receiver

    init {
        print("Initializing node $nodeId on port $port\n")
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