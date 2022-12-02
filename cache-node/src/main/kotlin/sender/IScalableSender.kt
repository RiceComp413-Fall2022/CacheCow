package sender

import BulkCopyRequest
import NodeId
import ScalableMessage

/**
 * An interface providing message passing and broadcasting required to support node scaling.
 */
interface IScalableSender: ISender {

    /**
     * Copies a list of key value pairs to the given node.
     *
     * @param kvPairs list of key value pairs
     * @param destNodeId node id of destination node
     */
    fun sendBulkCopy(kvPairs: BulkCopyRequest, destNodeId: NodeId)

    /**
     * Sends a scalable message to the given node.
     *
     * @param message scalable message to send
     * @param destNodeId node id of destination
     */
    fun sendScalableMessage(message: ScalableMessage, destNodeId: NodeId)

    /**
     * Broadcasts a scalable message to all other nodes in cluster, sending each message
     * asynchronously.
     *
     * @param message scalable message to broadcast.
     */
    fun broadcastScalableMessageAsync(message: ScalableMessage)
}