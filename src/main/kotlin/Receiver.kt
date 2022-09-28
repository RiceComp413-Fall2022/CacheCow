import interfaces.ICache
import interfaces.IReceiverService
import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * HTTP receiver to accept user API calls.
 */
class Receiver(private val nodeID: Int, private val receiverService: IReceiverService) {

    // TODO: Ensure that help message corresponds to the correct commands.
    private val helpMessageHandler: Handler = Handler { ctx ->
        ctx.result(
"Invalid Request.\n\n" +
        "Valid Requests:\n" +
        "/hello_world: Prints 'Hello World!'. Useful for testing.\n" +
        "/store/{key}/{version}/{value}: Stores the key-value pair: '<{key}|{version}, {value}>'.\n" +
        "/fetch/{key}/{version}: Fetch the value corresponding to key: '{key}|{version}'."
        )
    }

    private val app: Javalin = Javalin.create().start(7070 + this.nodeID)
        .error(404, helpMessageHandler)

    init {
        initReceiver()
    }

    private fun initReceiver() {
        initializeHelloWorld()
        initializeStore()
        initializeClientFetch()
        initializeNodeFetch()
        initializeRequestNode()
        initializeResponseNode()
    }

    private fun initializeHelloWorld() {
        app.get("/hello_world") { ctx ->
            ctx.result("Hello World!")
        }
    }

    private fun initializeStore() {
        app.post("/store/{key}/{version}") { ctx ->
            val key: String = ctx.pathParam("key")
            val version: Int = Integer.parseInt(ctx.pathParam("version"))
            val value: String? = ctx.formParam("value")
            val senderId: String? = ctx.formParam("senderId")

            // TODO: Look into Javalin validators
            if (value == null) {
                ctx.json(HttpResponseException(HttpStatus.BAD_REQUEST_400, "Expecting form key 'value' but none found"))
                return@post
            }

            try {
                if (senderId != null) {
                    receiverService.storeNode(KeyVersionPair(key, version), value, Integer.parseInt(senderId))
                } else {
                    receiverService.storeClient(KeyVersionPair(key, version), value)
                }
                ctx.json(KeyValueReply(key, value))
            } catch (e: HttpResponseException) {
                ctx.json(e.message!!).status(e.status)
            }
        }
    }

    private fun initializeClientFetch() {
        app.get("/fetch/{key}/{version}") { ctx ->
            val key: String = ctx.pathParam("key")
            val version: Int = Integer.parseInt(ctx.pathParam("version"))

            try {
                val value: String = receiverService.fetchClient(KeyVersionPair(key, version))
                ctx.json(KeyValueReply(key, value))
            } catch (e: HttpResponseException) {
                ctx.json(e.message!!).status(e.status)
            }
        }
    }

    private fun initializeNodeFetch() {
        app.post("/fetch/{key}/{version}") { ctx ->
            val key: String = ctx.pathParam("key")
            val version: Int = Integer.parseInt(ctx.pathParam("version"))
            val senderId: String? = ctx.formParam("senderId")

            if (senderId == null) {
                ctx.json(HttpResponseException(HttpStatus.BAD_REQUEST_400, "Expecting form key 'senderId' but none found"))
                return@post
            }

            try {
                val value: String = receiverService.fetchNode(KeyVersionPair(key, version), Integer.parseInt(senderId))
                ctx.json(KeyValueReply(key, value))
            } catch (e: HttpResponseException) {
                ctx.json(e.message!!).status(e.status)
            }
        }
    }

    private fun initializeRequestNode() {
        app.get("/request") { ctx ->

            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/response"))
                .build()
            val response = client.send(request, BodyHandlers.ofString())
            println(response.body())
            if (response.statusCode() in 200..299) {
                ctx.result("Successful HTTP Request")
            }
            else {
                ctx.result("Unsuccessful HTTP Request")
            }
        }
    }

    private fun initializeResponseNode() {
        app.get("/response") {
        }
    }
}