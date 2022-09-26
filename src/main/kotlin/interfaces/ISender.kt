package interfaces

/**
 * API to send data from one node to another node. Interface for our internal node
 * communication for the distributed system.
 */
interface ISender {

    // TODO: Add API methods as appropriate.
    // TODO: Determine Node Ids.

    fun fetchFromNode(key: String, version: Int, nodeId: Int): String

    fun storeToNode(key: String, version: Int, value: String, nodeId: Int)

}