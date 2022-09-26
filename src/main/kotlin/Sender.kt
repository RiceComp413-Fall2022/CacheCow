import com.fasterxml.jackson.databind.ObjectMapper
import interfaces.ISender
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The sender sends HTTP requests to other nodes. It is our form of communication
 * between nodes in our distributed system.
 */
class Sender : ISender {

    override fun fetchFromNode(key: KeyVersionPair, nodeId: NodeId): String {
        val baseKey = key.key
        val version = key.version.toString()

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + nodeId}/fetch/$baseKey/$version"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val mapper = ObjectMapper()
        val jsonResponse = mapper.readTree(response.body())
        return jsonResponse.get("value").textValue()
    }

    override fun storeToNode(key: KeyVersionPair, value: String, nodeId: NodeId) {
        val baseKey = key.key
        val version = key.version.toString()

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + nodeId}/store/$baseKey/$version/$value"))
            .build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

}