package cache.distributed

import KeyVersionPair
import NodeId
import cache.local.CacheInfo
import sender.SenderUsageInfo

/**
 * An interface specifying the behavior of a distributed data cache.
 */
interface IDistributedCache {

    /**
     * Fetch a value from the distributed cache.
     *
     * @param kvPair The key-version pair to look up
     * @param senderId If this lookup came from a remote node, the ID of that node
     * @return The value if found
     */
    fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray

    /**
     * Store a value to the distributed cache.
     *
     * @param kvPair The key-version pair to store
     * @param value The value to store
     * @param senderId If this lookup came from a remote node, the ID of that node
     */
    fun store(kvPair: KeyVersionPair, value: ByteArray, senderId: NodeId?)

    fun getCacheInfo(): CacheInfo

    fun getSenderInfo(): SenderUsageInfo
}