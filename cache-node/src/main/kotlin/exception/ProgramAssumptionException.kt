package exception

import org.eclipse.jetty.http.HttpStatus

class ProgramAssumptionException(override val message: String): CacheNodeException(HttpStatus.INTERNAL_SERVER_ERROR_500, message) {

    override fun getExceptionID(): Int {
        return 3
    }
}