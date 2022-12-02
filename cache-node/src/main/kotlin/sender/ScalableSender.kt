package sender

import BulkCopyRequest
import NodeId
import ScalableMessage
import exception.ConnectionRefusedException
import exception.CrossClientException
import exception.CrossServerException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import kotlin.streams.asStream

/**
 * Concrete implementation of a scalable sender.
 */
class ScalableSender(private val nodeId: NodeId, private var nodeList: MutableList<String>): Sender(nodeId, nodeList), IScalableSender {

    /**
     * Default retry count on all scalable requests, used to make communication more robust.
     */
    private val defaultRetryCount = 3

    override fun sendBulkCopy(kvPairs: BulkCopyRequest, destNodeId: NodeId) {
        val client = HttpClient.newBuilder().build()

        for (kvPair in kvPairs.values) {
            print("SCALABLE SENDER: Sending pair ${kvPair.key}, ${kvPair.value.contentToString()}\n")
        }
        val request = generatePostRequest(kvPairs, "/v1/bulk-copy", destNodeId)

        retryMessage(
            { client.send(request, HttpResponse.BodyHandlers.ofString()) },
            { statusCode: Int, nodeId: NodeId, isFinalTry: Boolean ->
                scalableErrorHandler(
                    statusCode,
                    nodeId,
                    isFinalTry
                )
            },
            destNodeId
        )
    }

    override fun broadcastScalableMessageAsync(message: ScalableMessage) {
        CompletableFuture.allOf(
            *nodeList.indices.asSequence().asStream()
                .filter { it != nodeId }
                .map {
                    CompletableFuture.supplyAsync {
                        sendScalableMessage(
                            message,
                            it
                        )
                    }
                }
                .toArray { arrayOfNulls<CompletableFuture<HttpResponse<String>>>(it) }
        ).join()
    }

    override fun sendScalableMessage(message: ScalableMessage, destNodeId: NodeId) {
        val client = HttpClient.newBuilder().build()
        val request = generatePostRequest(message, "/v1/inform", destNodeId)

        retryMessage(
            { client.send(request, HttpResponse.BodyHandlers.ofString()) },
            { statusCode: Int, nodeId: NodeId, isFinalTry: Boolean ->
                scalableErrorHandler(
                    statusCode,
                    nodeId,
                    isFinalTry
                )
            },
            destNodeId
        )
    }

    private fun scalableErrorHandler(
        statusCode: Int,
        destNodeId: NodeId,
        isFinalTry: Boolean
    ): Boolean {
        if (statusCode in 400..499) {
            throw CrossClientException(destNodeId)
        } else if (statusCode in 500..599) {
            if (isFinalTry) {
                throw CrossServerException(destNodeId)
            }
            return true
        }
        return false
    }

    private fun retryMessage(
        responseProducer: () -> HttpResponse<String>,
        errorHandler: (statusCode: Int, destNodeId: NodeId, isFinalRetry: Boolean) -> Boolean,
        destNodeId: NodeId,
        retryCount: Int = defaultRetryCount
    ): HttpResponse<String>? {
        var retryRemaining = retryCount
        var response: HttpResponse<String>? = null
        var success = false
        do {
            try {
                response = responseProducer.invoke()
                if (!errorHandler.invoke(
                        response.statusCode(),
                        destNodeId,
                        retryRemaining == 1
                    )
                ) {
                    success = true
                }
            } catch (_: ConnectException) {
                if (retryRemaining == 1) {
                    throw ConnectionRefusedException(destNodeId)
                }
            }
            retryRemaining--
        } while (retryRemaining > 0 && !success)
        return response
    }

    private fun generatePostRequest(payload: Any, endpoint: String, destNodeId: NodeId): HttpRequest {
        val destUrl =
            URI.create("http://${nodeList[destNodeId]}$endpoint")
        val requestBody =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)

        return HttpRequest.newBuilder()
            .uri(destUrl)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
    }
}