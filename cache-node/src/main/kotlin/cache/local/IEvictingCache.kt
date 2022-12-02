package cache.local

import cache.distributed.IDistributedCache

/**
 * An interface specifying the behavior of a local evicting data cache
 */
interface IEvictingCache : ILocalCache {

    /**
     * Returns JVM memory usage and updates respective LocalEvictingCache attributes
     */
    fun fetchJVMUsage(): IDistributedCache.MemoryUsageInfo

    /**
     * Monitors the current JVM memory usage and considers eviction accordingly.
     */
    fun monitorMemoryUsage()

}