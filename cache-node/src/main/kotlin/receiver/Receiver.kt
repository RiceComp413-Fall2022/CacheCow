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
        0, 0, 0, 0,
        0, 0, 0, 0, 0)

    /**
     * Time spent (in seconds) to perform client requests.
     */
    private var clientRequestTiming: TotalRequestTiming = TotalRequestTiming(
        0.0, 0.0, 0.0, 0.0)

    /**
     * Time spent (in seconds) to perform server requests.
     */
    private var serverRequestTiming: TotalRequestTiming = TotalRequestTiming(
        0.0, 0.0, 0.0, 0.0)

    init {

        /** ENDPOINTS **/

        /* Handle Testing: Hello Word */
        app.get("/v1/hello-world") { ctx ->
            ctx.result("Hello, World!").status(HttpStatus.OK_200)
        }

        /* Handle fetch requests */
        app.get("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            receiverUsageInfo.fetchAttempts++

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
                ctx.result(value).status(HttpStatus.OK_200)
            }

            // Increment node statistics
            receiverUsageInfo.fetchSuccesses++
            if (isClientRequest) {
                clientRequestTiming.fetchTiming += requestTime
            } else {
                serverRequestTiming.fetchTiming += requestTime
            }
        }

        /* Handle Store Requests */
        app.post("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            receiverUsageInfo.storeAttempts++

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
            receiverUsageInfo.storeSuccesses++
            if (isClientRequest) {
                clientRequestTiming.storeTiming += requestTime
            } else {
                serverRequestTiming.storeTiming += requestTime
            }
        }

        /* Handle Remove Requests */
        app.delete("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********REMOVE REQUEST*********\n")
            receiverUsageInfo.removeAttempts++

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
            receiverUsageInfo.removeSuccesses++
            if (isClientRequest) {
                clientRequestTiming.removeTiming += requestTime
            } else {
                serverRequestTiming.removeTiming += requestTime
            }
        }

        /* Handle Clear Requests */
        app.delete("/v1/clear") { ctx ->
            print("\n*********CLEAR REQUEST*********\n")
            receiverUsageInfo.clearAttempts++

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
                    localCache.clear()
                }
                ctx.status(HttpStatus.NO_CONTENT_204)
            }

            // Increment node statistics
            receiverUsageInfo.clearSuccesses++
            if (isClientRequest) {
                clientRequestTiming.clearTiming += requestTime
            } else {
                serverRequestTiming.clearTiming += requestTime
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
                receiverUsageInfo.invalidRequests++
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