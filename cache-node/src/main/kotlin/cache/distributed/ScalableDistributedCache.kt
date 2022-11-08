import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.CacheInfo
import cache.local.ILocalEvictingCache
import cache.local.ILocalScalableCache
import cache.local.LocalScalableCache
import exception.KeyNotFoundException
import org.w3c.dom.NodeList
import sender.IScalableSender
import sender.ISender
import sender.ScalableSender
import sender.SenderUsageInfo
import java.util.*
import kotlin.collections.ArrayList

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeList: MutableList<String>):
    IScalableDistributedCache {

    private var nodeCount = nodeList.size

    private val nodeHasher = NodeHasher(nodeCount)

    private val cache = LocalScalableCache(nodeHasher)

    private val sender = ScalableSender(nodeId, nodeList)

    private var sortedNodes: ArrayList<Pair<Int, Int>> = ArrayList()

    /**
     * Flag indicating whether the current node is copying to the new node
     */
    private var copyInProgress = false

    private val copySize = 10

    private val copyRetryCount = 3

    private val copyComplete = MutableList(nodeCount) { false }

    private var copyCompleteCount = 0

    private lateinit var copyThread: Thread

    init {
        for (i in 1..nodeCount) {
            sortedNodes.add(Pair(i, nodeHasher.nodeHashValue(i)))
        }
        sortedNodes.sortBy { it.second }
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("SCALABLE CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

        val primaryNodeIndex = findPrimaryNode(hashValue)
        val primaryNodeId = sortedNodes.get(primaryNodeIndex).second
        val oldPrimaryNodeId = sortedNodes.get((primaryNodeIndex + 1) % sortedNodes.size).second

        val value: ByteArray? = if (!copyInProgress || primaryNodeId != nodeCount - 1 || nodeId != oldPrimaryNodeId) {
            // Normal case
            if (nodeId == primaryNodeId) cache.fetch(kvPair) else sender.fetchFromNode(kvPair, primaryNodeId)
        } else {
            // This key is be copying to new node, check both locations
            cache.fetch(kvPair) ?: sender.fetchFromNode(kvPair, primaryNodeId)
        }

        if (value == null) {
            throw KeyNotFoundException(kvPair.key)
        }
        return value
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray, senderId: NodeId?) {

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("SCALABLE CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

        val primaryNodeId = findPrimaryNode(hashValue)

        // Always
        if (nodeId == primaryNodeId) {
            // Always store to new location, even during copying
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
        }
    }

    override fun getCacheInfo(): CacheInfo {
        return cache.getCacheInfo()
    }

    override fun getSenderInfo(): SenderUsageInfo {
        return sender.getSenderUsageInfo()
    }

    override fun broadcastLaunchIntentions() {
        val success = sender.broadcastScalableMessage(ScalableMessage(nodeId, nodeList[nodeId], ScalableMessageType.LAUNCH_NODE))
        print("SCALABLE CACHE: Result of broadcasting launch intentions was $success\n")
    }

    override fun markCopyComplete(nodeId: NodeId) {
        if (!copyComplete[nodeId]) {
            copyComplete[nodeId] = true
            copyCompleteCount++
            if (copyCompleteCount == nodeCount - 1) {
                sender.broadcastScalableMessage(ScalableMessage(nodeId, nodeList[nodeId], ScalableMessageType.SCALE_COMPLETE))
            }
        }
    }

    override fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>) {
        for (kvPair in kvPairs) {
            cache.store(KeyVersionPair(kvPair.key, kvPair.version), kvPair.value)
        }
    }

    override fun initiateCopy(hostName: String) {
        if (!copyInProgress) {
            copyInProgress = true
            val newHashValue = nodeHasher.nodeHashValue(nodeCount)
            val nextIndex = findPrimaryNode(newHashValue)
            val nextHashValue = sortedNodes.get(nextIndex).first

            // Add hash value of new node to sorted node values
            val updatedNodes = ArrayList<Pair<Int, Int>>()
            updatedNodes.addAll(sortedNodes.subList(0, nextIndex))
            updatedNodes.add(Pair(nodeCount, newHashValue)) // Place new node hash value
            updatedNodes.addAll(sortedNodes.subList(nextIndex, sortedNodes.size))

            // Update the sorted nodes and node count
            sortedNodes = updatedNodes
            nodeList.add(hostName)
            nodeCount = nodeList.size

            // Start the thread to copy asynchronously
            copyThread = Thread { copyKeysByHashValues(newHashValue, nextHashValue) }
            copyThread.start()
        }
    }

    private fun copyKeysByHashValues(start: Int, end: Int) {
        cache.initializeCopy(start, end)
        var kvPairs: MutableList<KeyValuePair>
        var retryRemaining: Int
        // TODO: Move retry logic to sender
        do {
            retryRemaining = copyRetryCount
            kvPairs = cache.streamCopyKeys(copySize)
            while (kvPairs.size > 0 && retryRemaining > 0 && !sender.sendBulkCopy(BulkCopyRequest(nodeId, kvPairs),  nodeCount)) {
                retryRemaining--
            }

        } while(kvPairs.size == copySize)
        copyInProgress = false
    }

    private fun findPrimaryNode(hashValue: Int) : NodeId {
        var i = 0
        var j = sortedNodes.size
        var k = 0
        while (i < j) {
            k = (i + j).floorDiv(2)
            if (hashValue < sortedNodes[k].second) {
                i = k
            } else {
                j = i
            }
        }
        return if (k == sortedNodes.size) 0 else k
    }
}