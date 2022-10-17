package sender

import KeyVersionPair
import NodeId
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A concrete sender that sends HTTP requests.
 */
class Sender(private val nodeId: NodeId) : ISender {

    /**
     * The ObjectMapper used to encode JSON data
     */
    private val mapper: ObjectMapper = ObjectMapper()

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): String? {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")

        val client = HttpClient.newBuilder().build()

        val destUrl = URI.create("http://localhost:${7070 + destNodeId}/fetch/${kvPair.key}/${kvPair.version}?senderId=${nodeId}")
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .GET()
            .build()

        print("SENDER: Sending fetch request to $destUrl\n")

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() in 400..599) {
            return null
        }

        val jsonResponse = mapper.readTree(response.body())

        return jsonResponse.get("value").textValue()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: String, destNodeId: NodeId): Boolean {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")

        val client = HttpClient.newBuilder().build()

        val destUrl = URI.create("http://localhost:${7070 + destNodeId}/store/${kvPair.key}/${kvPair.version}?senderId=${nodeId}")
        val requestBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        print("SENDER: Sending store request to $destUrl\n")

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() in 400..599) {
            return false
        }

        return true
    }

}