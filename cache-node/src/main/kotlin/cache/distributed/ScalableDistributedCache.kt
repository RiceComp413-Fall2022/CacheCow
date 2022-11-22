import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.ConsistentKeyDistributor
import cache.distributed.hasher.NodeHasher
import cache.distributed.launcher.AWSNodeLauncher
import cache.distributed.launcher.LocalNodeLauncher
import cache.local.LocalScalableCache
import exception.KeyNotFoundException
import sender.ScalableSender
import sender.Sender
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeList: MutableList<String>, isAWS: Boolean, private var isNewNode: Boolean):
    IScalableDistributedCache {

    /**
     * Updated immediately once copying starts to support re-routing
     */
    private var nodeCount = nodeList.size

    /**
     * Gives number of previous nodes is node was just booted
     */
    private val prevNodeCount = if (isNewNode) nodeCount - 1 else nodeCount

    /**
     * Computes all hash values used by cache
     */
    private val nodeHasher = NodeHasher(nodeCount)

    /**
     * Supports launching a new node
     */
    private val nodeLauncher = if (isAWS) AWSNodeLauncher() else LocalNodeLauncher()

    /**
     * Local cache implementation
     */
    private val cache = LocalScalableCache(nodeHasher, this)

    /**
     * Module used to send all out-going messages
     */
    private val sender = ScalableSender(nodeId, nodeList)

    /**
     * Module used to determine how keys should be distributed across machines
     */
    private val keyDistributor = ConsistentKeyDistributor(nodeCount)

    /**
     * Flag indicating whether scaling process is active, atomic to prevent multiple threads
     * from initiating scaling process simultaneously
     */
    private var scaleInProgress = AtomicBoolean(isNewNode)

    /**
     * Flag indicating whether the current node intends to launch
     */
    private var desireToLaunch = AtomicBoolean(false)

    /**
     * Timer used to ensure that nodes achieve launching consensus
     */
    private var launchTimer = Timer()

    /**
     * Minimum id of a node that is currently intending to launch another node
     */
    private var minLaunchingNode = nodeCount

    /**
     * Flag indicating whether this node is currently copying its data, not atomic as we
     * assume other nodes do not send duplicate requests
     */
    private var copyInProgress = false

    /**
     * Number of key-value pairs copied in a single bulk-copy request
     */
    private val copyBatchSize = 10

    /**
     * Used by a newly booted node to track which other nodes have completed copying, not atomic
     * as we assume other nodes do not send duplicate requests
     */
    private val copyComplete = MutableList(prevNodeCount) { false }

    /**
     * Number of nodes that have completed copying, atomic to ensure that the count is correct
     */
    private var copyCompleteCount = AtomicInteger(0)

    /**
     * Number of nodes that have completed copying, atomic to ensure that the count is correct
     */
    private var redistributeLock = ReentrantReadWriteLock()

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

        val initPrimaryNodeId = keyDistributor.getPrimaryNode(kvPair)

        print("SCALABLE CACHE: Primary node id is $initPrimaryNodeId\n")

        val primaryNodeId: NodeId = if (nodeId == initPrimaryNodeId) {
            // Ensure that this doesn't evade copying and store to previous node
            redistributeLock.readLock().lock()
            keyDistributor.getPrimaryNode(kvPair)
        } else {
            // The key will be stored on a different node
            initPrimaryNodeId
        }

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

        if (nodeId == initPrimaryNodeId) {
            // Release the previously acquired lock
            redistributeLock.readLock().unlock()
        }
    }

    override fun mockSender(mock: Sender) {
        TODO("Not yet implemented")
    }

    override fun start() {
        print("SCALABLE CACHE: Started with is new node $isNewNode\n")
        if (isNewNode) {
            Thread {
                sender.broadcastScalableMessageAsync(
                    ScalableMessage(
                        nodeId,
                        nodeList[nodeId],
                        ScalableMessageType.READY
                    )
                )
            }.start()
        }
    }

    override fun getSystemInfo(): IDistributedCache.SystemInfo {
        return IDistributedCache.SystemInfo(
            nodeId,
            getMemoryUsage(),
            cache.getCacheInfo(),
            null,
            sender.getSenderUsageInfo()
        )
    }

    override fun handleLaunchRequest(senderId: NodeId): Boolean {
        scaleInProgress.set(true)
        if (senderId < minLaunchingNode) {
            minLaunchingNode = senderId
            if (desireToLaunch.compareAndSet(true, false)) {
                launchTimer.cancel()
            }
            return true
        }
        return false
    }

    override fun scaleInProgress(): Boolean {
        return scaleInProgress.get()
    }

    override fun handleScaleCompleteRequest() {
        scaleInProgress.set(false)
        minLaunchingNode = nodeCount
    }

    override fun initiateLaunch() {
        print("SCALABLE CACHE: Received request to initiate node launch\n")
        if (minLaunchingNode == nodeCount) {
            // Make sure only one thread can will start broadcast
            if (scaleInProgress.compareAndSet(false, true)) {
                desireToLaunch.set(true)
                print("SCALABLE CACHE: Node list is $nodeList\n")

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
                print("SCALABLE CACHE: Created and running broadcast thread\n")

                // If this node has minimum node id, launch new node
                launchTimer.schedule(object : TimerTask() {
                    override fun run() {
                        print("SCALABLE SENDER: Launching new node\n")
                        nodeLauncher.launchNode(nodeCount)
                    }
                }, 2 * 1000)
            }
        }
    }

    override fun markCopyComplete(senderId: NodeId): Boolean {
        print("SCALABLE CACHE: Marking that node $senderId has completed copying\n")
        if (!copyComplete[senderId]) {
            copyComplete[senderId] = true

            if (copyCompleteCount.incrementAndGet() == prevNodeCount) {
                print("SCALABLE CACHE: Going to broadcast SCALE_COMPLETE message\n")
                Thread {
                    sender.broadcastScalableMessageAsync(
                        ScalableMessage(
                            nodeId,
                            "",
                            ScalableMessageType.SCALE_COMPLETE
                        )
                    )
                }.start()
                scaleInProgress.set(false)
                return true
            }
        }
        return false
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
            nodeCount++

            // Find range of keys to copy
            redistributeLock.writeLock().lock()
            val copyRanges = keyDistributor.addNode()
            redistributeLock.writeLock().unlock()

            // Start the thread to copy asynchronously
            Thread { copyKeysByHashValues(copyRanges) }.start()
        }
    }

    private fun copyKeysByHashValues(copyRanges: MutableList<Pair<Int, Int>>) {
        cache.initializeCopy(copyRanges)
        var kvPairs: MutableList<KeyValuePair>
        do {
            kvPairs = cache.streamCopyKeys(copyBatchSize)
            print("SCALABLE CACHE: Got ${kvPairs.size} key value pairs from stream\n")
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