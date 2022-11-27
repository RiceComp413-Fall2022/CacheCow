package cache.distributed.hasher

import KeyVersionPair
import NodeId

interface IKeyDistributor {
    fun getPrimaryNode(kvPair: KeyVersionPair): NodeId

    fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId>

    fun addNode(): MutableList<Pair<Int, Int>>
}