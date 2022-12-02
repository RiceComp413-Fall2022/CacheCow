package cache.distributed.hasher

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a node hasher, which is used to map keys to
 * nodes.
 */
interface INodeHasher {

    /**
     * Returns the hash value for given node.
     *
     * @param nodeId The node id
     * @return The hash value of the node id
     */
    fun nodeHashValue(nodeId: NodeId): Int

    /**
     * Returns the hash value for the (node, index) pair.
     *
     * @param nodeId The node id
     * @param index Integer used to adjust hash value
     * @return The hash value of the (node, index) pair
     */
    fun extendedNodeHashValue(nodeId: NodeId, index: Int): Int

    /**
     * Returns the hash value for given key-version pair.
     *
     * @param kvPair The key version pair
     * @return The hash value of the pair
     */
    fun primaryHashValue(kvPair: KeyVersionPair): Int

    /**
     * Finds the designated node for a given key-version pair.
     *
     * @param kvPair The key version pair
     * @return The ID of the node designated for the key version pair
     */
    fun primaryHashNode(kvPair: KeyVersionPair): NodeId

}