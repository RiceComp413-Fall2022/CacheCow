package receiver

import BulkCopyRequest
import NodeId
import ScalableMessage
import ScalableMessageType
import cache.distributed.IScalableDistributedCache
import org.eclipse.jetty.http.HttpStatus

class ScalableReceiver(port: Int, nodeId: NodeId, private var nodeCount: Int, private val distributedCache: IScalableDistributedCache): Receiver(
    port,
    nodeCount,
    distributedCache
), IScalableReceiver {

    init {

        /* Handle simple message passing for coordinating scaling process */
        app.post("/v1/inform") { ctx ->
            print("\n*********INFORM REQUEST*********\n")
            val message = ctx.bodyAsClass(ScalableMessage::class.java)

            print("SCALABLE RECEIVER: Deserialized message from node ${message.nodeId}\n")

            if (!distributedCache.scaleInProgress() && message.type != ScalableMessageType.LAUNCH_NODE) {
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
            when (message.type) {
                ScalableMessageType.LAUNCH_NODE -> {
                    print("SCALABLE RECEIVER: Got LAUNCH_NODE request from node ${message.nodeId}\n")
                    // Sender intends to launch a new node
                    accepted = distributedCache.handleLaunchRequest(message.nodeId)
                }

                ScalableMessageType.READY -> {
                    print("SCALABLE RECEIVER: Got READY request from node ${message.nodeId}\n")
                    // New node just booted up and is ready to receive copied values
                    if (message.hostName.isBlank()) {
                        throw simpleValidationException("Missing host name")
                    }
                    distributedCache.initiateCopy(message.hostName)
                }

                ScalableMessageType.COPY_COMPLETE -> {
                    print("SCALABLE RECEIVER: Got COPY_COMPLETE request from node ${message.nodeId}\n")
                    // Sender finished copying values to this node
                    if (nodeId != nodeCount) {
                        throw simpleValidationException("Only new node can accept this message type")
                    }
                    distributedCache.markCopyComplete(message.nodeId)
                }

                ScalableMessageType.SCALE_COMPLETE -> {
                    print("SCALABLE RECEIVER: Got SCALE_COMPLETE request from node ${message.nodeId}\n")
                    // New node has received all copied values, scaling has completed
                    distributedCache.handleScaleCompleteRequest()
                    nodeCount++
                }
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

            if (!distributedCache.scaleInProgress() || nodeId != nodeCount) {
                throw simpleValidationException("New node can only receive bulk copy requests while scaling is in progress")
            }
            if (bulkCopy.nodeId < 0 || bulkCopy.nodeId >= nodeCount) {
                throw simpleValidationException("Invalid node id")
            }
            distributedCache.bulkLocalStore(bulkCopy.values)
        }
    }

    override fun start() {
        super.start()
        distributedCache.start()
    }
}