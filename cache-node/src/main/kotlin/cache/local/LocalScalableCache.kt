package cache.local

import KeyVersionPair
import cache.distributed.hasher.INodeHasher
import node.MemoryUsageInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalScalableCache(private var nodeHasher: INodeHasher, private var maxCapacity: Int = 100): ILocalScalableCache {

    /* A process-safe concurrent hash map that is used to store LRU payload-containing node refferences */
    private val cache: ConcurrentHashMap<KeyVersionPair, LRUNode> = ConcurrentHashMap<KeyVersionPair, LRUNode>(maxCapacity)

    /* The head of the LRUQueue - the most recent node */
    private var head: LRUNode = LRUNode()

    /* The tail of the LRUQueue - the least recent node */
    private var tail: LRUNode = LRUNode()

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = 0

    /* The JVM runtime */
    private var JVMRuntime: Runtime

    /* The current amount of memory (bytes) the JVM is using */
    private var usedMemory: Long

    /* The maximum memory capacity (bytes) allotted to the JVM */
    private var maxMemory: Long

    /* The utilization threshold for the JVM */
    private var memoryUtilizationLimit: Float = 0.8F

    private var sortedLocalKeys: SortedMap<Int, KeyVersionPair> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    private var copyKvPairs = mutableListOf<Pair<KeyVersionPair, ByteArray>>()

    private var copyIndex = 0

    init {
        head.next = tail
        JVMRuntime = Runtime.getRuntime()
        val memoryUsageInfo = fetchJVMUsage()
        usedMemory = memoryUsageInfo.allocated
        maxMemory = (memoryUsageInfo.max * memoryUtilizationLimit).toLong()

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            monitorMemoryUsage()
        }, 1, 5, TimeUnit.SECONDS)

    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache.containsKey(kvPair)) {
            print("CACHE: Found value ${cache[kvPair]}\n")
            val node : LRUNode? = cache[kvPair]
            remove(node)
            insert(node)
            return node?.value
        }
        print("CACHE: Key not found\n")
        return null
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray): Boolean {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")


        val hashValue = nodeHasher.primaryHashValue(kvPair)
        val prevVal = cache[kvPair]?.value
        val prevKvByteSize = if(prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
        kvByteSize += (value.size + kvPair.key.length + 4) - prevKvByteSize

        val node = LRUNode(kvPair, value)
        if (cache.containsKey(kvPair)) {
            remove(cache[kvPair])
        } else {
            sortedLocalKeys[hashValue] = kvPair
        }
        insert(node)

        return true
    }

    private fun insert(nullableNode : LRUNode?) {
        if (nullableNode != null) {
            val node : LRUNode =  nullableNode

            node.next = head.next;
            node.prev = head;

            head.next?.prev = node;
            head.next = node;

            cache[node.kvPair] = node
        }
    }

    private fun remove(nullableNode : LRUNode?) {
        if (nullableNode != null) {
            val node : LRUNode =  nullableNode

            node.prev?.next = node.next;
            node.next?.prev = node.prev;
            cache.remove(node.kvPair)

        }
    }

    private fun removeLRU() : Int {
        if (cache.size == 0) {
            /* consider raising an exception */
            return 0
        }

        val nullableNode : LRUNode? = tail.prev
        if (nullableNode != null) {
            val node : LRUNode = nullableNode
            node.prev?.next = node.next;
            node.next?.prev = node.prev;
            cache.remove(node.kvPair)

            // Remove from local key tree
            val hashValue = nodeHasher.primaryHashValue(node.kvPair)
            sortedLocalKeys.remove(hashValue)

            return node.value.size + node.kvPair.key.length + 4
        }
        return 0
    }

    override fun isFull(): Boolean {
        return isCacheFull() || isJVMFull()
    }

    override fun isCacheFull(): Boolean {
        return cache.size >= maxCapacity
    }

    override fun isJVMFull(): Boolean {
        return usedMemory.toInt() > (maxMemory.toInt() * memoryUtilizationLimit).toInt()
    }

    override fun fetchJVMUsage(): MemoryUsageInfo {
        usedMemory = JVMRuntime.totalMemory() - JVMRuntime.freeMemory()
        maxMemory = JVMRuntime.maxMemory()
        val usage = usedMemory/(maxMemory * 1.0)
        return MemoryUsageInfo(usedMemory, maxMemory, usage)
    }

    override fun monitorMemoryUsage() {
        print("Monitoring Memory Usage")
        if (isCacheFull()) {
            for (i in 1..(cache.size - (maxCapacity * memoryUtilizationLimit).toInt()) + 1) {
                removeLRU()
            }
        }

        fetchJVMUsage()
        if (isJVMFull()) {
            val estimateToRemove = (maxMemory * (memoryUtilizationLimit - 0.2)) - usedMemory
            var removed = 0

            while (removed < estimateToRemove) {
                removed += removeLRU()
            }
        }
    }

    /**
     * Gets information about the cache at the current moment
     */
    override fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize)
    }

    override fun initializeCopy(start: Int, end: Int) {
        copyKvPairs = mutableListOf()
        if (start > end) {
            for (e in sortedLocalKeys.tailMap(end)) {
                addKeyToCopyPairs(e.value)
            }
            for (e in sortedLocalKeys.headMap(start)) {
                addKeyToCopyPairs(e.value)
            }
        } else {
            for (e in sortedLocalKeys.subMap(start, end)) {
                addKeyToCopyPairs(e.value)
            }
        }
    }

    override fun streamCopyKeys(count: Int): MutableList<Pair<KeyVersionPair, ByteArray>> {
        // Check bounds here and elsewhere
        val topIndex = Integer.max(copyIndex + count, copyKvPairs.count())
        val streamKeys = copyKvPairs.subList(copyIndex, topIndex)
        copyIndex = topIndex
        return streamKeys
    }

    private fun addKeyToCopyPairs(kvPair: KeyVersionPair?) {
        if (kvPair != null) {
            val node = cache[kvPair]
            if (node != null) {
                copyKvPairs.add(Pair(kvPair, node.value))
            }
        }
    }


    class LRUNode(val kvPair: KeyVersionPair = KeyVersionPair("", -1), val value: ByteArray = "".toByteArray()) {

        /* The next oldest node */
        var next : LRUNode? = null

        /* The next youngest node */
        var prev : LRUNode? = null

    }
}