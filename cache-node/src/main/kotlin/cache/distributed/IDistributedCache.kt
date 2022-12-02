package cache.distributed

import cache.ICache
import cache.local.CacheInfo
import receiver.ReceiverUsageInfo
import receiver.TotalRequestTiming
import sender.SenderUsageInfo

/**
 * An interface specifying the behavior of a distributed data cache.
 */
interface IDistributedCache: ICache {

    /**
     * Start the distributed cache on the given port.
     *
     * @param port port number on which to run receiver
     */
    fun start(port: Int)

    /**
     * Get all information about the usage of this node.
     */
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

    /**
     * Client response giving memory usage of the JVM.
     */
    data class MemoryUsageInfo(val allocated: Long, val max: Long, val usage: Double)

    /**
     * Encapsulates information about the usage of this node into one object.
     */
    data class SystemInfo(
        val nodeId: Int,
        val memUsage: MemoryUsageInfo,
        val cacheInfo: CacheInfo,
        var receiverUsageInfo: ReceiverUsageInfo,
        val senderUsageInfo: SenderUsageInfo,
        val clientRequestTiming: TotalRequestTiming,
        val serverRequestTiming: TotalRequestTiming
    )
}