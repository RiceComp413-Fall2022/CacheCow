package cache.distributed.hasher

import KeyVersionPair
import NodeId
import java.util.*

class ConsistentKeyDistributor(nodeId: NodeId, private var nodeCount: Int, private val pointsPerNode: Int = 10): IKeyDistributor {

    private var sortedNodes: SortedMap<Int, Int> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    private val nodeHasher = NodeHasher(nodeCount)

    init {
        val trueNodeCount = if (nodeId == nodeCount) nodeCount + 1 else nodeCount
        for (i in 0 until trueNodeCount) {
            for (j in 0 until pointsPerNode) {
                sortedNodes[nodeHasher.extendedNodeHashValue(i, j)] = i
            }
        }
    }

    override fun getPrimaryNode(kvPair: KeyVersionPair): NodeId {
        val hashValue = nodeHasher.primaryHashValue(kvPair)

        val tailMap = sortedNodes.tailMap(hashValue)
        if (tailMap.isNotEmpty()) {
            return tailMap.iterator().next().value
        }
        return sortedNodes.headMap(hashValue).iterator().next().value
    }

    override fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId> {
        val hashValue = nodeHasher.primaryHashValue(kvPair)
        val tailMap = sortedNodes.tailMap(hashValue)
        val headMap = sortedNodes.headMap(hashValue)

        val primaryNodeId = if (tailMap.isNotEmpty())
            tailMap.iterator().next().value
            else sortedNodes.headMap(hashValue).iterator().next().value
        val prevNodeId = if (headMap.isNotEmpty())
            headMap[headMap.lastKey()]!!
            else tailMap[tailMap.lastKey()]!!

       return Pair(primaryNodeId, prevNodeId)
    }

    override fun addNode(): MutableList<Pair<Int, Int>> {
        print("CONSISTENT DISTRIBUTOR: Beginning copying process\n")
        printSortedNodes()

        // Add hash values of new node to the circle
        var hashValue: Int
        val newHashValues = mutableListOf<Int>()
        for (j in 0 until pointsPerNode) {
            hashValue = nodeHasher.extendedNodeHashValue(nodeCount, j)
            newHashValues.add(hashValue)
            sortedNodes[hashValue] = nodeCount
        }

        // Determine the ranges where keys must be copied
        val copyRangeList = mutableListOf<Pair<Int, Int>>()
        for (newHashValue in newHashValues) {
            copyRangeList.add(Pair(getPrevNode(newHashValue), newHashValue))
        }

        // Update the node count
        nodeCount++

        // Return range of hash values to be copied
        return copyRangeList
    }

    private fun getPrevNode(hashValue: Int): NodeId {
        val tailMap = sortedNodes.tailMap(hashValue)
        val headMap = sortedNodes.headMap(hashValue)
        return if (headMap.isNotEmpty())
            headMap[headMap.lastKey()]!!
        else tailMap[tailMap.lastKey()]!!
    }

    private fun printSortedNodes() {
        print("There are ${sortedNodes.size} sorted nodes\n")
        for (pair in sortedNodes.asIterable()) {
            print("Hash value ${pair.key} and node id ${pair.value}\n")
        }
    }
}