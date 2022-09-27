import interfaces.ICache
import interfaces.ISender

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node(nodeID: Int, private val nodeHasher: NodeHasher) {

    private val nodeID: Int
    private val cache: ICache
    private val receiver: Receiver
    private val sender: ISender

    init {
        this.nodeID = nodeID
        cache = Cache()
        sender = Sender()
        receiver = Receiver(this.nodeID, object : ICache {
            override fun store(kvPair: KeyVersionPair, value: String) {
                val goalNodeID = nodeHasher.hash(kvPair)
                // TODO: hash collision handling might have to be done here, because
                //  nodeHasher does not know whether a node is full. We might want to
                //  add that functionality in nodeHasher if don't want to handle that here
                //  We will most likely want to have the sender ask other nodes if they're
                //  full

                if (goalNodeID == nodeID) {
                    cache.store(kvPair, value)
                } else {
                    sender.storeToNode(kvPair, value, goalNodeID)
                }
            }

            override fun fetch(kvPair: KeyVersionPair): String {
                val goalNodeID = nodeHasher.hash(kvPair)
                // TODO: see above todo

                val result: String? =
                    if (goalNodeID == nodeID) {
                        cache.fetch(kvPair)
                    } else {
                        sender.fetchFromNode(kvPair, goalNodeID)
                    }

                if (result == null) {
                    throw Exception("Key-version pair not found")
                }
                return result
            }

            override fun isFull(): Boolean {
                return cache.isFull()
            }
        })
    }
}