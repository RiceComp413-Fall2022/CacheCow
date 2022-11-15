package cache.local

import receiver.MemoryUsageInfo

/**
 * An interface specifying the behavior of a local data cache.
 */
interface ILocalEvictingCache : ILocalCache {

    /**
     * Fetches JVM Usage and updates respective LocalEvictingCache attributes
     */
    fun fetchJVMUsage(): MemoryUsageInfo

    fun monitorMemoryUsage()

    fun isCacheFull(): Boolean

    fun isJVMFull(): Boolean

}