import interfaces.IReceiverService
import org.eclipse.jetty.http.HttpStatus

class SingleNodeService(cache: Cache): IReceiverService {

    private val cache: Cache


    init {
        this.cache = cache
    }

    override fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?) {
        if (cache.isFull()) {
            throw io.javalin.http.HttpResponseException(HttpStatus.CONFLICT_409, "No space available")
        }
        cache.store(kvPair, value)
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String {
        return cache.fetch(kvPair)
            ?: throw io.javalin.http.HttpResponseException(HttpStatus.NOT_FOUND_404, "Value not found")
    }
}