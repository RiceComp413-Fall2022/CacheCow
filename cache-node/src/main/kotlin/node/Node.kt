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
     * The distributed cache (public for testing)
     */
    var distributedCache: IDistributedCache

    /**
     * The receiver, used to receive requests from users and other nodes (public for testing)
     */
    var receiver: Receiver

    init {
        print("Initializing node $nodeId on port $port\n")
        if (scalable) {
            val prevNodeCount: Int
            val isNewNode: Boolean = if (nodeId >= nodeList.size) {
                // TODO: Support new node on 2nd, 3rd, etc. boot
                for (i in nodeList.size..nodeId) {
                    nodeList.add("localhost:${6060 + i}")
                }
                prevNodeCount = nodeList.size - 1
                true
            } else {
                prevNodeCount = nodeList.size
                false
            }
            distributedCache = ScalableDistributedCache(nodeId, nodeList, isNewNode)
            receiver = ScalableReceiver(port, nodeId, prevNodeCount, distributedCache as IScalableDistributedCache)
        } else {
            distributedCache = DistributedCache(nodeId, nodeList)
            receiver = Receiver(port, nodeList.size, distributedCache)
        }
    }

    /**
     * Start the node
     */
    fun start() {
        receiver.start()
    }
}