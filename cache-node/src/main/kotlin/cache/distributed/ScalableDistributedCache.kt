import cache.distributed.IDistributedCache
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.TestCache
import exception.KeyNotFoundException
import sender.ISender
import java.util.*
import kotlin.collections.ArrayList

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, private var nodeCount: Int, private val cache: TestCache, var sender: ISender):
    IDistributedCache {

    /**
     * The INodeHasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

    private var sortedNodes: ArrayList<Pair<Int, Int>> = ArrayList()

    private var copyTimer = Timer()

    private val copySize = 10

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

        val primaryNodeId = findPrimaryNode(hashValue)

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
        // 1. Compute hash value of key
        // 2. Check membership

        val hashValue = nodeHasher.primaryHashValue(kvPair)

        print("TEST CACHE: Hash value of key ${kvPair.key} is ${hashValue}\n")

        val primaryNodeId = findPrimaryNode(hashValue)

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value, hashValue)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
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
        cache.initalizeCopy(start, end)
        var kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>
        do {
            kvPairs = cache.streamCopyKeys(copySize)
            // Send to node
        } while(kvPairs.size == copySize)
    }
}