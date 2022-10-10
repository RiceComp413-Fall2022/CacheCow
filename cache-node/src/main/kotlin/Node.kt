import interfaces.ICache
import interfaces.ISender
import service.SimpleDistributedCache
import service.SingleNodeCache

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node(nodeId: NodeId, nodeCount: Int) {

    private val capacity = 2
    private val nodeId: NodeId
    private val nodeCount: Int
    private val cache: ICache
    private val receiver: Receiver
    private val sender: ISender
    private val nodeHasher: NodeHasher

    init {
        print("Initializing node $nodeId on port ${7070 + nodeId} with cache capacity $capacity\n")
        this.nodeId = nodeId
        this.nodeCount = nodeCount
        cache = Cache(capacity)
        sender = Sender(nodeId)
        nodeHasher = NodeHasher(nodeCount)
        receiver = Receiver(this.nodeId, if (nodeCount == 1)
            SingleNodeCache(cache) else SimpleDistributedCache(nodeId, nodeCount, cache, sender, nodeHasher)
        )
    }
}