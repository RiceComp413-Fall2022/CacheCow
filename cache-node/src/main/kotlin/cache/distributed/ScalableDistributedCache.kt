import cache.distributed.IDistributedCache
import cache.distributed.hasher.INodeHasher
import cache.distributed.hasher.NodeHasher
import cache.local.ILocalCache
import exception.KeyNotFoundException
import sender.ISender
import java.util.*

/**
 * A concrete distributed cache that assigns keys to nodes using a NodeHasher.
 */
class ScalableDistributedCache(private val nodeId: NodeId, nodeCount: Int, private val cache: ILocalCache, var sender: ISender):
    IDistributedCache {

    /**
     * The INodeHasher used to map keys to nodes
     */
    private val nodeHasher: INodeHasher = NodeHasher(nodeCount)

    private var sortedNodes: List<Pair<Int, Int>>

    private var sortedLocalKeys: SortedMap<KeyVersionPair, Int> = Collections.synchronizedSortedMap(TreeMap())


    init {
        sortedNodes = listOf()
        for ()
    }

    override fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): ByteArray {
        val hashValue = nodeHasher.primaryHashValue(kvPair)




        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        val value: ByteArray? = if (nodeId == primaryNodeId) {
            cache.fetch(kvPair)
        } else {
            sender.fetchFromNode(
                kvPair,
                primaryNodeId
            )
        }
        if (value == null) {
            throw KeyNotFoundException(kvPair.key)
        }
        return value
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray, senderId: NodeId?) {
        val primaryNodeId = nodeHasher.primaryHash(kvPair)

        print("DISTRIBUTED CACHE: Hash value of key ${kvPair.key} is ${primaryNodeId}\n")

        if (nodeId == primaryNodeId) {
            cache.store(kvPair, value)
        } else {
            sender.storeToNode(
                kvPair,
                value,
                primaryNodeId
            )
        }
    }

    fun beginCopying() {
        nodeCount
    }
}