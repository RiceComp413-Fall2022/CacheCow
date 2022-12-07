package cache.distributed

import cache.ICache
import cache.local.CacheInfo
import com.fasterxml.jackson.annotation.JsonProperty
import receiver.ReceiverUsageInfo
import receiver.TotalRequestTiming
import sender.SenderUsageInfo

/**
 * An interface specifying the behavior of a distributed data cache.
 */
interface IDistributedCache: ICache {

    /**
     * Starts the distributed cache on the given port.
     *
     * @param port port number on which to run receiver
     */
    fun start(port: Int)

    /**
     * Gets all information about the usage of this node.
     */
    fun getSystemInfo(): SystemInfo

    /**
     * Gets all information about the usage of this node.
     */
    fun getGlobalSystemInfo(): MutableList<SystemInfo>

    /**
     * Gets memory usage information from JVM runtime.
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
    data class MemoryUsageInfo(
        @JsonProperty("allocated") val allocated: Long,
        @JsonProperty("max") val max: Long,
        @JsonProperty("usage") val usage: Double
    )

    /**
     * Encapsulates information about the usage of this node into one object.
     */
    data class SystemInfo(
        @JsonProperty("nodeId") val nodeId: Int,
        @JsonProperty("hostName") val hostName: String,
        @JsonProperty("memUsage") val memUsage: MemoryUsageInfo,
        @JsonProperty("cacheInfo") val cacheInfo: CacheInfo,
        @JsonProperty("receiverUsageInfo") var receiverUsageInfo: ReceiverUsageInfo,
        @JsonProperty("senderUsageInfo") val senderUsageInfo: SenderUsageInfo,
        @JsonProperty("clientRequestTiming") val clientRequestTiming: TotalRequestTiming,
        @JsonProperty("serverRequestTiming") val serverRequestTiming: TotalRequestTiming
    )
}