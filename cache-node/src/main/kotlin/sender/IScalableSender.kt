package sender

import BulkCopyRequest
import NodeId
import ScalableMessage

interface IScalableSender: ISender {


    /**
     * Add a new host name to the set of reachable hosts.
     * @param hostName URI of new host machine
     */
    fun addHost(hostName: String)

    /**
     * Copy a list of key value pairs to another node.
     * @param kvPairs list of key value pairs
     * @param destNodeId node id of destination node
     * @return boolean indicating success
     */
    fun sendBulkCopy(kvPairs: BulkCopyRequest, destNodeId: NodeId): Boolean

    fun broadcastScalableMessageAsync(message: ScalableMessage): Boolean

    fun broadcastScalableMessage(message: ScalableMessage): Boolean

    fun sendScalableMessage(message: ScalableMessage, destNodeId: NodeId): Boolean
}