package cache.local

import KeyValuePair
import KeyVersionPair
import cache.distributed.IDistributedCache
import cache.distributed.hasher.INodeHasher
import exception.CacheFullException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class ScalableLocalCache(private var nodeHasher: INodeHasher, maxCapacity: Int = 100) : IScalableLocalCache {

    /* A process-safe concurrent hash map that is used to store LRU payload-containing node refferences */
    private val cache: ConcurrentHashMap<KeyVersionPair, LRUNode> = ConcurrentHashMap<KeyVersionPair, LRUNode>(maxCapacity)

    /* The LRU queue */
    private var lruCache: ConcurrentLinkedQueue<LRUNode>

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize: Int = 0

    /* The JVM runtime */
    private var runtime: Runtime = Runtime.getRuntime()

    /* The current amount of memory (bytes) the cache is storing */
    private var usedMemory: Long = 0

    /* The maximum memory capacity (bytes) allotted to the JVM */
    private var maxMemory: Long = runtime.maxMemory()

    /* The utilization threshold for the JVM */
    private var memoryUtilizationLimit: Float = 0.8F

    /* Sorted hash values of all keys in the cache */
    private var sortedLocalKeys: SortedMap<Int, KeyVersionPair> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    /* List of hash values to copy */
    private var copyHashes = mutableListOf<Int>()

    /* Key value pair copy streaming index */
    private var copyIndex = 0

    init {
        lruCache = ConcurrentLinkedQueue<LRUNode>()

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            monitorMemoryUsage()
        }, 0, 2, TimeUnit.SECONDS)
    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("LOCAL CACHE: Attempting to fetch ${kvPair.key}\n")
        val nullableNode: LRUNode? = cache[kvPair]
        if (nullableNode != null) {
            print("LOCAL CACHE: Found value ${cache[kvPair]}\n")
            val node: LRUNode = nullableNode
            remove(node)
            insert(node)
            return node.value
        }
        print("LOCAL CACHE: Key not found\n")
        return null
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        print("LOCAL CACHE: Attempting to store (${kvPair.key}, $value)\n")

        if (isFull()) {
            print("LOCAL CACHE: Cache full, unable to store (${kvPair.key}, $value)\n")
            throw CacheFullException()
        }

        val hashValue = nodeHasher.primaryHashValue(kvPair)
        val prevVal = cache[kvPair]?.value
        val prevKvByteSize = if (prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
        kvByteSize += (value.size + kvPair.key.length + 4) - prevKvByteSize

        val nullableOldNode: LRUNode? = cache[kvPair]
        if (nullableOldNode != null) {
            remove(nullableOldNode)
        }

        val newNode = LRUNode(kvPair, value)
        cache[kvPair] = newNode
        insert(newNode)
        sortedLocalKeys[hashValue] = kvPair

        usedMemory += newNode.size
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun clearAll(isClientRequest: Boolean) {
        TODO("Not yet implemented")
    }

    private fun insert(node: LRUNode) {
        lruCache.add(node)
        cache[node.kvPair] = node
    }

    private fun remove(node: LRUNode) {
        lruCache.remove(node)
        cache.remove(node.kvPair)
    }

    private fun removeCopy(node: LRUNode) {
        remove(node)
        usedMemory -= node.size
        kvByteSize -= (node.value.size + node.kvPair.key.length + 4)
        print("Decreasing kv bytes size by ${node.value.size + node.kvPair.key.length}\n")
    }

    override fun fetchJVMUsage(): IDistributedCache.MemoryUsageInfo {
        return IDistributedCache.MemoryUsageInfo(usedMemory, maxMemory, usedMemory/(maxMemory * 1.0))
    }

    private fun isFull(): Boolean {
        return usedMemory > (maxMemory * memoryUtilizationLimit).toInt()
    }

    override fun monitorMemoryUsage() {
        print("LOCAL CACHE: Monitoring memory usage\n")

        if (isFull()) {
            print("LOCAL CACHE: Cache is full\n")
            val estimateToRemove = usedMemory - (maxMemory * (memoryUtilizationLimit - 0.2))
            print("LOCAL CACHE: Used $usedMemory maximum ${(maxMemory * memoryUtilizationLimit).toInt()} amount to remove $estimateToRemove\n")
            var removed: Long = 0

            while (removed < estimateToRemove) {
                removed += removeLRU()
                print("LOCAL CACHE: Removed $removed\n")
            }
        }
    }

    private fun removeLRU(): Long {
        val node = lruCache.poll()
        cache.remove(node.kvPair)
        usedMemory -= node.size

        // Remove from local key tree
        val hashValue = nodeHasher.primaryHashValue(node.kvPair)
        sortedLocalKeys.remove(hashValue)

        return node.size
    }

    /**
     * Gets information about the cache at the current moment
     */
    override fun getCacheInfo(): CacheInfo {
        print("LOCAL CACHE: Getting cache info with bytes: $kvByteSize\n")
        return CacheInfo(cache.size, kvByteSize)
    }

    override fun initializeCopy(copyRanges: MutableList<Pair<Int, Int>>) {
        printSortedLocalKeys()
        copyHashes = mutableListOf()
        for (copyRange in copyRanges) {
            print("LOCAL CACHE: Finding keys to copy in range (${copyRange.first}, ${copyRange.second})\n")
            if (copyRange.first > copyRange.second) {
                for (e in sortedLocalKeys.headMap(copyRange.second)) {
                    if (e.key != null) {
                        copyHashes.add(e.key)
                    }
                }
                for (e in sortedLocalKeys.tailMap(copyRange.first)) {
                    if (e.key != null) {
                        copyHashes.add(e.key)
                    }
                }
            } else {
                for (e in sortedLocalKeys.subMap(copyRange.first, copyRange.second)) {
                    if (e.key != null) {
                        copyHashes.add(e.key)
                    }
                }
            }
        }
        print("\nLOCAL CACHE: Found ${copyHashes.size} keys to copy\n")
        for (hashValue in copyHashes) {
            print("Hash value $hashValue\n")
        }
    }

    override fun streamCopyKeys(count: Int): MutableList<KeyValuePair> {
        // Check bounds here and elsewhere
        val topIndex = Integer.min(copyIndex + count, copyHashes.count())
        val streamKeys = mutableListOf<KeyValuePair>()
        for (i in copyIndex until topIndex) {
            val kvPair = sortedLocalKeys[copyHashes[i]]
            if (kvPair != null) {
                val node = cache[kvPair]
                if (node != null) {
                    streamKeys.add(KeyValuePair(kvPair.key, kvPair.version, node.value))
                }
            }
        }
        copyIndex = topIndex
        return streamKeys
    }

    // TODO: Clean up after each batch completes
    override fun cleanupCopy() {
        print("LOCAL CACHE: Cleaning up copied keys\n")
        for (hashValue in copyHashes) {
            val kvPair = sortedLocalKeys[hashValue]
            val node = cache[kvPair]
            if (node != null) {
                removeCopy(node)
                sortedLocalKeys.remove(hashValue)
            }
        }
        copyHashes = mutableListOf()
        copyIndex = 0
    }

    private fun printSortedLocalKeys() {
        print("There are ${sortedLocalKeys.size} local keys\n")
        for (key in sortedLocalKeys.asIterable()) {
            print("Hash value ${key.key} and kv pair ${key.value}\n")
        }
    }

    private fun printCacheContents() {
        print("Cache size is ${cache.size}\n")
        for (node in cache) {
            print("Node: ${node.key} ${node.value.kvPair} ${node.value.value}\n")
        }
    }

    /**
     * @param kvPair The key of the node - key of the key-value pair
     * @param value The payload of the node - value of the key-value pair
     */
    class LRUNode(val kvPair: KeyVersionPair=KeyVersionPair("", -1), val value: ByteArray=ByteArray(0)) {

        /* The size of the payload */
        var size : Long = (value.size + kvPair.key.length + 4).toLong()
    }
}