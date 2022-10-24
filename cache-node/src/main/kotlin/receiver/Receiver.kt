package receiver

import KeyVersionPair
import NodeId
import cache.distributed.IDistributedCache
import exception.CacheNodeException
import io.javalin.Javalin
import io.javalin.core.validation.ValidationException
import node.Node
import org.eclipse.jetty.http.HttpStatus

/**
 * A concrete receiver that accepts requests over HTTP.
 */
class Receiver(private val nodeId: NodeId, private val port: Int, private val nodeCount: Int, private val node: Node, private val distributedCache: IDistributedCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
    private val app: Javalin = Javalin.create()

    /**
     * count the number of requests that are received
     */
    private var receiverUsageInfo: ReceiverUsageInfo =
        ReceiverUsageInfo(0, 0, 0, 0,  0)

    init {

        /* Handle fetch requests */
        app.get("/fetch/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            receiverUsageInfo.fetchAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass<Int>("version")
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass<Int>("senderId")
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = distributedCache.fetch(KeyVersionPair(key, version), senderNum)
            receiverUsageInfo.fetchSuccesses++

            ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.OK_200)
        }

        /* Handle store requests */
        app.post("/store/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            receiverUsageInfo.storeAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass<Int>("version")
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass<Int>("senderId")
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()
            val value = ctx.bodyValidator<String>()
                .check({ it != ""}, "Expecting value but none found")
                .get()

            distributedCache.store(KeyVersionPair(key, version), value, senderNum)
            receiverUsageInfo.storeSuccesses++

            ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.CREATED_201)
        }

        /* Handle requests to monitor information about the node of this receiver */
        app.get("/node-info") { ctx ->
            print("\n*********NODE INFO REQUEST*********\n")
            ctx.json(node.getNodeInfo()).status(HttpStatus.OK_200)
        }

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
            if (ctx.resultString() == "Not found") {
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
}

/**
 * Represents a key-version-value tuple in a HTTP response.
 */
data class KeyValueReply(val key: String, val version: Int, val value: String, val success: Boolean = true)