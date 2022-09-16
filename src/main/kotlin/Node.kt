import io.javalin.Javalin

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node {

}
fun main() {
    val app = Javalin.create().start(7070)
    val memoryStore = Cache()

    app.get("/") { ctx ->
        ctx.result(
            "Commands:\n" +
                    "/hello_world: Prints 'hello world'.\n" +
                    "/store/{key}: Stores the 'key'.\n" +
                    "/fetch/{key}: Fetch the 'key'."
        )
    }

    app.get("/hello_world") { ctx ->
        ctx.result("Hello World!")
    }

    app.get("/store/{key}") { ctx ->
        val key: String = ctx.pathParam("key")
        ctx.result("Storing: " + key)

        memoryStore.store(key = key)
    }

    app.get("/fetch/{key}") { ctx ->
        val key: String = ctx.pathParam("key")
        ctx.result("Fetching: " + key)

        memoryStore.fetch(key = key)
    }
}