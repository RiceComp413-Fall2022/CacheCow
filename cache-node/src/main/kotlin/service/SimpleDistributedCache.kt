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
 * the primary node for that data.
 */
class SimpleDistributedCache(nodeId: NodeId, nodeCount: Int, cache: Cache, sender: Sender, nodeHasher: NodeHasher): IDistributedCache {

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
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")
        if (nodeId == primaryNodeId) {
            if (cache.isFull()) {
                throw io.javalin.http.HttpResponseException(HttpStatus.CONFLICT_409, "No space available")
            }
            cache.store(kvPair, value)
            return
        }
        sender.storeToNode(
            kvPair,
            value,
            primaryNodeId
        )
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)
        val value: String?

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")
        if (nodeId == primaryNodeId) {
            value = cache.fetch(kvPair)
            if (value == null) {
                throw io.javalin.http.HttpResponseException(HttpStatus.NOT_FOUND_404, "Value not found")
            }
            return value
        }
        return sender.fetchFromNode(
            kvPair,
            primaryNodeId
        )
    }
}