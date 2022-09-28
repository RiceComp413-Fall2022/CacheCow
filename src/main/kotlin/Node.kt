import interfaces.ICache
import interfaces.ISender
import service.RoundRobinService
import service.SingleNodeService

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node(nodeId: NodeId, nodeCount: Int) {

    private val nodeId: NodeId
    private val nodeCount: Int
    private val cache: ICache
    private val receiver: Receiver
    private val sender: ISender
    private val nodeHasher: NodeHasher

    init {
        this.nodeId = nodeId
        this.nodeCount = nodeCount
        cache = Cache()
        sender = Sender(nodeId)
        nodeHasher = NodeHasher(nodeCount)
        receiver = Receiver(this.nodeId, if (nodeCount == 1)
            SingleNodeService(cache) else RoundRobinService(nodeId, nodeCount, cache, sender, nodeHasher)
        )
    }
}