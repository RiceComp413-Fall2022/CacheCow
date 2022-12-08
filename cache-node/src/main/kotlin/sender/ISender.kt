package sender

import KeyVersionPair
import NodeId
import cache.distributed.IDistributedCache.SystemInfo
import com.fasterxml.jackson.annotation.JsonProperty
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
     * Get the local cache info from the destination node.
     *
     * @param destNodeId The node whose cache info we are fetching
     * @return The current cache info of the destination node
     */
    fun getCacheInfo(destNodeId: NodeId): SystemInfo

    /**
     * Gets the sender usage info
     * @return SenderUsageInfo data type of the info
     */
    fun getSenderUsageInfo(): SenderUsageInfo
}

/**
 * Information about what the sender has done so far.
 */
data class SenderUsageInfo(
    @JsonProperty("storeAttempts") val storeAttempts: AtomicInteger,
    @JsonProperty("storeSuccesses") val storeSuccesses: AtomicInteger,
    @JsonProperty("fetchAttempts") val fetchAttempts: AtomicInteger,
    @JsonProperty("fetchSuccesses") val fetchSuccesses: AtomicInteger,
    @JsonProperty("removeAttempts") val removeAttempts: AtomicInteger,
    @JsonProperty("removeSuccesses") val removeSuccesses: AtomicInteger,
    @JsonProperty("clearAttempts") val clearAttempts: AtomicInteger,
    @JsonProperty("clearSuccesses") val clearSuccesses: AtomicInteger
)