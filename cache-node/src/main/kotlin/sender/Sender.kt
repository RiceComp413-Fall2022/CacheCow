package sender

import KeyVersionPair
import NodeId
import cache.distributed.IDistributedCache.SystemInfo
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import exception.ConnectionRefusedException
import exception.CrossServerException
import exception.KeyNotFoundException
import org.eclipse.jetty.http.HttpStatus
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicInteger

/**
 * A concrete sender that sends HTTP requests.
 */
open class Sender(private val nodeId: NodeId, private val nodeList: List<String>) : ISender {

    /**
     * The ObjectMapper used to encode JSON data
     */
    protected val mapper: ObjectMapper = ObjectMapper()

    private var senderUsageInfo: SenderUsageInfo = SenderUsageInfo(
        AtomicInteger(0), AtomicInteger(0), AtomicInteger(0),
        AtomicInteger(0), AtomicInteger(0), AtomicInteger(0),
        AtomicInteger(0), AtomicInteger(0))

    override fun fetchFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray {
        print("SENDER: Delegating fetch key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.fetchAttempts.getAndIncrement()

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
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got fetch response with status code ${response.statusCode()}\n")

        if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
            throw KeyNotFoundException(kvPair.key)
        } else if (response.statusCode() in 400..599) {
            throw CrossServerException(destNodeId)
        }

        senderUsageInfo.fetchSuccesses.getAndIncrement()
        print("SENDER: Got fetch response with body ${response.body()}\n")
        return response.body().encodeToByteArray()
    }

    override fun storeToNode(kvPair: KeyVersionPair, value: ByteArray, destNodeId: NodeId) {
        print("SENDER: Delegating store key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.storeAttempts.getAndIncrement()

        val client = HttpClient.newBuilder().build()
        val key = URLEncoder.encode(kvPair.key, "UTF-8")

        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/blobs/${key}/${kvPair.version}?senderId=${nodeId}")
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
        senderUsageInfo.storeSuccesses.getAndIncrement()
    }

    override fun removeFromNode(kvPair: KeyVersionPair, destNodeId: NodeId): ByteArray? {
        print("SENDER: Delegating remove key ${kvPair.key} to node $destNodeId\n")
        senderUsageInfo.removeAttempts.getAndIncrement()

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
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got remove response with status code ${response.statusCode()}\n")

        if (response.statusCode() != 204 && response.statusCode() != 404) {
            throw CrossServerException(destNodeId)
        }

        senderUsageInfo.removeSuccesses.getAndIncrement()
        return response.body().encodeToByteArray()
    }

    override fun clearNode(destNodeId: NodeId) {
        print("SENDER: Clearing node $destNodeId\n")
        senderUsageInfo.clearAttempts.getAndIncrement()

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
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got clear response with status code ${response.statusCode()}\n")

        if (response.statusCode() != 204) {
            throw CrossServerException(destNodeId)
        }

        senderUsageInfo.clearSuccesses.getAndIncrement()
    }

    override fun getCacheInfo(destNodeId: NodeId): SystemInfo {
        print("SENDER: Fetching the local cache info from node $destNodeId\n")

        val client = HttpClient.newBuilder().build()
        val destUrl = URI.create("http://${nodeList[destNodeId]}/v1/local-cache-info?senderId=${nodeId}")
        val request = HttpRequest.newBuilder()
            .uri(destUrl)
            .GET()
            .build()

        print("SENDER: Sending cache info request to $destUrl\n")

        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            print("SENDER: Caught connection refused exception\n")
            throw ConnectionRefusedException(destNodeId)
        }

        print("SENDER: Got cache info response with status code ${response.statusCode()}\n")

        if (response.statusCode() != 200) {
            throw CrossServerException(destNodeId)
        }

        val systemInfo: SystemInfo
        try {
            systemInfo = mapper.treeToValue(mapper.readTree(response.body()), SystemInfo::class.java)
        } catch (e: JsonProcessingException) {
            print("SENDER: Caught JSON processing exception: ${e.message}\n")
            throw CrossServerException(destNodeId)
        }
        return systemInfo
    }

    override fun getSenderUsageInfo(): SenderUsageInfo {
        return senderUsageInfo
    }
}