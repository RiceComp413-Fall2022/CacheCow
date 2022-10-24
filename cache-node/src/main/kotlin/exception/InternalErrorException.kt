package exception
import org.eclipse.jetty.http.HttpStatus

class InternalErrorException(): CacheNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error") {

    /**
     * Returns the exception id unique to the exception type.
     */
    override fun getExceptionID(): Int {
        return 1
    }
}