package cache.distributed

import KeyVersionPair
import NodeId
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.ILocalCache
import cache.local.LocalCache
import exception.KeyNotFoundException
import io.javalin.Javalin
import receiver.IReceiver
import receiver.Receiver
import sender.ISender
import sender.Sender

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class DistributedCache(private val nodeId: NodeId, nodeList: List<String>): IDistributedCache,
    ITestableDistributedCache<ISender> {

    /**
     * The INodeHasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeList.size)

    /**
     * Local cache implementation
     */
    private val cache: ILocalCache = LocalCache()

    /**
     * Receiver implementation
     */
    private val receiver: IReceiver = Receiver(nodeId, this)

    /**
     * Module used to send all out-going messages (public for testing)
     */
    private var sender: ISender = Sender(nodeId, nodeList)

    override fun start(port: Int) {
        receiver.start(port)
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        val value: ByteArray? = if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(
                kvPair,
                primaryNodeId
            )
        }
        if (value == null) {
            throw KeyNotFoundException(kvPair.key)
        }
        return value
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray, senderId: NodeId?) {
        val primaryNodeId = nodeHasher.primaryHashNode(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
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

    override fun getApp(): Javalin {
        return receiver.getApp()
    }
}