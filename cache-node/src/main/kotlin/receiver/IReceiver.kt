package receiver

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
     * @return Time spent to perform client requests.
     */
    fun getClientRequestTiming(): TotalRequestTiming

    /**
     * @return Time spent to perform server requests.
     */
    fun getServerRequestTiming(): TotalRequestTiming

}

/**
 * Information about what the receiver has done so far
 */
data class ReceiverUsageInfo(val storeAttempts: AtomicInteger, val storeSuccesses: AtomicInteger,
                             val fetchAttempts: AtomicInteger, val fetchSuccesses: AtomicInteger,
                             val removeAttempts: AtomicInteger, val removeSuccesses: AtomicInteger,
                             val clearAttempts: AtomicInteger, val clearSuccesses: AtomicInteger,
                             val invalidRequests: AtomicInteger)

/**
 * Stores total time spent (in seconds) querying requests.
 */
data class TotalRequestTiming(val storeTiming: AtomicReference<Double>,
                              val fetchTiming: AtomicReference<Double>,
                              val removeTiming: AtomicReference<Double>,
                              val clearTiming: AtomicReference<Double>)

/**
 * Represents a key-version tuple in a HTTP response.
 */
data class KeyVersionReply(val key: String, val version: Int)