package exception

import exception.base.CacheNodeException
import org.eclipse.jetty.http.HttpStatus

class InvalidInputException(override val message: String): CacheNodeException(HttpStatus.CONFLICT_409, message) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 3
    }
}