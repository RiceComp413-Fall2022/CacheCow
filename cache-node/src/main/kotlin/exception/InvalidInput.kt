package exception

import org.eclipse.jetty.http.HttpStatus

class InvalidInput(override val message: String): CacheNodeException(HttpStatus.NOT_FOUND_404, message) {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 4
    }
}