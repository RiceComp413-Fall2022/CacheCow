package exception
import NodeId
import exception.base.CrossNodeException
import org.eclipse.jetty.http.HttpStatus

class ConnectionRefusedException(destNodeId: NodeId): CrossNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error", destNodeId) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 0
    }
}