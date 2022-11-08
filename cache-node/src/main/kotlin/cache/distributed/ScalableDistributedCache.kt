import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.NodeHasher
import cache.local.CacheInfo
import cache.local.LocalScalableCache
import exception.KeyNotFoundException
import sender.ScalableSender
import sender.SenderUsageInfo
import java.util.*

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeList: MutableList<String>):
    IScalableDistributedCache {

    /**
     * Updated immediately once copying starts to support re-routing
     */
    private var nodeCount = nodeList.size

    private val nodeHasher = NodeHasher(nodeCount)

    private val cache = LocalScalableCache(nodeHasher)

    private val sender = ScalableSender(nodeId, nodeList)

    private var sortedNodes: SortedMap<Int, Int> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    /**
     * Flag indicating whether the current node is copying to the new node
     */
    private var copyInProgress = false

    private val copyBatchSize = 10

    private val copyComplete = MutableList(nodeCount) { false }

    private var copyCompleteCount = 0

    private lateinit var copyThread: Thread

    init {
        for (i in 1..nodeCount) {
            sortedNodes[nodeHasher.nodeHashValue(i)] = i
        }
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("SCALABLE CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

        val primaryNodeId = findPrimaryNode(hashValue)
        val prevPrimaryNodeId = findPrevPrimaryNode(hashValue)

        val value: ByteArray? = if (!copyInProgress || primaryNodeId != nodeCount - 1 || nodeId != prevPrimaryNodeId) {
            // Normal case
            if (nodeId == primaryNodeId) cache.fetch(kvPair) else sender.fetchFromNode(kvPair, primaryNodeId)
        } else {
            // This key is being copied to new node, check both locations
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
            val nextNodeId = findPrimaryNode(newHashValue)
            val nextHashValue = nodeHasher.nodeHashValue(nextNodeId)

            // Update the sorted nodes and node count
            sortedNodes[nodeCount] = newHashValue
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
        do {
            kvPairs = cache.streamCopyKeys(copyBatchSize)
            sender.sendBulkCopy(BulkCopyRequest(nodeId, kvPairs),  nodeCount)

        } while(kvPairs.size == copyBatchSize)
        copyInProgress = false
    }

    private fun findPrimaryNode(hashValue: Int): NodeId {
        val tailMap = sortedNodes.tailMap(hashValue)
        if (tailMap.isNotEmpty()) {
            return tailMap.iterator().next().value
        }
        return sortedNodes.headMap(hashValue).iterator().next().value
    }

    private fun findPrevPrimaryNode(hashValue: Int): NodeId {
        val headMap = sortedNodes.headMap(hashValue)
        if (headMap.isNotEmpty()) {
            return headMap[headMap.lastKey()]!!
        }
        val tailMap = sortedNodes.tailMap(hashValue)
        return tailMap[tailMap.lastKey()]!!
    }
}