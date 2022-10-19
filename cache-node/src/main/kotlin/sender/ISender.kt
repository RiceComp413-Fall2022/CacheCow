package sender

import KeyVersionPair
import NodeId

/**
 * An interface specifying the behavior of a sender, which sends requests to other nodes
 * in the system.
 */
interface ISender {

    /**
     * Fetch a value from a remote node.
     *
     * @param kvPair The key-version pair to look up
     * @param destNodeId The node from which to retrieve the value
     * @return The value, or null if the lookup fails
     */
    fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): String

    /**
     * Store a value to a remote node.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @param destNodeId The node to which the value should be stored
     * @return True if the store succeeded, and false otherwise
     */
    fun storeToNode(kvPair: KeyVersionPair, value: String, destNodeId: NodeId)

    /**
     * Gets the sender usage info
     * @return SenderUsageInfo data type of the info
     */
    fun getSenderUsageInfo(): SenderUsageInfo

}

/**
 * Information about what the sender has done so far
 */
data class SenderUsageInfo(var storeAttempts: Int,
                           var storeSuccesses: Int,
                           var fetchAttempts: Int,
                           var fetchSuccesses: Int)