package exception
import NodeId
import org.eclipse.jetty.http.HttpStatus

class BroadcastException(private val nodeId: NodeId): CacheNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 3
    }

    fun getNodeId(): NodeId {
        return nodeId
    }
}