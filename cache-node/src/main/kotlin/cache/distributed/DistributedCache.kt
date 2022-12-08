package cache.distributed

import KeyVersionPair
import NodeId
import cache.distributed.IDistributedCache.SystemInfo
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.ILocalCache
import io.javalin.Javalin
import receiver.Receiver
import sender.ISender
import sender.Sender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, private var nodeList: List<String>,
                       private var cache: ILocalCache): IDistributedCache,
    ITestableDistributedCache<ISender> {

    /**
     * The node hasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeList.size)

    /**
     * Receiver implementation
     */
    private val receiver =  Receiver(nodeList.size, this)

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

    override fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            nodeId,
            nodeList.get(nodeId),
            getMemoryUsage(),
            cache.getCacheInfo(),
            receiver.getReceiverUsageInfo(),
            sender.getSenderUsageInfo(),
            receiver.getClientRequestTiming(),
            receiver.getServerRequestTiming()
        )
    }

    override fun getGlobalSystemInfo(): MutableList<SystemInfo> {
        val globalInfo = mutableListOf<SystemInfo>()
        for (destNodeId in nodeList.indices) {
            if (destNodeId == nodeId) {
                globalInfo.add(getSystemInfo())
            } else {
                globalInfo.add(sender.getCacheInfo(destNodeId))
            }
        }
        return globalInfo
    }

    override fun mockSender(mockSender: ISender) {
        sender = mockSender
    }

    override fun getJavalinApp(): Javalin {
        return receiver.getJavalinApp()
    }
}