package exception

import exception.base.LocalNodeException
import org.eclipse.jetty.http.HttpStatus

/**
 * Exception indicating that the underlying cache cannot hold any additional key-value pairs.
 */
class CacheFullException : LocalNodeException(HttpStatus.CONFLICT_409, "Cache is full") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 3
    }
}