package receiver

import cache.distributed.ITestableJavalinApp
import com.fasterxml.jackson.annotation.JsonProperty
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
data class ReceiverUsageInfo(
    @JsonProperty("storeAttempts") val storeAttempts: AtomicInteger,
    @JsonProperty("storeSuccesses") val storeSuccesses: AtomicInteger,
    @JsonProperty("fetchAttempts") val fetchAttempts: AtomicInteger,
    @JsonProperty("fetchSuccesses") val fetchSuccesses: AtomicInteger,
    @JsonProperty("clearAttempts") val clearAttempts: AtomicInteger,
    @JsonProperty("clearSuccesses") val clearSuccesses: AtomicInteger,
    @JsonProperty("invalidRequests") val invalidRequests: AtomicInteger
)

/**
 * The total time spent (in seconds) querying requests.
 */
data class TotalRequestTiming(
    @JsonProperty("storeTiming") val storeTiming: AtomicReference<Double>,
    @JsonProperty("fetchTiming") val fetchTiming: AtomicReference<Double>,
    @JsonProperty("clearTiming") val clearTiming: AtomicReference<Double>
)

/**
 * Represents a key-version tuple in a HTTP response
 */
data class KeyVersionReply(val key: String, val version: Int)