package receiver

import KeyVersionPair
import NodeId
import com.fasterxml.jackson.databind.ObjectMapper
import cache.distributed.IDistributedCache
import exception.ConnectionRefusedException
import exception.InternalErrorException
import exception.JSONParseException
import exception.KeyNotFoundException
import io.javalin.Javalin
import io.javalin.core.validation.ValidationException
import io.javalin.http.HttpCode
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

/**
 * A concrete receiver that accepts requests over HTTP.
 */
class Receiver(private val nodeId: NodeId, private val nodeCount: Int, private val distributedCache: IDistributedCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
    private val app: Javalin = Javalin.create()

    init {
        /* Handle store requests */
        app.post("/store/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass<Int>("version")
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass<Int>("senderId")
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()
            val value: String
            try {
                value = ctx.bodyValidator<String>()
                    .check({ it != ""}, "Expecting value but none found")
                    .get()
            } catch (e: ValidationException) {
                throw JSONParseException()
            }

            distributedCache.store(KeyVersionPair(key, version), value, senderNum)
            ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.CREATED_201)
        }

        /* Handle fetch requests */
        app.get("/fetch/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass<Int>("version")
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass<Int>("senderId")
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = distributedCache.fetch(KeyVersionPair(key, version), senderNum)
            ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.OK_200)
        }

        app.exception(HttpResponseException::class.java) { e, ctx ->
            ctx.result(e.message!!).status(e.status)
        }

        /* Handle invalid requests */
        app.error(HttpStatus.NOT_FOUND_404) { ctx ->
            if (ctx.resultString() == "Not found") {
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
        app.start(7070 + nodeId)
    }
}

/**
 * Represents a key-version-value tuple in a HTTP response.
 */
data class KeyValueReply(val key: String, val version: Int, val value: String, val success: Boolean = true)