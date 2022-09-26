import interfaces.ICache
import interfaces.ISender

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node(nodeID: Int) {

    private val nodeID: Int
    private val cache: ICache
    private val receiver : Receiver
    private val sender : ISender

    init {
        this.nodeID = nodeID
        cache = Cache()
        sender = Sender()
        receiver = Receiver(this.nodeID, object : ICache {
            override fun store(key: KeyVersionPair, value: String) {
                if (!cache.isFull()) {
                    cache.store(key, value)
                } else {
                    sender.storeToNode(key, value, 1 - nodeID)
                }
            }

            override fun fetch(key: KeyVersionPair): String {
                var result: String? = cache.fetch(key)
                if (result == null) {
                    result = sender.fetchFromNode(key, 1 - nodeID)
                }
                return result
            }

            override fun isFull(): Boolean {
                return cache.isFull()
            }
        })
    }
}