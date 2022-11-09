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
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicInteger

/**
 * A concrete sender that sends HTTP requests.
 */
class Sender(private val nodeId: NodeId, private val nodeList: List<String>) : ISender {

    /**
     * The ObjectMapper used to encode JSON data
     */
    private val mapper: ObjectMapper = ObjectMapper()

    private var senderUsageInfo: SenderUsageInfo =
        SenderUsageInfo(AtomicInteger(0), AtomicInteger(0), AtomicInteger(0), AtomicInteger(0))

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.fetchAttempts.addAndGet(1)

        val client = HttpClient.newBuilder().build()

        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/blobs/${kvPair.key}/${kvPair.version}?senderId=${nodeId}")
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

        senderUsageInfo.fetchSuccesses.addAndGet(1)
        return mapper.readTree(response.body()).binaryValue()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: ByteArray, destNodeId: NodeId) {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.storeAttempts.addAndGet(1)

        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/blobs/${kvPair.key}/${kvPair.version}?senderId=${nodeId}")
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
        senderUsageInfo.storeSuccesses.addAndGet(1)
    }

    override fun getSenderUsageInfo(): SenderUsageInfo {
        return senderUsageInfo
    }
}