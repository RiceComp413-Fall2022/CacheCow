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

class Node(private val nodeId: NodeId, nodeList: List<String>, port: Int, capacity: Int, scalable: Boolean) {
    /**
     * The local cache
     */
    private val localCache: ILocalCache

    /**
     * The distributed cache
     */
    private var distributedCache: IDistributedCache

    /**
     * The sender, used to send requests to other nodes
     */
    private var sender: ISender

    /**
     * The receiver, used to receive requests from users and other nodes
     */
    private val receiver: Receiver

    init {
        print("Initializing node $nodeId on port $port with cache capacity $capacity\n")
        val nodeHasher: INodeHasher = NodeHasher(nodeList.size)
        if (scalable) {
            localCache = LocalScalableCache(nodeHasher)
            sender = ScalableSender(nodeId, nodeList)
            distributedCache = ScalableDistributedCache(nodeId, nodeList.size, nodeHasher, localCache,
                sender as IScalableSender
            )
            receiver = ScalableReceiver(port, nodeList.size, this, distributedCache as IScalableDistributedCache)
        } else {
            localCache = LocalCache(capacity)
            sender = Sender(nodeId, nodeList)
            distributedCache = DistributedCache(nodeId, nodeList.size, localCache, sender)
            receiver = Receiver(port, nodeList.size, this, distributedCache)
        }
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
        val usage = allocatedMemory/(maxMemory * 1.0)
        return NodeInfo(nodeId,
                        MemoryUsageInfo(allocatedMemory, maxMemory, usage),
                        localCache.getCacheInfo(),
                        receiver.getReceiverUsageInfo(),
                        sender.getSenderUsageInfo())
    }

}

/**
 * Client response giving memory usage of the JVM.
 */
data class MemoryUsageInfo(val allocated: Long, val max: Long, val usage: Double)

/**
 * Encapsulates information about the usage of this node into one object
 */
data class NodeInfo(val nodeId: Int,
                    val memUsage: MemoryUsageInfo,
                    val cacheInfo: CacheInfo,
                    val receiverUsageInfo: ReceiverUsageInfo,
                    val senderUsageInfo: SenderUsageInfo)