package exception
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

class JSONParseException(): HttpResponseException(HttpStatus.BAD_REQUEST_400, "Unable to parse request JSON") {
}