package exception
import exception.base.LocalNodeException
import org.eclipse.jetty.http.HttpStatus

/**
 * Exception indicating a cache miss.
 */
class KeyNotFoundException(key: String): LocalNodeException(HttpStatus.NOT_FOUND_404, "Key $key not found in cache") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 4
    }
}