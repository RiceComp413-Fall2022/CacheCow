package cache.distributed

import KeyVersionPair
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.ILocalCache
import cache.local.LocalCache
import io.javalin.Javalin
import receiver.Receiver
import sender.ISender
import sender.Sender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, private var nodeList: List<String>): IDistributedCache,
    ITestableDistributedCache<ISender> {

    /**
     * The node hasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeList.size)

    /**
     * Local cache implementation
     */
    private val cache: ILocalCache = LocalCache()

    /**
     * Receiver implementation
     */
    private val receiver =  Receiver(nodeId, this)

    /**
     * Module used to send all out-going messages (public for testing)
     */
    private var sender: ISender = Sender(nodeId, nodeList)

    override fun start(port: Int) {
        receiver.start(port)
    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(kvPair, primaryNodeId)
        }
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(kvPair, value, primaryNodeId)
        }
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        return if (nodeId == primaryNodeId) {
            cache.remove(kvPair)
        } else {
            sender.removeFromNode(kvPair,primaryNodeId)
        }
    }

    override fun clearAll(isClientRequest: Boolean) {
        cache.clearAll(isClientRequest)
        if (isClientRequest) {
            for (primaryNodeId in nodeList.indices) {
                if (primaryNodeId != nodeId) {
                    sender.clearNode(primaryNodeId)
                }
            }
        }
    }

    override fun getSystemInfo(): IDistributedCache.SystemInfo {
        return IDistributedCache.SystemInfo(
            nodeId,
            getMemoryUsage(),
            cache.getCacheInfo(),
            receiver.getReceiverUsageInfo(),
            sender.getSenderUsageInfo()
        )
    }
    override fun mockSender(mockSender: ISender) {
        sender = mockSender
    }

    override fun getJavalinApp(): Javalin {
        return receiver.getJavalinApp()
    }
}