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

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): String {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")

        val baseKey = kvPair.key
        val version = kvPair.version.toString()
        val client = HttpClient.newBuilder().build()

        val destUrl = URI.create("http://localhost:${7070 + destNodeId}/fetch/${baseKey}/${version}?senderId=${nodeId}")
        print("SENDER: Sending fetch request to $destUrl\n")

        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")
        if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
            throw io.javalin.http.HttpResponseException(response.statusCode(), response.body())
        }

        val jsonResponse = mapper.readTree(response.body())
        return jsonResponse.get("value").textValue()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: String, destNodeId: NodeId) {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")

        val baseKey = kvPair.key
        val version = kvPair.version.toString()
        val client = HttpClient.newBuilder().build()

        val destUrl = URI.create("http://localhost:${7070 + destNodeId}/store/${baseKey}/${version}?senderId=${nodeId}")
        print("SENDER: Sending store request to $destUrl\n")

        val requestBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")
        if (response.statusCode() in 400..599) {
            throw io.javalin.http.HttpResponseException(response.statusCode(), response.body())
        }
    }

}