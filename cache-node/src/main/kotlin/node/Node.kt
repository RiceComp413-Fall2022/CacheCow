package node

import NodeId
import cache.local.ILocalCache
import sender.ISender
import cache.distributed.DistributedCache
import cache.local.CacheInfo
import cache.local.LocalCache
import cache.local.LocalEvictingCache
import receiver.Receiver
import receiver.ReceiverUsageInfo
import sender.Sender
import sender.SenderUsageInfo

/**
 * Node class that intermediates between the receiver, sender, local cache, and
 * distributed cache.
 */

open class Node(private val nodeId: NodeId, nodeList: List<String>, port: Int, capacity: Int) {
    /**
     * The local cache
     */
    private val localCache: ILocalCache

    /**
     * The distributed cache
     */
    var distributedCache: DistributedCache

    /**
     * The sender, used to send requests to other nodes
     */
    var sender: ISender

    /**
     * The receiver, used to receive requests from users and other nodes
     */
    val receiver: Receiver

    init {
        print("Initializing node $nodeId on port $port with cache capacity $capacity\n")
        localCache = LocalEvictingCache(capacity)
        sender = Sender(nodeId, nodeList)
        distributedCache = DistributedCache(nodeId, nodeList.size, localCache, sender)
        receiver = Receiver(port, nodeList.size, this, distributedCache)
    }

    /**
     * Start the node
     */
    fun start() {
        receiver.start()
    }

    /**
     * Gets node info for this node, including memory usage, cacheInfo
     * TODO: also process sender and receiver usage info
     */
    fun getNodeInfo(): NodeInfo {
        val runtime = Runtime.getRuntime()
        val allocatedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return NodeInfo(nodeId,
            MemoryUsageInfo(allocatedMemory, maxMemory),
            localCache.getCacheInfo(),
            receiver.getReceiverUsageInfo(),
            sender.getSenderUsageInfo())
    }

}

/**
 * Client response giving memory usage of the JVM.
 */
data class MemoryUsageInfo(val usedMemory: Long, val maxMemory: Long)

/**
 * Encapsulates information about the usage of this node into one object
 */
data class NodeInfo(val nodeId: Int,
                    val memUsage: MemoryUsageInfo,
                    val cacheInfo: CacheInfo,
                    val receiverUsageInfo: ReceiverUsageInfo,
                    val senderUsageInfo: SenderUsageInfo)