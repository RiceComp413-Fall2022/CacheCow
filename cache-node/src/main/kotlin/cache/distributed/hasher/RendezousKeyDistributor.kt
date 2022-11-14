package cache.distributed.hasher

import KeyVersionPair
import NodeId

class RendezousKeyDistributor: IKeyDistributor {
    override fun getPrimaryNode(kvPair: KeyVersionPair): NodeId {
        TODO("Not yet implemented")
    }

    override fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId> {
        TODO("Not yet implemented")
    }

    override fun addNode(newNodeId: NodeId): Pair<Int, Int> {
        TODO("Not yet implemented")
    }
}