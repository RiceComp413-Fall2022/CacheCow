package sender

import BulkCopyRequest
import NodeId
import ScalableMessage

interface IScalableSender: ISender {

    /**
     * Copy a list of key value pairs to another node.
     * @param kvPairs list of key value pairs
     * @param destNodeId node id of destination node
     */
    fun sendBulkCopy(kvPairs: BulkCopyRequest, destNodeId: NodeId)

    fun sendScalableMessage(message: ScalableMessage, destNodeId: NodeId)

    fun broadcastScalableMessageAsync(message: ScalableMessage)
}