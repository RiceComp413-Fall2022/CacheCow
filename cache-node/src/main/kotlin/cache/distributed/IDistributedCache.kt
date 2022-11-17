package cache.distributed

import KeyVersionPair
import NodeId
import cache.local.CacheInfo
import receiver.ReceiverUsageInfo
import sender.ISender
import sender.Sender
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

    fun getSystemInfo(): SystemInfo

    /**
     * Get memory usage information from JVM runtime.
     */
    fun getMemoryUsage(): MemoryUsageInfo {
        val runtime = Runtime.getRuntime()
        val allocatedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usage = allocatedMemory/(maxMemory * 1.0)
        return MemoryUsageInfo(allocatedMemory, maxMemory, usage)
    }

    fun mockSender(mock: Sender)

    /**
     * Client response giving memory usage of the JVM.
     */
    data class MemoryUsageInfo(val allocated: Long, val max: Long, val usage: Double)

    /**
     * Encapsulates information about the usage of this node into one object
     */
    data class SystemInfo(
        val nodeId: Int,
        val memUsage: MemoryUsageInfo,
        val cacheInfo: CacheInfo,
        var receiverUsageInfo: ReceiverUsageInfo?,
        val senderUsageInfo: SenderUsageInfo)
}