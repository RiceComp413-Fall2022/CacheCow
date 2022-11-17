package cache.distributed.hasher

import KeyVersionPair
import NodeId
import java.util.*

class ConsistentKeyDistributor(
    private var nodeCount: Int,
    private val pointsPerNode: Int = 25
): IKeyDistributor {

    private var sortedNodes: SortedMap<Int, Int> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    private val nodeHasher = NodeHasher(nodeCount)

    init {
        for (i in 0 until nodeCount) {
            for (j in 0 until pointsPerNode) {
                val hashValue = nodeHasher.extendedNodeHashValue(i, j)
                print("Building points: $i, $j, $hashValue\n")
                sortedNodes[hashValue] = i
            }
        }
        printSortedNodes()
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
            copyRangeList.add(Pair(getPrevHashValue(newHashValue), newHashValue))
        }

        // Update the node count
        nodeCount++

        print("CONSISTENT DISTRIBUTOR: Added points for new node\n");
        printSortedNodes()

        // Return range of hash values to be copied
        return copyRangeList
    }

    private fun getPrevHashValue(hashValue: Int): Int {
        val tailMap = sortedNodes.tailMap(hashValue)
        val headMap = sortedNodes.headMap(hashValue)
        return if (headMap.isNotEmpty())
            headMap.lastKey() else tailMap.lastKey()
    }

    private fun printSortedNodes() {
        print("There are ${sortedNodes.size} sorted nodes\n")
        for (pair in sortedNodes.asIterable()) {
            print("Hash value ${pair.key} and node id ${pair.value}\n")
        }
    }
}