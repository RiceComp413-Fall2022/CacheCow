import interfaces.ICache
import interfaces.ISender

/**
 * Node class that contains the receiver, sender and memory cache.
 */
class Node {

    private val cache: ICache
    private val receiver : Receiver
    private val sender : ISender

    init {
        cache = Cache()
        receiver = Receiver(object : ICache {
            override fun store(key: String) {
                cache.store(key)
            }

            override fun fetch(key: String): String {
                return cache.fetch(key)
            }

        })
        sender = Sender()
    }
}