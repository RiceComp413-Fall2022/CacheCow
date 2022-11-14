package cache.local

import cache.distributed.IDistributedCache

/**
 * An interface specifying the behavior of a local data cache.
 */
interface ILocalCache: IDistributedCache {

    /**
     * @return Cache metrics and performance statistics.
     */
    fun getCacheInfo() : CacheInfo

}

/**
 * Encapsulates information about the cache in this node
 */
data class CacheInfo(val totalKeys: Int, val kvBytes: Int)