import io.javalin.Javalin

//fun main() {
//    val app = Javalin.create().start(7070)
//    app.get("/") { ctx -> ctx.result("Hello World") }
//}

fun main() {
    val app = Javalin.create().start(7070)
    val data: String = "hi"
    app.get("/") {
        ctx -> ctx.result("Value: $data")
    }
    app.get("/path") {
            ctx -> ctx.result("Other Path")
    }
}