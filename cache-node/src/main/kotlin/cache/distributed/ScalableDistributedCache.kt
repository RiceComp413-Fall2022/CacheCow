import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.NodeHasher
import cache.distributed.launcher.LocalNodeLauncher
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

    private val nodeLauncher = LocalNodeLauncher()

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

    private lateinit var scaleThread: Thread

    init {
        for (i in 0 until nodeCount) {
            sortedNodes[nodeHasher.nodeHashValue(i)] = i
        }

        // If the node was just booted up
        if (nodeId == nodeCount) {
            sortedNodes[nodeHasher.nodeHashValue(nodeId)] = nodeId
            scaleThread = Thread { sender.broadcastScalableMessage(ScalableMessage(nodeId, "localhost:${7070 + nodeId}", ScalableMessageType.READY)) }
            scaleThread.start()
        }
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("SCALABLE CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

        val primaryNodeId = findPrimaryNode(hashValue)

        print("SCALABLE CACHE: Primary node id is $primaryNodeId\n")
        val prevPrimaryNodeId = findPrevPrimaryNode(hashValue)

        val value: ByteArray? = if (!copyInProgress || primaryNodeId != nodeCount - 1 || nodeId != prevPrimaryNodeId) {
            // Normal case
            print("SCALABLE CACHE: Fetch entered normal case\n")
            if (nodeId == primaryNodeId) cache.fetch(kvPair) else sender.fetchFromNode(kvPair, primaryNodeId)
        } else {
            // This key is being copied to new node, check both locations
            print("SCALABLE CACHE: Fetch entered copy case\n")
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
        print("SCALABLE CACHE: Primary node id is $primaryNodeId\n")

        if (nodeId == primaryNodeId) {
            print("SCALABLE CACHE: Store entered local case\n")
            // Always store to new location, even during copying
            cache.store(kvPair, value)
        } else {
            print("SCALABLE CACHE: Store entered remote case\n")
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
        // val success = sender.broadcastScalableMessage(ScalableMessage(nodeId, "", ScalableMessageType.LAUNCH_NODE))
        scaleThread = Thread { sender.broadcastScalableMessageAsync(ScalableMessage(nodeId, "", ScalableMessageType.LAUNCH_NODE)) }
        scaleThread.start()
        print("SCALABLE CACHE: Created and running broadcast thread")
    }

    override fun markCopyComplete(senderId: NodeId) {
        print("SCALABLE CACHE: Marking that node $senderId has completed copying\n")
        if (!copyComplete[senderId]) {
            copyComplete[senderId] = true
            copyCompleteCount++
            print("SCALABLE CACHE: Complete count is now $copyCompleteCount out of $nodeCount\n")
            if (copyCompleteCount == nodeCount) {
                print("SCALABLE CACHE: Going to broadcast SCALE_COMPLETE message\n")
                scaleThread = Thread { sender.broadcastScalableMessage(ScalableMessage(nodeId, "", ScalableMessageType.SCALE_COMPLETE)) }
                scaleThread.start()
            }
        }
    }

    override fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>) {
        print("SCALABLE CACHE: Completing bulk local store with ${kvPairs.size} pairs\n")
        for (kvPair in kvPairs) {
            cache.store(KeyVersionPair(kvPair.key, kvPair.version), kvPair.value)
        }
    }

    override fun initiateLaunch(): Boolean {
        return nodeLauncher.launchNode(nodeCount)
    }

    override fun initiateCopy(hostName: String) {
        print("SCALABLE CACHE: Beginning copying process\n")
        if (!copyInProgress) {
            copyInProgress = true
            val newHashValue = nodeHasher.nodeHashValue(nodeCount)

            // Update the sorted nodes and node count
            sortedNodes[newHashValue] = nodeCount
            nodeList.add(hostName)
            nodeCount = nodeList.size
            printSortedNodes()

            // Start the thread to copy asynchronously
            scaleThread = Thread { copyKeysByHashValues(nodeHasher.nodeHashValue(nodeId), newHashValue) }
            scaleThread.start()
        }
    }

    private fun copyKeysByHashValues(start: Int, end: Int) {
        Thread.sleep(5_000)
        cache.initializeCopy(start, end)
        var kvPairs: MutableList<KeyValuePair>
        do {
            kvPairs = cache.streamCopyKeys(copyBatchSize)
            if (kvPairs.size > 0) {
                sender.sendBulkCopy(BulkCopyRequest(nodeId, kvPairs),  nodeCount - 1)
            }

        } while(kvPairs.size == copyBatchSize)
        sender.sendScalableMessage(ScalableMessage(nodeId, "", ScalableMessageType.COPY_COMPLETE), nodeCount - 1)
        cache.cleanupCopy()
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

    private fun printSortedNodes() {
        print("There are ${sortedNodes.size} sorted nodes\n")
        for (pair in sortedNodes.asIterable()) {
            print("Hash value ${pair.key} and node id ${pair.value}\n")
        }
    }
}