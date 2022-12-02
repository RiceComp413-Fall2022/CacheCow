package exception

import exception.base.CacheNodeException
import org.eclipse.jetty.http.HttpStatus

/**
 * Exception indicating that a internal program assumption was violated.
 */
class ProgramAssumptionException(override val message: String): CacheNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, message) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 5
    }
}