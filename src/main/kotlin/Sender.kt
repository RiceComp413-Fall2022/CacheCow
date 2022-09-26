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

    override fun fetchFromNode(key: String, version: Int, nodeId: Int): String {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + nodeId}/fetch/$key/$version"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val mapper = ObjectMapper()
        val jsonResponse = mapper.readTree(response.body())
        return jsonResponse.get("value").textValue()
    }

    override fun storeToNode(key: String, version: Int, value: String, nodeId: Int) {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${7070 + nodeId}/store/$key/$version/$value"))
            .build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

}