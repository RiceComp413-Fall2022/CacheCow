package receiver

import KeyVersionPair
import NodeId
import com.fasterxml.jackson.databind.ObjectMapper
import cache.distributed.IDistributedCache
import io.javalin.Javalin
import io.javalin.http.HttpCode
import org.eclipse.jetty.http.HttpStatus

/**
 * A concrete receiver that accepts requests over HTTP.
 */
class Receiver(private val nodeId: NodeId, private val distributedCache: IDistributedCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
    private val app: Javalin = Javalin.create()

    /**
     * The ObjectMapper used to decode JSON data
     */
    private val mapper: ObjectMapper = ObjectMapper()

    init {
        /* Handle store requests */
        app.post("/store/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            val key = ctx.pathParam("key")
            val version = Integer.parseInt(ctx.pathParam("version"))
            val value = if (ctx.body() == "") null else mapper.readTree(ctx.body()).textValue()
            val senderId = ctx.queryParam("senderId")

            if (value == null) { // TODO: Look into Javalin validators
                print(ctx.body())
                ctx.json(FailureReply("Expecting request body but none found.")).status(HttpCode.BAD_REQUEST)
                return@post
            }

            val senderNum = if (senderId == null) null else Integer.parseInt(senderId)
            val success = distributedCache.store(KeyVersionPair(key, version), value, senderNum)
            if (success) {
                ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.CREATED_201)
            } else {
                ctx.json(FailureReply("Failed to store pair.")).status(HttpCode.INTERNAL_SERVER_ERROR) // TODO: Make more descriptive
            }
        }

        /* Handle fetch requests */
        app.get("/fetch/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            val key = ctx.pathParam("key")
            val version = Integer.parseInt(ctx.pathParam("version"))
            val senderId = ctx.queryParam("senderId")

            val senderNum = if (senderId == null) null else Integer.parseInt(senderId)
            val value = distributedCache.fetch(KeyVersionPair(key, version), senderNum)
            if (value != null) {
                ctx.json(KeyValueReply(key, version, value)).status(HttpStatus.OK_200)
            } else {
                ctx.json(FailureReply("Failed to fetch pair.")).status(HttpCode.INTERNAL_SERVER_ERROR) // TODO: Make more descriptive
            }

        }

        /* Handle invalid requests */
        app.error(404) { ctx ->
            ctx.result("""
                Invalid Request.
                Valid Requests:
                GET /fetch/{key}/{version}: Fetch the value corresponding to the given key-version pair
                POST /store/{key}/{version} with request body ""value"": Store the value corresponding to the given key-version pair
                """.trimIndent())
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

/**
 * Represents a failure in a HTTP response.
 */
data class FailureReply(val message: String, val success: Boolean = false)