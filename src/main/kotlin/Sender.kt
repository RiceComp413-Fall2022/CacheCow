import com.fasterxml.jackson.databind.ObjectMapper
import interfaces.ISender
import org.eclipse.jetty.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The sender sends HTTP requests to other nodes. It is our form of communication
 * between nodes in our distributed system.
 */
class Sender(nodeId: NodeId) : ISender {

    private val nodeId: NodeId
    private val mapper: ObjectMapper = ObjectMapper()

    init {
        this.nodeId = nodeId
    }

    override fun fetchFromNode(key: KeyVersionPair, destNodeId: NodeId): String {
        val baseKey = key.key
        val version = key.version.toString()
        val client = HttpClient.newBuilder().build()

        val requestBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FetchRequestBody(nodeId))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + destNodeId}/fetch/${baseKey}/${version}"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
            throw io.javalin.http.HttpResponseException(response.statusCode(), response.body())
        }

        val jsonResponse = mapper.readTree(response.body())
        return jsonResponse.get("value").textValue()
    }

    override fun storeToNode(key: KeyVersionPair, value: String, destNodeId: NodeId) {
        val baseKey = key.key
        val version = key.version.toString()
        val client = HttpClient.newBuilder().build()

        val requestBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(StoreRequestBody(value, nodeId))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + destNodeId}/store/${baseKey}/${version}"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response =  client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == HttpStatus.CONFLICT_409) {
            throw io.javalin.http.HttpResponseException(response.statusCode(), response.body())
        }
    }

}