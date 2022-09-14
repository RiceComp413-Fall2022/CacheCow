import io.javalin.Javalin

fun main() {
    val app = Javalin.create().start(7070)

    app.get("/") {
            ctx -> ctx.result("Commands:\n" +
            "/hello_world: Prints 'hello world'.\n" +
            "/store/{key}: Stores the 'key'.\n" +
            "/fetch/{key}: Fetch the 'key'.")
    }

    app.get("/hello_world") {
        ctx -> ctx.result("Hello World!")
    }

    app.get("/store/{key}") {
            ctx -> ctx.result("Storing: " + ctx.pathParam("key"))
    }

    app.get("/fetch/{key}") {
            ctx -> ctx.result("Fetching: " + ctx.pathParam("key"))
    }
}