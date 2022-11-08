package sender

import KeyVersionPair
import NodeId
import exception.ConnectionRefusedException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ScalableSender(private val nodeId: NodeId, private val nodeList: List<String>): Sender(nodeId, nodeList), IScalableSender {

    override fun copyKvPairs(kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>, destNodeId: NodeId): Boolean {
        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/bulk-copy")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(kvPairs)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        print("SENDER: Sending bulk copy request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        // TODO: Error handling
        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        return !(response.statusCode() in 400..599)
    }
}