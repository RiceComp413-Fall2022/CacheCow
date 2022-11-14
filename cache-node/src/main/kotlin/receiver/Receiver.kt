package receiver

import KeyVersionPair
import cache.distributed.IDistributedCache
import cache.local.ILocalCache
import exception.CacheNodeException
import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.plugin.bundled.CorsContainer
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.validation.ValidationError
import io.javalin.validation.ValidationException
import node.Node
import org.eclipse.jetty.http.HttpStatus
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.*

/**
 * A concrete receiver that accepts requests over HTTP.
 */
class Receiver(private val port: Int, private val nodeCount: Int, private val node: Node,
               private val distributedCache: IDistributedCache, private val localCache : ILocalCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
     val app: Javalin = Javalin.create { config: JavalinConfig ->
        config.plugins.enableCors { cors: CorsContainer ->
            cors.add { it: CorsPluginConfig -> it.anyHost() }
        }
    }

    /**
     * Counts the number of requests that are received
     */
    private var receiverUsageInfo: ReceiverUsageInfo = ReceiverUsageInfo(
        AtomicInteger(0), AtomicInteger(0), AtomicInteger(0),
        AtomicInteger(0), AtomicInteger(0), AtomicInteger(0),
        AtomicInteger(0), AtomicInteger(0), AtomicInteger(0))

    /**
     * Time spent (in seconds) to perform client requests.
     */
    private var clientRequestTiming: TotalRequestTiming = TotalRequestTiming(
        AtomicReference<Double>(0.0), AtomicReference<Double>(0.0),
        AtomicReference<Double>(0.0), AtomicReference<Double>(0.0))

    /**
     * Time spent (in seconds) to perform server requests.
     */
    private var serverRequestTiming: TotalRequestTiming = TotalRequestTiming(
        AtomicReference<Double>(0.0), AtomicReference<Double>(0.0),
        AtomicReference<Double>(0.0), AtomicReference<Double>(0.0))

    init {

        /** ENDPOINTS **/

        /* Handle Testing: Hello Word */
        app.get("/v1/hello-world") { ctx ->
            ctx.result("Hello, World!").status(HttpStatus.OK_200)
        }

        /* Handle fetch requests */
        app.get("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            receiverUsageInfo.fetchAttempts.getAndIncrement()

            // Handle Request
            var isClientRequest = false
            val requestTime = 1/1000.0 * measureTimeMillis {
                // Parse Path
                val key = ctx.pathParam("key")
                val version = ctx.pathParamAsClass("version", Int::class.java)
                    .check({ it >= 0 }, "Version number cannot be negative")
                    .get()
                val senderNum = if (ctx.queryParam("senderId") == null) null else
                    ctx.queryParamAsClass("senderId", Int::class.java)
                        .check(
                            { it in 0 until nodeCount },
                            "Sender id must be in range (0, ${nodeCount - 1})"
                        )
                        .get()
                isClientRequest = senderNum == null

                // Fetch Data
                val value = if (isClientRequest) {
                    distributedCache.fetch(KeyVersionPair(key, version))
                } else {
                    localCache.fetch(KeyVersionPair(key, version))
                }
                if (value != null) {
                    ctx.result(value).status(HttpStatus.OK_200)
                } else {
                    ctx.status(HttpStatus.NOT_FOUND_404)
                }
            }

            // Increment node statistics
            receiverUsageInfo.fetchSuccesses.getAndIncrement()
            if (isClientRequest) {
                clientRequestTiming.fetchTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            } else {
                serverRequestTiming.fetchTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            }
        }

        /* Handle Store Requests */
        app.post("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            receiverUsageInfo.storeAttempts.getAndIncrement()

            // Handle Request
            var isClientRequest = false
            val requestTime = 1/1000.0 * measureTimeMillis {
                val key = ctx.pathParam("key")
                val version = ctx.pathParamAsClass("version", Int::class.java)
                    .check({ it >= 0}, "Version number cannot be negative")
                    .get()
                val senderNum = if (ctx.queryParam("senderId") == null) null else
                    ctx.queryParamAsClass("senderId", Int::class.java)
                        .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                        .get()
                isClientRequest = senderNum == null

                // Store Data
                val value = ctx.bodyAsBytes()
                if (value.isEmpty()) {
                    throw ValidationException(mapOf("REQUEST_BODY" to listOf(ValidationError("Binary blob cannot be empty"))))
                }
                if (isClientRequest) {
                    distributedCache.store(KeyVersionPair(key, version), value)
                } else {
                    localCache.store(KeyVersionPair(key, version), value)
                }
                ctx.json(KeyVersionReply(key, version)).status(HttpStatus.CREATED_201)
            }

            // Increment node statistics
            receiverUsageInfo.storeSuccesses.getAndIncrement()
            if (isClientRequest) {
                clientRequestTiming.storeTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            } else {
                serverRequestTiming.storeTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            }
        }

        /* Handle Remove Requests */
        app.delete("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********REMOVE REQUEST*********\n")
            receiverUsageInfo.removeAttempts.getAndIncrement()

            // Handle Request
            var isClientRequest = false
            val requestTime = 1/1000.0 * measureTimeMillis {
                // Parse Path
                val key = ctx.pathParam("key")
                val version = ctx.pathParamAsClass("version", Int::class.java)
                    .check({ it >= 0 }, "Version number cannot be negative")
                    .get()
                val senderNum = if (ctx.queryParam("senderId") == null) null else
                    ctx.queryParamAsClass("senderId", Int::class.java)
                        .check(
                            { it in 0 until nodeCount },
                            "Sender id must be in range (0, ${nodeCount - 1})"
                        )
                        .get()
                isClientRequest = senderNum == null

                // Remove Data
                val value = if (isClientRequest) {
                    distributedCache.remove(KeyVersionPair(key, version))
                } else {
                    localCache.remove(KeyVersionPair(key, version))
                }

                if (value != null) {
                    ctx.status(HttpStatus.NO_CONTENT_204)
                } else {
                    ctx.status(HttpStatus.NOT_FOUND_404)
                }
            }

            // Increment node statistics
            receiverUsageInfo.removeSuccesses.getAndIncrement()
            if (isClientRequest) {
                clientRequestTiming.removeTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            } else {
                serverRequestTiming.removeTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            }
        }

        /* Handle Clear Requests */
        app.delete("/v1/clear") { ctx ->
            print("\n*********CLEAR REQUEST*********\n")
            receiverUsageInfo.clearAttempts.getAndIncrement()

            // Handle Request
            var isClientRequest = false
            val requestTime = 1/1000.0 * measureTimeMillis {
                // Parse Path
                val senderNum = if (ctx.queryParam("senderId") == null) null else
                    ctx.queryParamAsClass("senderId", Int::class.java)
                        .check(
                            { it in 0 until nodeCount },
                            "Sender id must be in range (0, ${nodeCount - 1})"
                        )
                        .get()
                isClientRequest = senderNum == null

                // Clear Cache
                if (isClientRequest) {
                    distributedCache.clearAll()
                } else {
                    localCache.clearAll()
                }
                ctx.status(HttpStatus.NO_CONTENT_204)
            }

            // Increment node statistics
            receiverUsageInfo.clearSuccesses.getAndIncrement()
            if (isClientRequest) {
                clientRequestTiming.clearTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            } else {
                serverRequestTiming.clearTiming.accumulateAndGet(requestTime) { a: Double, b: Double -> a + b }
            }
        }

        /* Handle Node Information */
        app.get("/v1/node-info") { ctx ->
            print("\n*********NODE INFO REQUEST*********\n")
            ctx.json(node.getNodeInfo()).status(HttpStatus.OK_200)
        }



        /** ERROR HANDLING **/

        app.exception(CacheNodeException::class.java) { e, ctx ->
            print("RECEIVER: Caught cache node exception with id ${e.getExceptionID()}\n")
            ctx.result(e.message).status(e.status)
        }

        app.exception(ValidationException::class.java) { e, ctx ->
            val firstError = e.errors.asIterable().iterator().next()
            print("RECEIVER: Caught validation exception for field ${firstError.key}\n")

            // TODO: Return message in JSON response body, extract more info from exception
            ctx.result(firstError.value[0].message).status(HttpStatus.BAD_REQUEST_400)
        }

        /* Handle invalid requests */
        app.error(HttpStatus.NOT_FOUND_404) { ctx ->
            if (ctx.result() == "Not found") {
                receiverUsageInfo.invalidRequests.getAndIncrement()
                ctx.result(
              """
                Invalid Request.
                Valid Requests:
                GET /fetch/{key}/{version}: Fetch the value corresponding to the given key-version pair
                POST /store/{key}/{version} with request body ""value"": Store the value corresponding to the given key-version pair
                """.trimIndent()
                )
            }
        }
    }

    /**
     * Start the HTTP server.
     */
    override fun start() {
        app.start(port)
    }

    override fun getReceiverUsageInfo(): ReceiverUsageInfo {
        return receiverUsageInfo
    }

    override fun getClientRequestTiming(): TotalRequestTiming {
        return clientRequestTiming
    }

    override fun getServerRequestTiming(): TotalRequestTiming {
        return serverRequestTiming
    }
}