package receiver

import CopyClass
import KeyValuePair
import cache.distributed.IScalableDistributedCache
import com.fasterxml.jackson.databind.ObjectMapper
import node.Node
import org.eclipse.jetty.http.HttpStatus
import java.io.File
import java.util.*

class ScalableReceiver(port: Int, private val nodeCount: Int, node: Node, distributedCache: IScalableDistributedCache): Receiver(port, nodeCount, node, distributedCache), IScalableReceiver {

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

    private val mapper: ObjectMapper = ObjectMapper()

    init {
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

        app.post("/v1/test-bulk-receive") { ctx ->
            val kvPairs: CopyClass = ctx.bodyAsClass(CopyClass::class.java)
            val values = kvPairs.values
            print("${values[0].key}, ${values[0].version}, ${values[0].value}\n")
        }

        app.get("/v1/test-bulk-send") { ctx ->
            val kvPairs = CopyClass(mutableListOf(KeyValuePair("1", 1, "1".encodeToByteArray())))
            ctx.result(mapper.writeValueAsString(kvPairs)).status(HttpStatus.OK_200)
        }
    }

    /**
     * Should eventually be abstracted in different class (NodeLauncher)
     */
    override fun launchNode() {
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

}