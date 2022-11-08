package receiver

import cache.local.CacheInfo
import io.javalin.Javalin
import sender.SenderUsageInfo

/**
 * An interface specifying the behavior of a receiver, which receives request from other
 * nodes in the system.
 */
interface IReceiver {

    /**
     * Start the receiver
     */
    fun start()

    /**
     * Get the receiver usage info
     * @return ReceiverUsageInfo data type of the info
     */
    fun getReceiverUsageInfo(): ReceiverUsageInfo

    /**
     * Get system info for distributed cache, including memory usage
     */
    fun getSystemInfo(): SystemInfo

}

/**
 * Client response giving memory usage of the JVM.
 */
data class MemoryUsageInfo(val allocated: Long, val max: Long, val usage: Double)

/**
 * Encapsulates information about the usage of this node into one object
 */
data class SystemInfo(val nodeId: Int,
                      val memUsage: MemoryUsageInfo,
                      val cacheInfo: CacheInfo,
                      val receiverUsageInfo: ReceiverUsageInfo,
                      val senderUsageInfo: SenderUsageInfo
)

/**
 * Information about what the receiver has done so far
 */
data class ReceiverUsageInfo(var storeAttempts: Int,
                             var storeSuccesses: Int,
                             var fetchAttempts: Int,
                             var fetchSuccesses: Int,
                             var invalidRequests: Int)