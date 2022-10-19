package exception
import org.eclipse.jetty.http.HttpStatus

class KeyNotFoundException(key: String): CacheNodeException(HttpStatus.NOT_FOUND_404, "Key $key not found in cache") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 2
    }
}