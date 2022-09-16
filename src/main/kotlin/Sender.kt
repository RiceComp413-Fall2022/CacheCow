import interfaces.ISender

/**
 * The sender sends HTTP requests to other nodes. It is our form of communication
 * between nodes in our distributed system.
 */
class Sender : ISender {

    override fun fetchFromNode(key: String, nodeId: Int) {
        TODO("Not yet implemented")
    }

}