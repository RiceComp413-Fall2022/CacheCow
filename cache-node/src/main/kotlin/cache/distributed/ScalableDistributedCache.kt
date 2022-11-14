import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.ConsistentKeyDistributor
import cache.distributed.hasher.NodeHasher
import cache.distributed.launcher.LocalNodeLauncher
import cache.local.LocalScalableCache
import exception.KeyNotFoundException
import receiver.SystemInfo
import sender.ScalableSender
import java.util.*

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeList: MutableList<String>):
    IScalableDistributedCache {

    private val isNewNode = nodeId == nodeList.size

    /**
     * Updated immediately once copying starts to support re-routing
     */
    private var nodeCount = if (isNewNode) nodeList.size else nodeList.size + 1

    /**
     * Computes all hash values used by cache
     */
    private val nodeHasher = NodeHasher(nodeCount)

    /**
     * Supports launching a new node
     */
    private val nodeLauncher = LocalNodeLauncher()

    /**
     * Local cache implementation
     */
    private val cache = LocalScalableCache(nodeHasher)

    /**
     * Module used to send all out-going messages
     */
    private val sender = ScalableSender(nodeId, nodeList)

    /**
     * Module used to determine how keys should be distributed across machines
     */
    private val keyDistributor = ConsistentKeyDistributor(nodeId, nodeCount, 1)

    /**
     * Flag indicating whether the current node is copying to the new node
     */
    private var scaleInProgress = isNewNode

    /**
     * Flag indicating whether the current node intends to launch
     */
    private var desireToLaunch = false

    /**
     * Timer used to ensure that nodes achieve launching consensus
     */
    private var launchTimer = Timer()

    /**
     * Minimum id of a node that is currently intending to launch another node
     */
    private var minLaunchingNode = nodeCount

    /**
     * Flag indicating whether this node is currently copying its data
     */
    private var copyInProgress = false

    /**
     * Number of key-value pairs copied in a single bulk-copy request
     */
    private val copyBatchSize = 10

    /**
     * Used by a newly booted node to track which other nodes have completed copying
     */
    private val copyComplete = MutableList(nodeCount) { false }

    /**
     * Number of nodes that have completed copying
     */
    private var copyCompleteCount = 0

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {

        val nodeIds = keyDistributor.getPrimaryAndPrevNode(kvPair)
        val primaryNodeId = nodeIds.first
        val prevNodeId = nodeIds.second

        print("SCALABLE CACHE: Primary node id is $primaryNodeId\n")

        val value: ByteArray? =
            if (!copyInProgress || primaryNodeId != nodeCount - 1 || nodeId != prevNodeId) {
                // Normal case
                print("SCALABLE CACHE: Fetch entered normal case\n")
                if (nodeId == primaryNodeId) cache.fetch(kvPair) else sender.fetchFromNode(
                    kvPair,
                    primaryNodeId
                )
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

        val primaryNodeId = keyDistributor.getPrimaryNode(kvPair)
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

    override fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            nodeId,
            getMemoryUsage(),
            cache.getCacheInfo(),
            null,
            sender.getSenderUsageInfo())
    }

    override fun start() {
        if (nodeId == nodeCount) {
            Thread {
                sender.broadcastScalableMessage(
                    ScalableMessage(
                        nodeId,
                        "localhost:${7070 + nodeId}",
                        ScalableMessageType.READY
                    )
                )
            }.start()
        }
    }

    override fun handleLaunchRequest(senderId: NodeId): Boolean {
        scaleInProgress = true
        if (senderId < minLaunchingNode) {
            minLaunchingNode = senderId
            if (desireToLaunch) {
                desireToLaunch = false
                launchTimer.cancel()
            }
            return true
        }
        return false
    }

    override fun scaleInProgress(): Boolean {
        return scaleInProgress
    }

    override fun handleScaleCompleteRequest() {
        scaleInProgress = false
        minLaunchingNode = nodeCount
    }

    override fun initiateLaunch() {
        if (minLaunchingNode == nodeCount) {
            desireToLaunch = true
            scaleInProgress = true

            // Broadcast launch intentions to all other nodes (should be async)
            Thread {
                sender.broadcastScalableMessageAsync(
                    ScalableMessage(
                        nodeId,
                        "",
                        ScalableMessageType.LAUNCH_NODE
                    )
                )
            }.start()
            print("SCALABLE CACHE: Created and running broadcast thread")

            // If this node has minimum node id, launch new node
            launchTimer.schedule(object : TimerTask() {
                override fun run() {
                    print("SCALABLE SENDER: Launching new node\n")
                    nodeLauncher.launchNode(nodeCount)
                }
            }, 2 * 1000)
        }
    }

    override fun markCopyComplete(senderId: NodeId) {
        print("SCALABLE CACHE: Marking that node $senderId has completed copying\n")
        if (!copyComplete[senderId]) {
            copyComplete[senderId] = true
            copyCompleteCount++
            print("SCALABLE CACHE: Complete count is now $copyCompleteCount out of $nodeCount\n")
            if (copyCompleteCount == nodeCount) {
                print("SCALABLE CACHE: Going to broadcast SCALE_COMPLETE message\n")
                Thread {
                    sender.broadcastScalableMessage(
                        ScalableMessage(
                            nodeId,
                            "",
                            ScalableMessageType.SCALE_COMPLETE
                        )
                    )
                }.start()
            }
        }
    }

    override fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>) {
        print("SCALABLE CACHE: Completing bulk local store with ${kvPairs.size} pairs\n")
        for (kvPair in kvPairs) {
            cache.store(KeyVersionPair(kvPair.key, kvPair.version), kvPair.value)
        }
    }

    override fun initiateCopy(newHostName: String) {
        print("SCALABLE CACHE: Beginning copying process\n")
        if (!copyInProgress) {

            // Update state to reflect new node
            copyInProgress = true
            nodeList.add(newHostName)
            sender.addHost(newHostName)
            nodeCount++

            // Find range of keys to copy
            val copyRange = keyDistributor.addNode()

            // Start the thread to copy asynchronously
            Thread { copyKeysByHashValues(copyRange) }.start()
        }
    }

    private fun copyKeysByHashValues(copyRange: Pair<Int, Int>) {
        cache.initializeCopy(copyRange.first, copyRange.second)
        var kvPairs: MutableList<KeyValuePair>
        do {
            kvPairs = cache.streamCopyKeys(copyBatchSize)
            if (kvPairs.size > 0) {
                sender.sendBulkCopy(BulkCopyRequest(nodeId, kvPairs), nodeCount - 1)
            }

        } while (kvPairs.size == copyBatchSize)
        sender.sendScalableMessage(
            ScalableMessage(
                nodeId,
                "",
                ScalableMessageType.COPY_COMPLETE
            ), nodeCount - 1
        )
        cache.cleanupCopy()
        copyInProgress = false
    }
}