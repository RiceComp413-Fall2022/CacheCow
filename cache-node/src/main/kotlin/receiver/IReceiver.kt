package receiver

import io.javalin.Javalin

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
     * @return TotalRequestTiming: Time spent to perform store and fetch requests.
     */
    fun getTotalRequestTiming(): TotalRequestTiming

}

/**
 * Information about what the receiver has done so far
 */
data class ReceiverUsageInfo(var storeAttempts: Int,
                             var storeSuccesses: Int,
                             var fetchAttempts: Int,
                             var fetchSuccesses: Int,
                             var invalidRequests: Int)

/**
 * Stores total time spent (in seconds) querying requests.
 */
data class TotalRequestTiming(var storeTiming: Double,
                              var fetchTiming: Double)