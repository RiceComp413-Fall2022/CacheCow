package receiver

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
data class ReceiverUsageInfo(var storeAttempts: Int, var storeSuccesses: Int,
                             var fetchAttempts: Int, var fetchSuccesses: Int,
                             var removeAttempts: Int, var removeSuccesses: Int,
                             var clearAttempts: Int, var clearSuccesses: Int,
                             var invalidRequests: Int)

/**
 * Stores total time spent (in seconds) querying requests.
 */
data class TotalRequestTiming(var storeTiming: Double,
                              var fetchTiming: Double,
                              var removeTiming: Double,
                              var clearTiming: Double)

/**
 * Represents a key-version tuple in a HTTP response.
 */
data class KeyVersionReply(val key: String, val version: Int)