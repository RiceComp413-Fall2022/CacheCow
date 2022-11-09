package receiver

import io.javalin.Javalin
import java.util.concurrent.atomic.AtomicInteger

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

}

/**
 * Information about what the receiver has done so far
 */
data class ReceiverUsageInfo(var storeAttempts: AtomicInteger,
                             var storeSuccesses: AtomicInteger,
                             var fetchAttempts: AtomicInteger,
                             var fetchSuccesses: AtomicInteger,
                             var invalidRequests: AtomicInteger)