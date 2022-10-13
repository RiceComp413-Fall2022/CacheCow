package sender

import KeyVersionPair
import NodeId

/**
 * API to send data from one node to another node. Interface for our internal node
 * communication for the distributed system.
 */
interface ISender {

    fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): String?

    fun storeToNode(kvPair: KeyVersionPair, value: String, destNodeId: NodeId): Boolean

}