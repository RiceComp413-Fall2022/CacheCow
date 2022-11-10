package sender

import KeyVersionPair
import NodeId
import com.fasterxml.jackson.databind.ObjectMapper
import exception.ConnectionRefusedException
import exception.InternalErrorException
import exception.KeyNotFoundException
import org.eclipse.jetty.http.HttpStatus
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A concrete sender that sends HTTP requests.
 */
class Sender(private val nodeId: NodeId, private val nodeList: List<String>) : ISender {

    /**
     * The ObjectMapper used to encode JSON data
     */
    private val mapper: ObjectMapper = ObjectMapper()

    private var senderUsageInfo: SenderUsageInfo = SenderUsageInfo(
        0, 0, 0, 0, 0, 0, 0, 0)

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.fetchAttempts++

        val client = HttpClient.newBuilder().build()
        val key = URLEncoder.encode(kvPair.key, "UTF-8")
        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/blobs/${key}/${kvPair.version}?senderId=${nodeId}")
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .GET()
            .build()

        print("SENDER: Sending fetch request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
            throw KeyNotFoundException(kvPair.key)
        } else if (response.statusCode() in 400..599) {
            throw InternalErrorException()
        }

        senderUsageInfo.fetchSuccesses++
        return mapper.readTree(response.body()).binaryValue()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: ByteArray, destNodeId: NodeId) {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.storeAttempts++

        val client = HttpClient.newBuilder().build()
        val key = URLEncoder.encode(kvPair.key, "UTF-8")
        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/blobs/${key}/${kvPair.version}?senderId=${nodeId}")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        print("SENDER: Sending store request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() in 400..599) {
            throw InternalErrorException()
        }
        senderUsageInfo.storeSuccesses++
    }

    override fun removeFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray? {
        print("SENDER: Delegating remove key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.removeAttempts++

        val client = HttpClient.newBuilder().build()
        val key = URLEncoder.encode(kvPair.key, "UTF-8")
        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/blobs/${key}/${kvPair.version}?senderId=${nodeId}")
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .DELETE()
            .build()

        print("SENDER: Sending remove request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        print("SENDER: Got remove response with status code ${response.statusCode()}\n")

        if (response.statusCode() != 204 && response.statusCode() != 404) {
            throw InternalErrorException()
        }

        senderUsageInfo.removeSuccesses++
        return mapper.readTree(response.body()).binaryValue()
    }

    override fun clearNode(destNodeId: NodeId) {
        print("SENDER: Clearing node $destNodeId\n")
        senderUsageInfo.clearAttempts++

        val client = HttpClient.newBuilder().build()
        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/clear?senderId=${nodeId}")
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .DELETE()
            .build()

        print("SENDER: Sending clear request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException()
        }

        print("SENDER: Got clear response with status code ${response.statusCode()}")

        if (response.statusCode() != 204) {
            throw InternalErrorException()
        }

        senderUsageInfo.clearSuccesses++
    }

    override fun getSenderUsageInfo(): SenderUsageInfo {
        return senderUsageInfo
    }
}