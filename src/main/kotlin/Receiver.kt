import interfaces.ICache
import io.javalin.Javalin
import io.javalin.http.Handler
import util.makeKey

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

            cache.store(key = key)
        }
    }

    fun initializeFetch() {
        app.get("/fetch/{key}/{version}") { ctx ->
            val key: String = makeKey(ctx.pathParam("key"), ctx.pathParam("version"))
            ctx.result("Fetching: <$key>...")

            val value: String = cache.fetch(key = key)
            ctx.result("Result: <$key, $value>")
        }
    }
}