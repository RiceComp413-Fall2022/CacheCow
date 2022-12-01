package cache.local

import cache.distributed.IDistributedCache

/**
 * An interface specifying the behavior of a local data cache.
 */
interface ILocalEvictingCache : ILocalCache {

    /**
     * Fetches JVM Usage and updates respective LocalEvictingCache attributes
     */
    fun fetchJVMUsage(): IDistributedCache.MemoryUsageInfo

    fun monitorMemoryUsage()

}