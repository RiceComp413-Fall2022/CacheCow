package cache.distributed.hasher

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a distributed cache key distributor.
 */
interface IKeyDistributor {

    /**
     * Gets the primary
     */
    fun getPrimaryNode(kvPair: KeyVersionPair): NodeId

    fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId>

    fun addNode(): MutableList<Pair<Int, Int>>
}