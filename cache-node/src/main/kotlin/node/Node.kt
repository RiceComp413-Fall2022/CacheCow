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

class Node(nodeId: NodeId, nodeList: MutableList<String>, port: Int, isAWS: Boolean, scalable: Boolean, newNode: Boolean) {

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
            var isNewNode = newNode
            if (!isNewNode && nodeId >= nodeList.size) {
                // Run on localhost
                isNewNode = true
                for (i in nodeList.size..nodeId) {
                    nodeList.add("localhost:${7070 + i}")
                }
                prevNodeCount = nodeList.size - 1
            } else {
                prevNodeCount = nodeList.size
            }

            distributedCache = ScalableDistributedCache(nodeId, nodeList, isAWS, isNewNode)
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