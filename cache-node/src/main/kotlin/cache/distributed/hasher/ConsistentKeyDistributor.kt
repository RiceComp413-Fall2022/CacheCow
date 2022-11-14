package cache.distributed.hasher

import KeyVersionPair
import NodeId

class ConsistentKeyDistributor(private var nodeCount: Int): IKeyDistributor {

    private val nodeHasher = NodeHasher(nodeCount)

    override fun getPrimaryNode(kvPair: KeyVersionPair): NodeId {
        TODO("Not yet implemented")
    }

    override fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId> {
        TODO("Not yet implemented")
    }

    override fun addNode(newNodeId: NodeId): Pair<Int, Int> {
        nodeCount++
        TODO("Not yet implemented")
    }
}