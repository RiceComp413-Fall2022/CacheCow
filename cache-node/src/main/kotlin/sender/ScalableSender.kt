package sender

import BulkCopyRequest
import NodeId
import ScalableMessage
import exception.BroadcastException
import exception.ConnectionRefusedException
import org.eclipse.jetty.webapp.MetaData.Complete
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import kotlin.streams.asStream
import kotlin.streams.toList

class ScalableSender(private val nodeId: NodeId, private var nodeList: MutableList<String>): Sender(nodeId, nodeList), IScalableSender {

    // TODO: Add retry logic since failures are less tolerable

    private val retryCount = 3

    override fun addHost(hostName: String) {
        nodeList.add(hostName)
    }

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
            print("SCALABLE SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        return response.statusCode() !in 400..599
    }

    override fun broadcastScalableMessageAsync(message: ScalableMessage): Boolean {
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message)
        return try {
            CompletableFuture.allOf(
                *nodeList.indices.asSequence().asStream()
                    .filter{ it != nodeId }
                    .map { CompletableFuture.supplyAsync { retryScalableMessage(requestBody, it) } }
                    .toArray { arrayOfNulls<CompletableFuture<HttpResponse<String>>>(it) }
            ).join()
            true
        } catch (e: BroadcastException) {
            print("SCALABLE SENDER: Caught broadcast exception from node ${e.getNodeId()}\n")
            false
        }
    }

    private fun retryScalableMessage(requestBody: String, destNodeId: NodeId): HttpResponse<String>? {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://${nodeList[destNodeId]}/v1/inform"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        var retryRemaining = retryCount
        var response: HttpResponse<String>?
        do {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 400..599) {
                    return response
                }
            } catch (_: ConnectException) {
            }
            retryRemaining--
        } while (retryRemaining > 0)
        throw BroadcastException(destNodeId)
    }

    override fun broadcastScalableMessage(message: ScalableMessage): Boolean {

        print("SCALABLE SENDER: Sending broadcast message ${message.type}\n")

        for (i in nodeList.indices) {
            if (i != nodeId) {
               if (!sendScalableMessage(message, i)) {
                   return false
               }
            }
        }
        return true
    }

    override fun sendScalableMessage(message: ScalableMessage, destNodeId: NodeId): Boolean {
        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/inform")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message)

        print("SCALABLE SENDER: Sending scalable message request to $destUrl\n")

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
            print("SCALABLE SENDER: Caught connection refused exception\n")
        }

        return success
    }
}