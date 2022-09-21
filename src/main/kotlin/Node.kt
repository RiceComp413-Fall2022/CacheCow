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
        receiver = Receiver(object : ICache {
            override fun store(key: String, value: String) {
                cache.store(key, value)
            }

            override fun fetch(key: String): String? {
                return cache.fetch(key)
            }

        })
        sender = Sender()
    }
}