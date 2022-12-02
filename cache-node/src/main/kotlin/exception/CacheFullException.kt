package exception

import exception.base.LocalNodeException
import org.eclipse.jetty.http.HttpStatus

class CacheFullException : LocalNodeException(HttpStatus.CONFLICT_409, "Cache is full") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 3
    }
}