package sender

import KeyVersionPair
import NodeId
import com.fasterxml.jackson.databind.ObjectMapper
import exception.ConnectionRefusedException
import exception.CrossServerException
import exception.KeyNotFoundException
import org.eclipse.jetty.http.HttpStatus
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A concrete sender that sends HTTP requests.
 */
open class Sender(private val nodeId: NodeId, private val nodeList: List<String>) : ISender {

    /**
     * The ObjectMapper used to encode JSON data
     */
    protected val mapper: ObjectMapper = ObjectMapper()

    private var senderUsageInfo: SenderUsageInfo =
        SenderUsageInfo(0, 0, 0, 0)

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.fetchAttempts++

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
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
            throw KeyNotFoundException(kvPair.key)
        } else if (response.statusCode() in 400..599) {
            throw CrossServerException(destNodeId)
        }

        senderUsageInfo.fetchSuccesses++
        print("SENDER: Got fetch response with body ${response.body()}\n")
        return response.body().encodeToByteArray()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: ByteArray, destNodeId: NodeId) {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.storeAttempts++

        val client = HttpClient.newBuilder().build()

        val destUrl =
            URI.create("http://${nodeList[destNodeId]}/v1/blobs/${kvPair.key}/${kvPair.version}?senderId=${nodeId}")

        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofByteArray(value))
            .build()

        print("SENDER: Sending store request to $destUrl with value ${value.contentToString()}\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got store response with status code ${response.statusCode()}\n")

        if (response.statusCode() in 400..599) {
            throw CrossServerException(destNodeId)
        }
        senderUsageInfo.storeSuccesses++
    }

    override fun getSenderUsageInfo(): SenderUsageInfo {
        return senderUsageInfo
    }
}