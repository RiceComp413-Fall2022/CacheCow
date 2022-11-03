package cache.local

import KeyVersionPair
import java.lang.Integer.max
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * A concrete local cache that stores data in a ConcurrentHashMap.
 */
class TestCache(private var maxCapacity: Int = 100) {

    private val cache: ConcurrentHashMap<KeyVersionPair, ByteArray> = ConcurrentHashMap<KeyVersionPair, ByteArray>(maxCapacity)

    /* Store the total size of key and value bytes. Note that HashMap's auxiliary objects are not counted */
    private var kvByteSize = 0

    private var sortedLocalKeys: SortedMap<Int, KeyVersionPair> = Collections.synchronizedSortedMap(
        TreeMap()
    )

    private var copyKvPairs = mutableListOf<Pair<KeyVersionPair, ByteArray>>()

    private var copyIndex = 0


    fun fetch(kvPair: KeyVersionPair): ByteArray? {
        print("CACHE: Attempting to fetch ${kvPair.key}\n")
        if (cache[kvPair] != null) {
            print("CACHE: Found value ${cache[kvPair]}\n")
        } else {
            print("CACHE: Key not found\n")
        }
        return cache[kvPair]
    }

    fun store(kvPair: KeyVersionPair, value: ByteArray, hashValue: Int): Boolean {
        print("CACHE: Attempting to store (${kvPair.key}, $value)\n")
        return if (cache.size < maxCapacity) {
            print("CACHE: Success\n")
            val prevVal = cache[kvPair]
            val prevKvByteSize = if (prevVal == null) 0 else (prevVal.size + kvPair.key.length + 4)
            kvByteSize += (value.size + kvPair.key.length + 4) - prevKvByteSize
            cache[kvPair] = value
            sortedLocalKeys[hashValue] = kvPair
            true
        } else {
            print("CACHE: Full\n")
            false
        }
    }

    fun isFull(): Boolean {
        return this.cache.size >= this.maxCapacity
    }

    /**
     * Gets information about the cache at the current moment
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(cache.size, kvByteSize)
    }

    fun initalizeCopy(start: Int, end: Int) {
        copyKvPairs = listOf()
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

    fun streamCopyKeys(count: Int): MutableList<Pair<KeyVersionPair, ByteArray>> {
        // Check bounds here and elsewhere
        val topIndex = max(copyIndex + count, copyKvPairs.count())
        val streamKeys = copyKvPairs.subList(copyIndex, topIndex)
        copyIndex = topIndex
        return streamKeys
    }

    private fun addKeyToCopyPairs(kvPair: KeyVersionPair?) {
        if (kvPair != null) {
            val arr = cache[kvPair]
            if (arr != null) {
                copyKvPairs.add(Pair(kvPair, arr))
            }
        }
    }

}


