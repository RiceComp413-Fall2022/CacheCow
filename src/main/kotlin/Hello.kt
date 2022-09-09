import io.javalin.Javalin

fun main() {
    val app = Javalin.create().start(7070)
    app.get("/") { ctx -> ctx.result("Hello World") }
}