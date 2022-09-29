package interfaces

import KeyVersionPair
import NodeId

/**
 * API to send data from one node to another node. Interface for our internal node
 * communication for the distributed system.
 */
interface ISender {

    // TODO: Add API methods as appropriate.
    // TODO: Determine Node Ids.

    fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): String

    fun storeToNode(kvPair: KeyVersionPair, value: String, destNodeId: NodeId)

}