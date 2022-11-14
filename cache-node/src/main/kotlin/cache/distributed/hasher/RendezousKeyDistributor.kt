package cache.distributed.hasher

import KeyVersionPair
import NodeId

class RendezvousKeyDistributor: IKeyDistributor {
    override fun getPrimaryNode(kvPair: KeyVersionPair): NodeId {
        TODO("Not yet implemented")
    }

    override fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId> {
        TODO("Not yet implemented")
    }

    override fun addNode(): Pair<Int, Int> {
        TODO("Not yet implemented")
    }
}