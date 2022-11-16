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

fun aggregateTableInfo(info1: CacheInfo, info2: CacheInfo): CacheInfo {
    return CacheInfo(info1.totalKeys + info2.totalKeys,
        info1.memorySize + info2.memorySize)
}

data class CacheInfo(val totalKeys: Int, val memorySize: Int)