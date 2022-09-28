package service

import Cache
import KeyVersionPair
import NodeHasher
import NodeId
import Sender
import interfaces.IDistributedCache
import org.eclipse.jetty.http.HttpStatus

/**
 * Implementation of IReceiverService that fetches and stores cache data by delegating to
 * the first and second storing nodes.
 */
class RoundRobinCache(nodeId: NodeId, nodeCount: Int, cache: Cache, sender: Sender, nodeHasher: NodeHasher): IDistributedCache {

    private val nodeId: NodeId
    private val nodeCount: Int
    private val cache: Cache
    private val sender: Sender
    private val nodeHasher: NodeHasher

    init {
        this.nodeId = nodeId
        this.nodeCount = nodeCount
        this.cache = cache
        this.sender = sender
        this.nodeHasher = nodeHasher
    }

    override fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?) {
        if (senderId == null) {
            storeClient(kvPair, value)
        } else {
            storeNode(kvPair, value, senderId)
        }
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String {
        if (senderId == null) {
            return fetchClient(kvPair)
        }
        return fetchNode(kvPair, senderId)
    }

    private fun storeClient(kvPair: KeyVersionPair, value: String) {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        if (primaryNodeId == nodeId && !cache.isFull()) {
            cache.store(kvPair, value)
            return
        }

        val destNodeId = if (primaryNodeId == nodeId) nodeHasher.secondaryHash(kvPair) else primaryNodeId
        sender.storeToNode(
            kvPair,
            value,
            destNodeId
        )
    }

    private fun storeNode(kvPair: KeyVersionPair, value: String, senderId: NodeId) {
        if (!cache.isFull()) {
            cache.store(kvPair, value)
            return
        }

        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        if (senderId != primaryNodeId) {
            sender.storeToNode(
                kvPair,
                value,
                nodeHasher.secondaryHash(kvPair)
            )
        }
        throw io.javalin.http.HttpResponseException(HttpStatus.CONFLICT_409, "No space available")
    }

    private fun fetchClient(kvPair: KeyVersionPair): String {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)
        val value: String?

        if (primaryNodeId == nodeId) {
            value = cache.fetch(kvPair)
            if (value != null) {
                return value
            }
        }

        val destNodeId = if (primaryNodeId == nodeId) nodeHasher.secondaryHash(kvPair) else primaryNodeId

        return sender.fetchFromNode(
            kvPair,
            destNodeId
        )
    }

    private fun fetchNode(kvPair: KeyVersionPair, senderId: NodeId): String {
        val value = cache.fetch(kvPair)

        if (value != null) {
            return value
        }

        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        if (senderId != primaryNodeId) {
            return sender.fetchFromNode(kvPair, nodeHasher.secondaryHash(kvPair))
        }
        throw io.javalin.http.HttpResponseException(HttpStatus.NOT_FOUND_404, "Value not found")
    }
}