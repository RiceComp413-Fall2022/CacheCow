package cache.local

import KeyVersionPair
import node.MemoryUsageInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalEvictingCache(private var maxCapacity: Int = 100) : ILocalEvictingCache {

    /* A process-safe concurrent hash map that is used to store LRU payload-containing node refferences */
    private val cache: ConcurrentHashMap<KeyVersionPair, LRUNode> = ConcurrentHashMap<KeyVersionPair, LRUNode>(maxCapacity)


    /* The LRU queue */
    var LRUCache : ConcurrentLinkedQueue<LRUNode>

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = 0

    /* The JVM runtime */
    private var JVMRuntime : Runtime = Runtime.getRuntime()

    /* The current amount of memory (bytes) the cache is storing */
    private var usedMemory : Long = 0

    /* The maximum memory capacity (bytes) allotted to the JVM */
    private var maxMemory : Long = JVMRuntime.maxMemory()

    /* The utilization threshold for the JVM */
    private var memoryUtilizationLimit : Float = 0.8F

    init {
        LRUCache = ConcurrentLinkedQueue<LRUNode>()

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            monitorMemoryUsage()
        }, 0, 2, TimeUnit.SECONDS)
    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        val nullableNode : LRUNode? = cache[kvPair]
        if (nullableNode != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
            val node : LRUNode = nullableNode
            remove(node)
            insert(node)
            return node.value
        }
        print("CACHE: Key not found\n")
        return null
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray): Boolean {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")

        if (isFull()) {
            print("CACHE: Cache full, unable to store (${kvPair.key}, $value)\n")
            return false
        }

        val prevVal = cache[kvPair]?.value
        val prevKvByteSize = if(prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
        kvByteSize += (value.size + kvPair.key.length + 4) - prevKvByteSize

        val nullableOldNode : LRUNode? = cache[kvPair]
        if (nullableOldNode != null) {
            val oldNode : LRUNode = nullableOldNode
            remove(oldNode)
        }

        val newNode = LRUNode(kvPair, value)
        cache[kvPair] = newNode
        insert(newNode)

        usedMemory += newNode.size

        return true
    }

    private fun insert(node : LRUNode) {
        LRUCache.add(node)
        cache[node.kvPair] = node
    }

    private fun remove(node : LRUNode) {
        LRUCache.remove(node)
        cache.remove(node.kvPair)
    }

    override fun fetchJVMUsage(): MemoryUsageInfo {
        return MemoryUsageInfo(usedMemory, maxMemory)
    }

    override fun isFull(): Boolean {
        return keysFull() && JVMFull()
    }

    fun keysFull(): Boolean {
        return LRUCache.size > maxCapacity
    }

    fun JVMFull(): Boolean {
        return usedMemory > (maxMemory * memoryUtilizationLimit).toInt()
    }


    override fun monitorMemoryUsage() {
        // print("Monitoring Memory Usage\n")

        if (JVMFull()) {
            print("Cache is full\n")
            val estimateToRemove = usedMemory - (maxMemory * (memoryUtilizationLimit - 0.2))
            print("used $usedMemory maximum ${(maxMemory * memoryUtilizationLimit).toInt()} amount to remove $estimateToRemove\n")
            var removed : Long = 0

            while (removed < estimateToRemove) {
                removed += removeLRU()
                print("removed $removed\n")
            }
        }
        if (keysFull()) {
            print("Cache is full\n")
            val KVPToRemove = (maxCapacity * 0.1).toInt()
            print("used all ${LRUCache.size} key. Will now evict ${(maxCapacity * 0.1).toInt()} keys.\n")

            for (i in 1..KVPToRemove) {
                removeLRU()
            }
        }
    }

    private fun removeLRU() : Long {
        val node = LRUCache.poll()
        cache.remove(node.kvPair)
        usedMemory -= node.size
        return node.size
    }

    /**
     * Gets information about the cache at the current moment
     */
    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize)
    }


    class LRUNode(kvPair : KeyVersionPair=KeyVersionPair("", -1), value : ByteArray=ByteArray(0)) {

        /* The key of the node - key of the key-value pair */
        val kvPair : KeyVersionPair = kvPair

        /* The payload of the node - value of the key-value pair */
        val value : ByteArray = value

        /* The size of the payload */
        var size : Long = (value.size + kvPair.key.length + 4).toLong()

    }
}
