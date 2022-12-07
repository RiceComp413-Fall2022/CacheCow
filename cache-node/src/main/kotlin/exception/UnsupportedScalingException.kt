import exception.base.LocalNodeException
import org.eclipse.jetty.http.HttpStatus

/**
 * Exception indicating that the requested operation is not supported as the cluster is
 * currently scaling.
 */
class UnsupportedScalingException(override val message: String): LocalNodeException(
    HttpStatus.CONFLICT_409, message) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 6
    }
}