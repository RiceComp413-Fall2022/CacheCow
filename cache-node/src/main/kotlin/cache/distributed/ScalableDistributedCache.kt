import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.ILocalEvictingCache
import cache.local.ILocalScalableCache
import cache.local.LocalScalableCache
import exception.KeyNotFoundException
import sender.IScalableSender
import sender.ISender
import sender.ScalableSender
import java.util.*
import kotlin.collections.ArrayList

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeCount: Int, private val nodeHasher: INodeHasher, private var cache: ILocalScalableCache, private var sender: IScalableSender):
    IScalableDistributedCache {

    private var sortedNodes: ArrayList<Pair<Int, Int>> = ArrayList()

    private var copyInProgress = false

    private var copyTimer = Timer()

    private val copySize = 10

    private val copyRetryCount = 3

    init {
        for (i in 1..nodeCount) {
            sortedNodes.add(Pair(i, nodeHasher.nodeHashValue(i)))
        }
        sortedNodes.sortBy { it.second }
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {

        // 1. Compute hash value of key
        // 2. Check membership

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("TEST CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

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

        print("TEST CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

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

    override fun testCopy() {
        sender.copyKvPairs(
            mutableListOf(Pair(KeyVersionPair("key1", 1), "1".encodeToByteArray()), Pair(KeyVersionPair("key2", 2), "2".encodeToByteArray())),
            1
            )
    }

    override fun bulkLocalStore(kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>) {
        for (kvPair in kvPairs) {
            cache.store(kvPair.first, kvPair.second)
        }
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

    fun copyHandler() {
        val newHashValue = nodeHasher.nodeHashValue(nodeCount)
        val nextIndex = findPrimaryNode(newHashValue)
        val nextHashValue = sortedNodes.get(nextIndex).first

        // Add hash value of new node to sorted node values
        val updatedNodes = ArrayList<Pair<Int, Int>>()
        updatedNodes.addAll(sortedNodes.subList(0, nextIndex))
        updatedNodes.add(Pair(nodeCount, newHashValue)) // Place new node hash value
        updatedNodes.addAll(sortedNodes.subList(nextIndex, sortedNodes.size))
        sortedNodes = updatedNodes

        copyTimer.schedule(object: TimerTask() {
            override fun run() {
                copyHelper(newHashValue, nextHashValue)
            }
        },20)
    }

    private fun copyHelper(start: Int, end: Int) {
        cache.initializeCopy(start, end)
        var kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>
        var retryRemaining: Int
        do {
            retryRemaining = copyRetryCount
            kvPairs = cache.streamCopyKeys(copySize)
            while (kvPairs.size > 0 && retryRemaining > 0 && !sender.copyKvPairs(kvPairs, nodeCount)) {
                retryRemaining--
            }

        } while(kvPairs.size == copySize)
    }
}