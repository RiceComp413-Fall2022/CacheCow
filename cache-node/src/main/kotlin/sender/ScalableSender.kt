package sender

import BulkCopyRequest
import NodeId
import ScalableMessage
import exception.ConnectionRefusedException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ScalableSender(private val nodeId: NodeId, private val nodeList: List<String>): Sender(nodeId, nodeList), IScalableSender {

    // TODO: Add retry logic since failures a less tolerable

    override fun sendBulkCopy(kvPairs: BulkCopyRequest, destNodeId: NodeId): Boolean {
        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/bulk-copy")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(kvPairs)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        print("SCALABLE SENDER: Sending bulk copy request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        return response.statusCode() !in 400..599
    }

    override fun broadcastScalableMessage(message: ScalableMessage): Boolean {

        print("SCALABLE SENDER: Sending broadcast message ${message.type}\n")

        for (i in nodeList.indices) {
            if (i != nodeId) {
               if (!broadcastHelper(message, i)) {
                   return false
               }
            }
        }
        return true
    }

    private fun broadcastHelper(message: ScalableMessage, destNodeId: NodeId): Boolean {
        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/bulk-copy")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response: HttpResponse<String>
        var success = false
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
            success = response.statusCode() !in 400..599
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
        }

        return success
    }
}