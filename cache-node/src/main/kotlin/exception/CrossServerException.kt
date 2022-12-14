package exception
import NodeId
import exception.base.CrossNodeException
import org.eclipse.jetty.http.HttpStatus

/**
 * Exception indicating that this node received a server error when messaging another node
 * in thc cluster.
 */
class CrossServerException(destNodeId: NodeId): CrossNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error", destNodeId) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 2
    }
}