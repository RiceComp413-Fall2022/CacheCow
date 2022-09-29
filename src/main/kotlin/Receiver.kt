import com.fasterxml.jackson.databind.ObjectMapper
import interfaces.IDistributedCache
import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.http.HttpCode
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.logging.Logger

/**
 * HTTP receiver to accept user API calls.
 */
class Receiver(private val nodeID: Int, private val receiverService: IDistributedCache) {

    // TODO: Ensure that help message corresponds to the correct commands.
    private val helpMessageHandler: Handler = Handler { ctx ->
        ctx.result(
"Invalid Request.\n\n" +
        "Valid Requests:\n" +
        "GET /hello_world: Prints 'Hello World!'. Useful for testing.\n" +
        "GET /fetch/{key}/{version}: Fetch the value corresponding to key: '{key}|{version}'." +
        "POST /store/{key}/{version} with request body {value}: Stores the key-value pair: '<{key}|{version}, {value}>'.\n"
        )
    }

    private val mapper: ObjectMapper = ObjectMapper()

    private val app: Javalin = Javalin.create().start(7070 + this.nodeID)
        .error(404, helpMessageHandler)

    init {
        initReceiver()
    }

    private fun initReceiver() {
        initializeHelloWorld()
        initializeStore()
        initializeFetch()
        initializeRequestNode()
        initializeResponseNode()
    }

    private fun initializeHelloWorld() {
        app.get("/hello_world") { ctx ->
            ctx.result("Hello World!")
        }
    }

    private fun initializeFetch() {
        app.get("/fetch/{key}/{version}") { ctx ->
            print("*********FETCH REQUEST*********\n")
            val key: String = ctx.pathParam("key")
            val version: Int = Integer.parseInt(ctx.pathParam("version"))
            val senderId: String? = ctx.queryParam("senderId")

            try {
                val senderNum = if (senderId == null) null else Integer.parseInt(senderId)
                val value: String = receiverService.fetch(KeyVersionPair(key, version), senderNum)
                ctx.json(KeyValueReply(key, value)).status(HttpStatus.OK_200)
            } catch (e: HttpResponseException) {
                ctx.json(e.message!!).status(e.status)
            }
        }
    }

    private fun initializeStore() {
        app.post("/store/{key}/{version}") { ctx ->
            print("*********STORE REQUEST*********\n")
            val key: String = ctx.pathParam("key")
            val version: Int = Integer.parseInt(ctx.pathParam("version"))
            val senderId: String? = ctx.queryParam("senderId")
            val value: String? = if (ctx.body() == "") null else mapper.readTree(ctx.body()).textValue()

            // TODO: Look into Javalin validators
            if (value == null) {
                print(ctx.body())
                ctx.json("Expecting request body but none found").status(HttpCode.BAD_REQUEST)
                return@post
            }

            try {
                val senderNum = if (senderId == null) null else Integer.parseInt(senderId)
                receiverService.store(KeyVersionPair(key, version), value, senderNum)
                ctx.json(KeyValueReply(key, value)).status(HttpStatus.CREATED_201)
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