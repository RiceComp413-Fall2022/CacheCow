package cache.local

import KeyValuePair
import KeyVersionPair
import cache.distributed.IDistributedCache
import cache.distributed.IScalableDistributedCache
import cache.distributed.hasher.INodeHasher
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class LocalScalableCache(private var nodeHasher: INodeHasher, private val distributedCache: IScalableDistributedCache, private var maxCapacity: Int = 5): ILocalScalableCache {

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

    private var copyHashes = mutableListOf<Int>()

    private var copyIndex = 0

    init {
        head.next = tail
        JVMRuntime = Runtime.getRuntime()
        val memoryUsageInfo = fetchJVMUsage()
        usedMemory = memoryUsageInfo.allocated
        maxMemory = (memoryUsageInfo.max * memoryUtilizationLimit).toLong()

//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
//            monitorMemoryUsage()
//        }, 1, 5, TimeUnit.SECONDS)

    }

    override fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("LOCAL CACHE: Attempting to fetch ${kvPair.key}\n")
        printCacheContents()
        if (cache.containsKey(kvPair)) {
            print("LOCAL CACHE: Found value ${cache[kvPair]}\n")
            val node : LRUNode? = cache[kvPair]
            remove(node)
            insert(node)
            return node?.value
        }
        print("LOCAL CACHE: Key not found\n")
        return null
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray): Boolean {
        print("LOCAL CACHE: Attempting to store (${kvPair.key}, $value)\n")
        printCacheContents()
        val hashValue = nodeHasher.primaryHashValue(kvPair)
        val prevVal = cache[kvPair]?.value
        val prevKvByteSize = if (prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
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

    private fun insert(node : LRUNode?) {
        if (node != null) {
            print("LOCAL CACHE: Inserting key ${node.kvPair}\n")

            node.next = head.next;
            node.prev = head;

            head.next?.prev = node;
            head.next = node;

            cache[node.kvPair] = node

            print("LOCAL CACHE: Finished insert\n")
            printCacheContents()
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

    override fun fetchJVMUsage(): IDistributedCache.MemoryUsageInfo {
        usedMemory = JVMRuntime.totalMemory() - JVMRuntime.freeMemory()
        maxMemory = JVMRuntime.maxMemory()
        val usage = usedMemory/(maxMemory * 1.0)
        return IDistributedCache.MemoryUsageInfo(usedMemory, maxMemory, usage)
    }

    override fun monitorMemoryUsage() {
        print("Monitoring Memory Usage\n")
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

    override fun initializeCopy(copyRanges: MutableList<Pair<Int, Int>>) {
        printSortedLocalKeys()
        copyHashes = mutableListOf()
        for (copyRange in copyRanges) {
            print("LOCAL CACHE: Finding keys to copy in range (${copyRange.first}, ${copyRange.second})\n")
            if (copyRange.first > copyRange.second) {
                print("CASE 1\n")
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
                print("CASE 2\n")
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

    override fun cleanupCopy() {
        print("LOCAL CACHE: Cleaning up copied keys\n")
        for (hashValue in copyHashes) {
            val kvPair = sortedLocalKeys[hashValue]
            val node = cache[kvPair]
            remove(node)
            sortedLocalKeys.remove(hashValue)
        }
        copyHashes = mutableListOf()
        copyIndex = 0
    }

    private fun printCacheContents() {
        print("Cache size is ${cache.size}\n")
        for (node in cache) {
            print("Node: ${node.key} ${node.value.kvPair} ${node.value.value}\n")
        }
    }

    private fun printSortedLocalKeys() {
        print("There are ${sortedLocalKeys.size} local keys\n")
        for (key in sortedLocalKeys.asIterable()) {
            print("Hash value ${key.key} and kv pair ${key.value}\n")
        }
    }


    class LRUNode(val kvPair: KeyVersionPair = KeyVersionPair("", -1), val value: ByteArray = "".toByteArray()) {

        /* The next oldest node */
        var next : LRUNode? = null

        /* The next youngest node */
        var prev : LRUNode? = null

    }
}