package interfaces

import KeyVersionPair
import NodeId

/**
 * Interface describing actions supported by the receiver. Implementing classes will
 * elect caching policies across nodes in the system.
 */
interface IReceiverService {

    /**
     * Store the given key value pair in the distributed cache.
     */
    fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?)

    /**
     * Fetch the value corresponding to the given key from the distributed cache.
     */
    fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String
}