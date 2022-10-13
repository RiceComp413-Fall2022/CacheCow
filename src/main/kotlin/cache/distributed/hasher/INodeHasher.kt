package cache.distributed.hasher

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a node hasher, which is used to map keys to
 * nodes.
 */
interface INodeHasher {

    /**
     * Get the designated node for a given key-version pair.
     *
     * @param kvPair The key version pair
     * @return The ID of the node designated for the key version pair
     */
    fun primaryHash(kvPair: KeyVersionPair): NodeId

}