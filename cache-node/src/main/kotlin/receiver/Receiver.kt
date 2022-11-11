package receiver

import KeyVersionPair
import NodeId
import cache.distributed.IDistributedCache
import exception.CacheNodeException
import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.plugin.bundled.CorsContainer
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.validation.ValidationError
import io.javalin.validation.ValidationException
import org.eclipse.jetty.http.HttpStatus

/**
 * A concrete receiver that accepts requests over HTTP.
 */
open class Receiver(private val port: Int, private val nodeId: NodeId, private val nodeCount: Int, private val distributedCache: IDistributedCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
    val app: Javalin = Javalin.create { config: JavalinConfig ->
        config.plugins.enableCors { cors: CorsContainer ->
            cors.add { it: CorsPluginConfig -> it.anyHost() }
        }
    }


    /**
     * Count the number of requests that are received
     */
    private var receiverUsageInfo: ReceiverUsageInfo =
        ReceiverUsageInfo(0, 0, 0, 0,  0)


    init {

        /* Check if receiver is up and running */
        app.get("/v1/hello-world") { ctx ->
            ctx.result("Hello, World!").status(HttpStatus.OK_200)
        }

        /* Handle fetch requests */
        app.get("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            receiverUsageInfo.fetchAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass("version", Int::class.java)
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass("senderId", Int::class.java)
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = distributedCache.fetch(KeyVersionPair(key, version), senderNum)
            receiverUsageInfo.fetchSuccesses++

            ctx.result(value).status(HttpStatus.OK_200)
        }

        /* Handle store requests */
        app.post("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            receiverUsageInfo.storeAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass("version", Int::class.java)
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass("senderId", Int::class.java)
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = ctx.bodyAsBytes()
            if (value.isEmpty()) {
                throw simpleValidationException("Binary blob cannot be empty")
            }
            distributedCache.store(KeyVersionPair(key, version), value, senderNum)
            receiverUsageInfo.storeSuccesses++

            ctx.json(KeyVersionReply(key, version)).status(HttpStatus.CREATED_201)
        }

        /* Handle requests to monitor information about the node of this receiver */
        app.get("/v1/node-info") { ctx ->
            print("\n*********NODE INFO REQUEST*********\n")
            ctx.json(getSystemInfo()).status(HttpStatus.OK_200)
        }

        /* Catch and return any internal errors */
        app.exception(CacheNodeException::class.java) { e, ctx ->
            print("RECEIVER: Caught cache node exception with id ${e.getExceptionID()}\n")
            ctx.result(e.message).status(e.status)
        }

        /* Catch and format any errors resulting from request validation */
        app.exception(ValidationException::class.java) { e, ctx ->
            val firstError = e.errors.asIterable().iterator().next()
            print("RECEIVER: Caught validation exception for field ${firstError.key}: ${firstError.value[0].message}\n")

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

    fun simpleValidationException(message: String): ValidationException {
        return ValidationException(mapOf("REQUEST_BODY" to listOf(ValidationError(message))))
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

    override fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            nodeId,
            getMemoryUsage(),
            distributedCache.getCacheInfo(),
            getReceiverUsageInfo(),
            distributedCache.getSenderInfo())
    }

    /**
     * Get memory usage information from JVM runtime.
     */
    private fun getMemoryUsage(): MemoryUsageInfo {
        val runtime = Runtime.getRuntime()
        val allocatedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usage = allocatedMemory/(maxMemory * 1.0)
        return MemoryUsageInfo(allocatedMemory, maxMemory, usage)
    }
}

/**
 * Represents a key-version tuple in an HTTP response.
 */
data class KeyVersionReply(val key: String, val version: Int)