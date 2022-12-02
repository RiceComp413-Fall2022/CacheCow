package cache.distributed.hasher

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a distributed cache key distributor.
 */
interface IKeyDistributor {

    /**
     * Gets the primary node for storing the given key-value pair.
     *
     * @param kvPair key-value pair
     * @return the primary node id for storing the pair
     */
    fun getPrimaryNode(kvPair: KeyVersionPair): NodeId

    /**
     * Gets the primary node for the storing given key-value pair as well as the node
     * previously used for storing the pair.
     *
     * @param kvPair key-value pair
     * @return the primary and previous node ids for storing the pair
     */
    fun getPrimaryAndPrevNode(kvPair: KeyVersionPair): Pair<NodeId, NodeId>

    /**
     * Updates key distribution following node addition and returns the ranges of
     * hash values that must be copied.
     */
    fun addNode(): MutableList<Pair<Int, Int>>
}