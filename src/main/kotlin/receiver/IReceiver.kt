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

}

/**
 * Information about what the receiver has done so far
 * TODO: not used yet
 */
data class ReceiverUsageInfo(val requestsReceived: Int)