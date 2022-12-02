package receiver

import cache.distributed.IDistributedCache
import cache.distributed.ITestableJavalinApp
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * An interface specifying the behavior of a receiver, which receives request from other
 * nodes in the system.
 */
interface IReceiver: ITestableJavalinApp {

    /**
     * Start the receiver on the given port.
     *
     * @param port for running Javalin
     */
    fun start(port: Int)

    /**
     * Returns the information about the receiver usage.
     */
    fun getReceiverUsageInfo(): ReceiverUsageInfo

    /**
     * Get system info for distributed cache, including memory usage.
     */
    fun getSystemInfo(): IDistributedCache.SystemInfo

    /**
     * Returns the time spent to perform client requests.
     */
    fun getClientRequestTiming(): TotalRequestTiming

    /**
     * Returns the time spent to perform server requests.
     */
    fun getServerRequestTiming(): TotalRequestTiming


}
/**
 * Information about what the receiver has done so far.
 */
data class ReceiverUsageInfo(val storeAttempts: AtomicInteger, val storeSuccesses: AtomicInteger,
                             val fetchAttempts: AtomicInteger, val fetchSuccesses: AtomicInteger,
                             val removeAttempts: AtomicInteger, val removeSuccesses: AtomicInteger,
                             val clearAttempts: AtomicInteger, val clearSuccesses: AtomicInteger,
                             val invalidRequests: AtomicInteger)

/**
 * The total time spent (in seconds) querying requests.
 */
data class TotalRequestTiming(val storeTiming: AtomicReference<Double>,
                              val fetchTiming: AtomicReference<Double>,
                              val removeTiming: AtomicReference<Double>,
                              val clearTiming: AtomicReference<Double>)

/**
 * Represents a key-version tuple in a HTTP response
 */
data class KeyVersionReply(val key: String, val version: Int)