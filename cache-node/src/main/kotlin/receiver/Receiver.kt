package receiver

import KeyVersionPair
import cache.distributed.IDistributedCache
import exception.CacheNodeException
import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.plugin.bundled.CorsContainer
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.validation.ValidationError
import io.javalin.validation.ValidationException
import node.Node
import org.eclipse.jetty.http.HttpStatus
import java.io.File
import java.util.*

/**
 * A concrete receiver that accepts requests over HTTP.
 */
class Receiver(private val port: Int, private val nodeCount: Int, private val node: Node, private val distributedCache: IDistributedCache) : IReceiver {

    /**
     * The Javalin server used to route HTTP requests to handlers
     */
    val app: Javalin = Javalin.create { config: JavalinConfig ->
        config.plugins.enableCors { cors: CorsContainer ->
            cors.add { it: CorsPluginConfig -> it.anyHost() }
        }
    }

    private var launchTimer = Timer()

    /**
     * Flag indicating whether the current node intends to launch
     */
    private var desireToLaunch = false

    /**
     * Flag indicating whether the current node is copying to the new node
     */
    private var copyingInProgress = false

    /**
     * Minimum id of a node that is currently intending to launch another node
     */
    private var minLaunchingNode = nodeCount

    /**
     * Count the number of requests that are received
     */
    private var receiverUsageInfo: ReceiverUsageInfo =
        ReceiverUsageInfo(0, 0, 0, 0,  0)


    init {
        app.get("/v1/hello-world") { ctx ->
            ctx.result("Hello, World!").status(HttpStatus.OK_200)
        }

        /* Handle fetch requests */
        app.get("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********FETCH REQUEST*********\n")
            receiverUsageInfo.fetchAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass("version", Int::class.java)
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass("senderId", Int::class.java)
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = distributedCache.fetch(KeyVersionPair(key, version), senderNum)
            receiverUsageInfo.fetchSuccesses++

            ctx.result(value).status(HttpStatus.OK_200)
        }

        /* Handle store requests */
        app.post("/v1/blobs/{key}/{version}") { ctx ->
            print("\n*********STORE REQUEST*********\n")
            receiverUsageInfo.storeAttempts++

            val key = ctx.pathParam("key")
            val version = ctx.pathParamAsClass("version", Int::class.java)
                .check({ it >= 0}, "Version number cannot be negative")
                .get()
            val senderNum = if (ctx.queryParam("senderId") == null) null else
                ctx.queryParamAsClass("senderId", Int::class.java)
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            val value = ctx.bodyAsBytes()
            if (value.isEmpty()) {
                throw ValidationException(mapOf("REQUEST_BODY" to listOf(ValidationError("Binary blob cannot be empty"))))
            }
            distributedCache.store(KeyVersionPair(key, version), value, senderNum)
            receiverUsageInfo.storeSuccesses++

            ctx.json(KeyVersionReply(key, version)).status(HttpStatus.CREATED_201)
        }

        /* Handle requests to monitor information about the node of this receiver */
        app.get("/v1/node-info") { ctx ->
            print("\n*********NODE INFO REQUEST*********\n")
            ctx.json(node.getNodeInfo()).status(HttpStatus.OK_200)
        }

        /**
         * Test endpoint to launch a node, note this would normally be embedded in cache
         * along with fullness criteria
         */
        app.get("/v1/launch-node") { ctx ->
            print("*********LAUNCH NODE REQUEST*********\n")

            if (minLaunchingNode == nodeCount) {
                desireToLaunch = true
                // Send LAUNCH_NODE request to all other nodes
                // Set timer for a few seconds, if sender num is still
                 launchTimer.schedule(object: TimerTask() {
                     override fun run() {
                         launchNode()
                     }
                 },2 * 1000)
            }

            ctx.status(HttpStatus.OK_200)
        }

        /* Test endpoint to print a message from another node */
        app.post("/v1/inform") { ctx ->
            print("*********NODE INFO REQUEST*********\n")
            val message = ctx.body()

            val senderNum = ctx.queryParamAsClass("senderId", Int::class.java)
                    .check({ it in 0 until nodeCount }, "Sender id must be in range (0, ${nodeCount - 1})")
                    .get()

            if (message == "LAUNCH_NODE") {
                // Node senderNum intends to launch a node
                if (senderNum < minLaunchingNode) {
                    minLaunchingNode = senderNum
                    if (desireToLaunch) {
                        desireToLaunch = false
                        launchTimer.cancel()
                    }
                    ctx.result("ACCEPTED").status(HttpStatus.ACCEPTED_202)
                } else if (senderNum > minLaunchingNode) {
                    ctx.result("REJECTED").status(HttpStatus.CONFLICT_409)
                }
            } else if (message == "BEGIN_COPY") {
                // Node senderNum just booted up and is ready for data
                if (senderNum == nodeCount) {
                    copyingInProgress = true
                    // 1. Increment node count
                    // 2. Begin copying data to new node in small chunks, we can delete
                    // once it has been successfully copied
                }
            }
        }

        app.get("/v1/bulk-copy") { ctx ->
            val message = ctx.body()
        }

        app.exception(CacheNodeException::class.java) { e, ctx ->
            print("RECEIVER: Caught cache node exception with id ${e.getExceptionID()}\n")
            ctx.result(e.message).status(e.status)
        }

        app.exception(ValidationException::class.java) { e, ctx ->
            val firstError = e.errors.asIterable().iterator().next()
            print("RECEIVER: Caught validation exception for field ${firstError.key}\n")

            // TODO: Return message in JSON response body, extract more info from exception
            ctx.result(firstError.value[0].message).status(HttpStatus.BAD_REQUEST_400)
        }

        /* Handle invalid requests */
        app.error(HttpStatus.NOT_FOUND_404) { ctx ->
            if (ctx.result() == "Not found") {
                receiverUsageInfo.invalidRequests++
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
     * Should eventually be abstracted in different class (NodeLauncher)
     */
    fun launchNode() {
        val args = arrayOf("/bin/bash", "-c", "./gradlew run --args '$nodeCount ${7070 + nodeCount}'")
        val pb = ProcessBuilder(*args)
        val currentDirectory = System.getProperty("user.dir")
        print("Command: ${pb.command()}\n")
        print("Current directory: $currentDirectory\n")

        pb.directory(File(currentDirectory))
        pb.redirectOutput(File("$currentDirectory/out.txt"))
        print("$currentDirectory/out.txt")
        pb.start()
    }

    /**
     * Start the HTTP server.
     */
    override fun start() {
        app.start(port)
    }

    override fun getReceiverUsageInfo(): ReceiverUsageInfo {
        return receiverUsageInfo
    }
}

/**
 * Represents a key-version tuple in a HTTP response.
 */
data class KeyVersionReply(val key: String, val version: Int)