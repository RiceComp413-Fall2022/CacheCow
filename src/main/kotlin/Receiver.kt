import interfaces.ICache
import io.javalin.Javalin
import io.javalin.http.Handler
import util.makeKey
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * HTTP receiver to accept user API calls.
 */
class Receiver(private val cache: ICache) {

    val helpMessageHandler: Handler = Handler { ctx ->
        ctx.result(
            // TODO: Ensure that help message corresponds to the correct commands.
"Invalid Request.\n\n" +
        "Valid Requests:\n" +
        "/hello_world: Prints 'Hello World!'. Useful for testing.\n" +
        "/store/{key}/{version}/{value}: Stores the key-value pair: '<{key}|{version}, {value}>'.\n" +
        "/fetch/{key}/{version}: Fetch the value corresponding to key: '{key}|{version}'."
        )
    }

    private val app: Javalin = Javalin.create().start(7070)
        .error(404, helpMessageHandler)

    init {
        initReceiver()
    }

    fun initReceiver() {
        initializeHelloWorld()
        initializeStore()
        initializeFetch()
        initializeRequestNode()
        initializeResponseNode()
    }

    fun initializeHelloWorld() {
        app.get("/hello_world") { ctx ->
            ctx.result("Hello World!")
        }
    }

    fun initializeStore() {
        app.get("/store/{key}/{version}/{value}") { ctx ->
            val key: String = makeKey(ctx.pathParam("key"), ctx.pathParam("version"))
            val value: String = ctx.pathParam("value")
            ctx.result("Storing: <$key, $value>")

            cache.store(key = key, value = value)
        }
    }

    fun initializeFetch() {
        app.get("/fetch/{key}/{version}") { ctx ->
            val key: String = makeKey(ctx.pathParam("key"), ctx.pathParam("version"))

            val value: String? = cache.fetch(key = key)
            ctx.result("Result: <$key, $value>")
        }
    }

    fun initializeRequestNode() {
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

    fun initializeResponseNode() {
        app.get("/response") {
        }
    }
}