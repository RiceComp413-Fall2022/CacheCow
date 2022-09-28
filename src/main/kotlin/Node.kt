import interfaces.ICache
import interfaces.IReceiverService
import interfaces.ISender
import org.eclipse.jetty.client.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

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
        receiver = Receiver(this.nodeId, object : IReceiverService {
            override fun storeClient(kvPair: KeyVersionPair, value: String) {
                 // TODO: hash collision handling might have to be done here, because
                 //  nodeHasher does not know whether a node is full. We might want to
                 //  add that functionality in nodeHasher if don't want to handle that here
                 //  We will most likely want to have the sender ask other nodes if they're
                 //  full
                val primaryNodeId = nodeHasher.primaryHash(kvPair)

                if (primaryNodeId == nodeId && !cache.isFull()) {
                    cache.store(kvPair, value)
                    return
                }

                val destNodeId = if (primaryNodeId == nodeId) nodeHasher.secondaryHash(kvPair) else primaryNodeId
                sender.storeToNode(
                    kvPair,
                    value,
                    destNodeId
                )
            }

            override fun storeNode(kvPair: KeyVersionPair, value: String, senderId: NodeId) {

                if (!cache.isFull()) {
                    cache.store(kvPair, value)
                    return
                }

                val primaryNodeId = nodeHasher.primaryHash(kvPair)

                if (senderId != primaryNodeId) {
                    sender.storeToNode(
                        kvPair,
                        value,
                        nodeHasher.secondaryHash(kvPair)
                    )
                }

                throw io.javalin.http.HttpResponseException(HttpStatus.CONFLICT_409, "No space available")
            }

            override fun fetchClient(kvPair: KeyVersionPair): String {
                val primaryNodeId = nodeHasher.primaryHash(kvPair)
                val value: String?

                if (primaryNodeId == nodeId) {
                    value = cache.fetch(kvPair)
                    if (value != null) {
                        return value
                    }
                }

                val destNodeId = if (primaryNodeId == nodeId) nodeHasher.secondaryHash(kvPair) else primaryNodeId

                return sender.fetchFromNode(
                    kvPair,
                    destNodeId
                )
            }

            override fun fetchNode(kvPair: KeyVersionPair, senderId: NodeId): String {
                val value = cache.fetch(kvPair)

                if (value != null) {
                    return value
                }

                val primaryNodeId = nodeHasher.primaryHash(kvPair)

                if (senderId != primaryNodeId) {
                    return sender.fetchFromNode(kvPair, nodeHasher.secondaryHash(kvPair))
                }

                throw io.javalin.http.HttpResponseException(HttpStatus.NOT_FOUND_404, "Value not found")
            }
        })
    }
}