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

    fun getApp(): Javalin

    /**
     * Get the receiver usage info
     * @return ReceiverUsageInfo data type of the info
     */
    fun getReceiverUsageInfo(): ReceiverUsageInfo

}

/**
 * Information about what the receiver has done so far
 */
data class ReceiverUsageInfo(var storeAttempts: Int,
                             var storeSuccesses: Int,
                             var fetchAttempts: Int,
                             var fetchSuccesses: Int,
                             var invalidRequests: Int)