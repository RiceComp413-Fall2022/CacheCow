package exception
import io.javalin.http.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

class KeyNotFoundException(key: String): HttpResponseException(HttpStatus.NOT_FOUND_404, "Key $key not found in cache") {
}