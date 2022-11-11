package receiver

import BulkCopyRequest
import KeyValuePair
import NodeId
import ScalableMessage
import ScalableMessageType
import cache.distributed.IScalableDistributedCache
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jetty.http.HttpStatus
import java.io.File
import java.util.*

class ScalableReceiver(port: Int, nodeId: NodeId, private var nodeCount: Int, distributedCache: IScalableDistributedCache): Receiver(port, nodeId, nodeCount, distributedCache), IScalableReceiver {

    /**
     * Flag indicating whether the current node intends to launch
     */
    private var desireToLaunch = false

    /**
     * Flag indicating whether the scaling process has begun
     */
    private var scaleInProgress = nodeId == nodeCount

    /**
     * Timer used to ensure that nodes achieve launching consensus
     */
    private var launchTimer = Timer()

    /**
     * Minimum id of a node that is currently intending to launch another node
     */
    private var minLaunchingNode = nodeCount

    /**
     * TODO: Remove
     */
    private val mapper: ObjectMapper = ObjectMapper()

    init {

        /* Handle simple message passing for coordinating scaling process */
        app.post("/v1/inform") { ctx ->
            print("\n*********INFORM REQUEST*********\n")
            val message = ctx.bodyAsClass(ScalableMessage::class.java)

            print("SCALABLE RECEIVER: Deserialized message from node ${message.nodeId}\n")

            if (!scaleInProgress && message.type != ScalableMessageType.LAUNCH_NODE) {
                throw simpleValidationException("Scaling not currently in progress")
            }

            if (message.nodeId < 0 || message.nodeId > nodeCount) {
                throw simpleValidationException("Invalid node id")
            }

            if (message.type == ScalableMessageType.COPY_COMPLETE || message.type == ScalableMessageType.LAUNCH_NODE) {
                if (message.nodeId == nodeCount) {
                    throw simpleValidationException("New node cannot send this message type")
                }
            } else {
                if (message.nodeId != nodeCount) {
                    throw simpleValidationException("Only new node can send this message type")
                }
            }

            var accepted = true
            if (message.type == ScalableMessageType.LAUNCH_NODE) {
                print("SCALABLE RECEIVER: Got LAUNCH_NODE request from node ${message.nodeId}\n")
                // Sender intends to launch a new node
                scaleInProgress = true
                if (message.nodeId < minLaunchingNode) {
                    minLaunchingNode = message.nodeId
                    if (desireToLaunch) {
                        desireToLaunch = false
                        launchTimer.cancel()
                    }
                } else {
                    accepted = false
                }
            } else if (message.type == ScalableMessageType.READY) {
                print("SCALABLE RECEIVER: Got READY request from node ${message.nodeId}\n")
                // New node just booted up and is ready to receive copied values
                if (message.hostName.isBlank()) {
                    throw simpleValidationException("Missing host name")
                }
                distributedCache.initiateCopy(message.hostName)
            } else if (message.type == ScalableMessageType.COPY_COMPLETE) {
                print("SCALABLE RECEIVER: Got COPY_COMPLETE request from node ${message.nodeId}\n")
                // Sender finished copying values to this node
                if (nodeId != nodeCount) {
                    throw simpleValidationException("Only new node can accept this message type")
                }
                distributedCache.markCopyComplete(message.nodeId)
            } else if (message.type == ScalableMessageType.SCALE_COMPLETE) {
                print("SCALABLE RECEIVER: Got SCALE_COMPLETE request from node ${message.nodeId}\n")
                // New node has received all copied values, scaling has completed
                scaleInProgress = false
                nodeCount++
                minLaunchingNode = nodeCount
            }

            if (accepted) {
                ctx.result("Accepted message").status(HttpStatus.ACCEPTED_202)
            } else {
                ctx.result("Rejected message").status(HttpStatus.CONFLICT_409)
            }
        }

        /* Handle bulk copy requests */
        app.post("/v1/bulk-copy") { ctx ->
            print("SCALABLE RECEIVER: Received bulk copy request\n")
            val bulkCopy: BulkCopyRequest = ctx.bodyAsClass(BulkCopyRequest::class.java)

            if (!scaleInProgress || nodeId != nodeCount) {
                throw simpleValidationException("New node can only receive bulk copy requests while scaling is in progress")
            }
            if (bulkCopy.nodeId < 0 || bulkCopy.nodeId >= nodeCount) {
                throw simpleValidationException("Invalid node id")
            }
            distributedCache.bulkLocalStore(bulkCopy.values)
        }

        /**
         * Test endpoint to launch a node, note this would normally be embedded in cache
         * along with fullness criteria
         */
        app.get("/v1/launch-node") { ctx ->
            print("*********LAUNCH NODE REQUEST*********\n")

            if (minLaunchingNode == nodeCount) {
                desireToLaunch = true
                scaleInProgress = true

                // Broadcast launch intentions to all other nodes (should be async)
                distributedCache.broadcastLaunchIntentions()

                // If this node has minimum node id, launch new node
                launchTimer.schedule(object : TimerTask() {
                    override fun run() {
                        print("SCALABLE SENDER: Launching new node\n")
                        launchNode()
                    }
                }, 2 * 1000)
            }

            ctx.status(HttpStatus.OK_200)
        }

        /**
         * Test endpoint to check json of bulk copy request
         */
        app.get("/v1/test-bulk-send") { ctx ->
            val kvPairs = BulkCopyRequest(
                nodeId,
                mutableListOf(
                    KeyValuePair(
                        "1",
                        1,
                        "1".encodeToByteArray()
                    )
                )
            )
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
        pb.start()
    }

}