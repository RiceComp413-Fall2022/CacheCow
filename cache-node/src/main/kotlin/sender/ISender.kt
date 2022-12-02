package sender

import KeyVersionPair
import NodeId
import java.util.concurrent.atomic.AtomicInteger

/**
 * An interface specifying the behavior of a sender, which sends requests to other nodes
 * in the system.
 */
interface ISender {

    /**
     * Fetches a value from a remote node.
     *
     * @param kvPair The key-version pair to look up
     * @param destNodeId The node from which to retrieve the value
     * @return The value, or null if the lookup fails
     */
    fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray

    /**
     * Stores a value to a remote node.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @param destNodeId The node to which the value should be stored
     * @return True if the store succeeded, and false otherwise
     */
    fun storeToNode(kvPair: KeyVersionPair, value: ByteArray, destNodeId: NodeId)

    /**
     * Removes a specified element from the node's local cache.
     * @param kvPair The key-version pair to look up
     * @param destNodeId The node to which the value should be stored
     * @return the previous value associated with the key-version pair, or null if there
     * was no mapping for the key-version pair.
     */
    fun removeFromNode(kvPair: KeyVersionPair, destNodeId: NodeId) : ByteArray?

    /**
     * Clears all elements from the node's local cache.
     * @param destNodeId The node to which the value should be stored
     */
    fun clearNode(destNodeId: NodeId)

    /**
     * Gets the sender usage info
     * @return SenderUsageInfo data type of the info
     */
    fun getSenderUsageInfo(): SenderUsageInfo
}

/**
 * Information about what the sender has done so far.
 */
data class SenderUsageInfo(val storeAttempts: AtomicInteger, val storeSuccesses: AtomicInteger,
                           val fetchAttempts: AtomicInteger, val fetchSuccesses: AtomicInteger,
                           val removeAttempts: AtomicInteger, val removeSuccesses: AtomicInteger,
                           val clearAttempts: AtomicInteger, val clearSuccesses: AtomicInteger)