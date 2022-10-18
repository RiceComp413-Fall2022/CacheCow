package exception
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

class ConnectionRefusedException(): HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error") {
}