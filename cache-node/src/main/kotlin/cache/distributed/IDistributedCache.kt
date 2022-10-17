package cache.distributed

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a distributed data cache.
 */
interface IDistributedCache {

    /**
     * Fetch a value from the distributed cache.
     *
     * @param kvPair The key-version pair to look up
     * @param senderId If this lookup came from a remote node, the ID of that node
     * @return The value, or null if the lookup fails
     */
    fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String?

    /**
     * Store a value to the distributed cache.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @param senderId If this lookup came from a remote node, the ID of that node
     * @return True if the store succeeded, and false otherwise
     */
    fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?): Boolean

}