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
            override fun store(key: String, version: Int, value: String) {
                if (!cache.isFull()) {
                    cache.store(key, version, value)
                } else {
                    sender.storeToNode(key, version, value, 1 - nodeID)
                }
            }

            override fun fetch(key: String, version: Int): String {
                var result: String? = cache.fetch(key, version)
                if (result == null) {
                    result = sender.fetchFromNode(key, version, 1 - nodeID)
                }
                return result
            }

            override fun isFull(): Boolean {
                return cache.isFull()
            }
        })
    }
}