package cache.local

import cache.ICache
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * An interface specifying the behavior of a local data cache.
 */
interface ILocalCache: ICache {

    /**
     * @return Cache metrics and performance statistics.
     */
    fun getCacheInfo(): CacheInfo

}

/**
 * Given the cache info of two different caches, returns the combined cache info.
 *
 * @param info1 cache info of first cache
 * @param info2 cache info of second cache
 * @return overall cache info
 */
fun aggregateTableInfo(info1: CacheInfo, info2: CacheInfo): CacheInfo {
    return CacheInfo(info1.totalKeys + info2.totalKeys,
        info1.memorySize + info2.memorySize)
}

data class CacheInfo(
    @JsonProperty("totalKeys") val totalKeys: Int,
    @JsonProperty("memorySize") val memorySize: Int
)