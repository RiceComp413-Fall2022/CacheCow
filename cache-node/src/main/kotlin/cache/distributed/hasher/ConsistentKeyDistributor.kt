package cache.distributed.hasher

import KeyVersionPair
import NodeId
import java.util.*

class ConsistentKeyDistributor(private val nodeId: NodeId, private var nodeCount: Int, k: Int): IKeyDistributor {

    private var sortedNodes: SortedMap<Int, Int> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    private val nodeHasher = NodeHasher(nodeCount)

    init {
        for (i in 0 until nodeCount) {
            sortedNodes[nodeHasher.nodeHashValue(i)] = i
        }

        if (nodeId == nodeCount) {
            sortedNodes[nodeHasher.nodeHashValue(nodeId)] = nodeId
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

    override fun addNode(): Pair<Int, Int> {
        print("CONSISTENT DISTRIBUTOR: Beginning copying process\n")
        printSortedNodes()
        
        val newHashValue = nodeHasher.nodeHashValue(nodeCount)

        // Update the sorted nodes and node count
        sortedNodes[newHashValue] = nodeCount
        nodeCount++

        // Return range of hash values to be copied
        return Pair(nodeHasher.nodeHashValue(nodeId), newHashValue)
    }

    private fun printSortedNodes() {
        print("There are ${sortedNodes.size} sorted nodes\n")
        for (pair in sortedNodes.asIterable()) {
            print("Hash value ${pair.key} and node id ${pair.value}\n")
        }
    }
}